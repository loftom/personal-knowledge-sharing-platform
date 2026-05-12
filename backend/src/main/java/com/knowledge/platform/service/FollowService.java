package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.platform.common.AppException;
import com.knowledge.platform.domain.dto.ContentDtos;
import com.knowledge.platform.domain.entity.Content;
import com.knowledge.platform.domain.entity.FollowRelation;
import com.knowledge.platform.domain.entity.User;
import com.knowledge.platform.domain.mapper.ContentMapper;
import com.knowledge.platform.domain.mapper.FollowRelationMapper;
import com.knowledge.platform.domain.mapper.UserMapper;
import com.knowledge.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FollowService {

    private final FollowRelationMapper followRelationMapper;
    private final UserMapper userMapper;
    private final ContentMapper contentMapper;
    private final BehaviorService behaviorService;

    public FollowService(FollowRelationMapper followRelationMapper,
                         UserMapper userMapper,
                         ContentMapper contentMapper,
                         BehaviorService behaviorService) {
        this.followRelationMapper = followRelationMapper;
        this.userMapper = userMapper;
        this.contentMapper = contentMapper;
        this.behaviorService = behaviorService;
    }

    @Transactional
    public boolean toggleFollow(Long targetUserId) {
        Long userId = UserContext.getUserId();
        if (userId.equals(targetUserId)) {
            throw new AppException("不能关注自己");
        }
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new AppException("用户不存在");
        }
        FollowRelation relation = followRelationMapper.selectOne(new LambdaQueryWrapper<FollowRelation>()
                .eq(FollowRelation::getFollowerUserId, userId)
                .eq(FollowRelation::getTargetUserId, targetUserId));
        if (relation == null) {
            FollowRelation create = new FollowRelation();
            create.setFollowerUserId(userId);
            create.setTargetUserId(targetUserId);
            create.setStatus(1);
            create.setCreatedAt(LocalDateTime.now());
            create.setUpdatedAt(LocalDateTime.now());
            followRelationMapper.insert(create);
            behaviorService.record(userId, "FOLLOW", "USER", targetUserId, 2);
            return true;
        }
        relation.setStatus(relation.getStatus() != null && relation.getStatus() == 1 ? 0 : 1);
        relation.setUpdatedAt(LocalDateTime.now());
        followRelationMapper.updateById(relation);
        if (relation.getStatus() == 1) {
            behaviorService.record(userId, "FOLLOW", "USER", targetUserId, 2);
        }
        return relation.getStatus() == 1;
    }

    public List<ContentDtos.ContentListItem> feed(int page, int size) {
        Long userId = UserContext.getUserId();
        // Get all users I follow
        List<FollowRelation> followings = followRelationMapper.selectList(new LambdaQueryWrapper<FollowRelation>()
                .eq(FollowRelation::getFollowerUserId, userId)
                .eq(FollowRelation::getStatus, 1));
        if (followings.isEmpty()) {
            return List.of();
        }
        List<Long> followedUserIds = followings.stream()
                .map(FollowRelation::getTargetUserId)
                .toList();

        List<Content> contents = contentMapper.selectList(new LambdaQueryWrapper<Content>()
                .in(Content::getAuthorId, followedUserIds)
                .eq(Content::getStatus, "PUBLISHED")
                .orderByDesc(Content::getPublishedAt)
                .last("LIMIT " + (page - 1) * size + ", " + size));

        Set<Long> authorIds = contents.stream().map(Content::getAuthorId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return contents.stream().map(content -> {
            ContentDtos.ContentListItem item = new ContentDtos.ContentListItem();
            item.setId(content.getId());
            item.setTitle(content.getTitle());
            item.setSummary(content.getSummary());
            item.setType(content.getType());
            item.setAuthorId(content.getAuthorId());
            User author = userMap.get(content.getAuthorId());
            item.setAuthorName(author != null ? (author.getNickname() != null ? author.getNickname() : author.getUsername()) : "未知用户");
            item.setViewCount(content.getViewCount());
            item.setLikeCount(content.getLikeCount());
            item.setFavoriteCount(content.getFavoriteCount());
            item.setPublishedAt(content.getPublishedAt());
            item.setCreatedAt(content.getCreatedAt());
            return item;
        }).toList();
    }
}
