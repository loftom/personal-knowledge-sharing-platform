package com.zhihu.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhihu.platform.domain.dto.ContentDtos;
import com.zhihu.platform.domain.dto.Phase2Dtos;
import com.zhihu.platform.domain.entity.*;
import com.zhihu.platform.domain.mapper.*;
import com.zhihu.platform.common.AppException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final UserMapper userMapper;
    private final ContentMapper contentMapper;
    private final FavoriteRecordMapper favoriteRecordMapper;
    private final FollowRelationMapper followRelationMapper;
    private final CommentMapper commentMapper;

    public ProfileService(UserMapper userMapper,
                          ContentMapper contentMapper,
                          FavoriteRecordMapper favoriteRecordMapper,
                          FollowRelationMapper followRelationMapper,
                          CommentMapper commentMapper) {
        this.userMapper = userMapper;
        this.contentMapper = contentMapper;
        this.favoriteRecordMapper = favoriteRecordMapper;
        this.followRelationMapper = followRelationMapper;
        this.commentMapper = commentMapper;
    }

    public Phase2Dtos.PersonalSpaceResponse space(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return null;
        }

        List<Content> worksRaw = contentMapper.selectList(new LambdaQueryWrapper<Content>()
                .eq(Content::getAuthorId, userId)
                .eq(Content::getStatus, "PUBLISHED")
                .orderByDesc(Content::getCreatedAt)
                .last("LIMIT 30"));

        List<FavoriteRecord> favoriteRecords = favoriteRecordMapper.selectList(new LambdaQueryWrapper<FavoriteRecord>()
                .eq(FavoriteRecord::getUserId, userId)
                .orderByDesc(FavoriteRecord::getCreatedAt)
                .last("LIMIT 30"));
        List<Content> favoriteContents = new ArrayList<>();
        for (FavoriteRecord record : favoriteRecords) {
            Content c = contentMapper.selectById(record.getContentId());
            if (c != null && "PUBLISHED".equals(c.getStatus())) {
                favoriteContents.add(c);
            }
        }

        long totalLikesReceived = worksRaw.stream().mapToLong(c -> c.getLikeCount() == null ? 0 : c.getLikeCount()).sum();
        long totalFavoritesReceived = worksRaw.stream().mapToLong(c -> c.getFavoriteCount() == null ? 0 : c.getFavoriteCount()).sum();

        long totalCommentsReceived = 0;
        if (!worksRaw.isEmpty()) {
            List<Long> workIds = worksRaw.stream().map(Content::getId).toList();
            totalCommentsReceived = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                    .in(Comment::getContentId, workIds)
                    .eq(Comment::getStatus, 1));
        }

        long followerCount = followRelationMapper.selectCount(new LambdaQueryWrapper<FollowRelation>()
                .eq(FollowRelation::getTargetUserId, userId)
                .eq(FollowRelation::getStatus, 1));
        long followingCount = followRelationMapper.selectCount(new LambdaQueryWrapper<FollowRelation>()
                .eq(FollowRelation::getFollowerUserId, userId)
                .eq(FollowRelation::getStatus, 1));

        Phase2Dtos.PersonalSpaceResponse response = new Phase2Dtos.PersonalSpaceResponse();

        Phase2Dtos.UserSimple simple = new Phase2Dtos.UserSimple();
        simple.setId(user.getId());
        simple.setUsername(user.getUsername());
        simple.setNickname(user.getNickname());
        response.setUser(simple);

        Phase2Dtos.PersonalStats stats = new Phase2Dtos.PersonalStats();
        stats.setContentCount((long) worksRaw.size());
        stats.setFavoriteCount((long) favoriteContents.size());
        stats.setTotalLikesReceived(totalLikesReceived);
        stats.setTotalFavoritesReceived(totalFavoritesReceived);
        stats.setTotalCommentsReceived(totalCommentsReceived);
        stats.setFollowerCount(followerCount);
        stats.setFollowingCount(followingCount);
        response.setStats(stats);

        response.setWorks(toContentItems(worksRaw));
        response.setFavorites(toContentItems(favoriteContents));
        return response;
    }

    public List<Phase2Dtos.UserSimple> followers(Long userId) {
        List<FollowRelation> relations = followRelationMapper.selectList(new LambdaQueryWrapper<FollowRelation>()
                .eq(FollowRelation::getTargetUserId, userId)
                .eq(FollowRelation::getStatus, 1)
                .orderByDesc(FollowRelation::getCreatedAt));
        return toUsers(relations.stream().map(FollowRelation::getFollowerUserId).collect(Collectors.toList()));
    }

    public List<Phase2Dtos.UserSimple> followings(Long userId) {
        List<FollowRelation> relations = followRelationMapper.selectList(new LambdaQueryWrapper<FollowRelation>()
                .eq(FollowRelation::getFollowerUserId, userId)
                .eq(FollowRelation::getStatus, 1)
                .orderByDesc(FollowRelation::getCreatedAt));
        return toUsers(relations.stream().map(FollowRelation::getTargetUserId).collect(Collectors.toList()));
    }

    public Phase2Dtos.UserSimple updateNickname(Long userId, String nickname) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException("User not found");
        }
        user.setNickname(nickname.trim());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        Phase2Dtos.UserSimple simple = new Phase2Dtos.UserSimple();
        simple.setId(user.getId());
        simple.setUsername(user.getUsername());
        simple.setNickname(user.getNickname());
        return simple;
    }

    private List<Phase2Dtos.UserSimple> toUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        List<Phase2Dtos.UserSimple> users = new ArrayList<>();
        for (Long id : ids) {
            User user = userMapper.selectById(id);
            if (user != null) {
                Phase2Dtos.UserSimple simple = new Phase2Dtos.UserSimple();
                simple.setId(user.getId());
                simple.setUsername(user.getUsername());
                simple.setNickname(user.getNickname());
                users.add(simple);
            }
        }
        return users;
    }

    private List<ContentDtos.ContentListItem> toContentItems(List<Content> contents) {
        return contents.stream().map(c -> {
            ContentDtos.ContentListItem item = new ContentDtos.ContentListItem();
            item.setId(c.getId());
            item.setType(c.getType());
            item.setTitle(c.getTitle());
            item.setSummary(c.getSummary());
            item.setStatus(c.getStatus());
            item.setAuthorId(c.getAuthorId());
            item.setCategoryId(c.getCategoryId());
            item.setVisibility(c.getVisibility());
            item.setViewCount(c.getViewCount());
            item.setLikeCount(c.getLikeCount());
            item.setFavoriteCount(c.getFavoriteCount());
            item.setPublishedAt(c.getPublishedAt());
            item.setCreatedAt(c.getCreatedAt());
            return item;
        }).collect(Collectors.toList());
    }
}
