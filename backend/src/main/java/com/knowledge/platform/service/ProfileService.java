package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.platform.common.AppException;
import com.knowledge.platform.domain.dto.ContentDtos;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.domain.entity.BadgeUser;
import com.knowledge.platform.domain.entity.Comment;
import com.knowledge.platform.domain.entity.Content;
import com.knowledge.platform.domain.entity.FavoriteRecord;
import com.knowledge.platform.domain.entity.FollowRelation;
import com.knowledge.platform.domain.entity.LikeRecord;
import com.knowledge.platform.domain.entity.Notification;
import com.knowledge.platform.domain.entity.PointAccount;
import com.knowledge.platform.domain.entity.PointLog;
import com.knowledge.platform.domain.entity.QaAnswer;
import com.knowledge.platform.domain.entity.User;
import com.knowledge.platform.domain.entity.UserBehaviorEvent;
import com.knowledge.platform.domain.mapper.BadgeUserMapper;
import com.knowledge.platform.domain.mapper.CommentMapper;
import com.knowledge.platform.domain.mapper.ContentMapper;
import com.knowledge.platform.domain.mapper.FavoriteRecordMapper;
import com.knowledge.platform.domain.mapper.FollowRelationMapper;
import com.knowledge.platform.domain.mapper.LikeRecordMapper;
import com.knowledge.platform.domain.mapper.NotificationMapper;
import com.knowledge.platform.domain.mapper.PointAccountMapper;
import com.knowledge.platform.domain.mapper.PointLogMapper;
import com.knowledge.platform.domain.mapper.QaAnswerMapper;
import com.knowledge.platform.domain.mapper.UserBehaviorEventMapper;
import com.knowledge.platform.domain.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final UserMapper userMapper;
    private final ContentMapper contentMapper;
    private final FavoriteRecordMapper favoriteRecordMapper;
    private final FollowRelationMapper followRelationMapper;
    private final CommentMapper commentMapper;
    private final LikeRecordMapper likeRecordMapper;
    private final NotificationMapper notificationMapper;
    private final UserBehaviorEventMapper userBehaviorEventMapper;
    private final PointAccountMapper pointAccountMapper;
    private final PointLogMapper pointLogMapper;
    private final BadgeUserMapper badgeUserMapper;
    private final QaAnswerMapper qaAnswerMapper;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public ProfileService(UserMapper userMapper,
                          ContentMapper contentMapper,
                          FavoriteRecordMapper favoriteRecordMapper,
                          FollowRelationMapper followRelationMapper,
                          CommentMapper commentMapper,
                          LikeRecordMapper likeRecordMapper,
                          NotificationMapper notificationMapper,
                          UserBehaviorEventMapper userBehaviorEventMapper,
                          PointAccountMapper pointAccountMapper,
                          PointLogMapper pointLogMapper,
                          BadgeUserMapper badgeUserMapper,
                          QaAnswerMapper qaAnswerMapper) {
        this.userMapper = userMapper;
        this.contentMapper = contentMapper;
        this.favoriteRecordMapper = favoriteRecordMapper;
        this.followRelationMapper = followRelationMapper;
        this.commentMapper = commentMapper;
        this.likeRecordMapper = likeRecordMapper;
        this.notificationMapper = notificationMapper;
        this.userBehaviorEventMapper = userBehaviorEventMapper;
        this.pointAccountMapper = pointAccountMapper;
        this.pointLogMapper = pointLogMapper;
        this.badgeUserMapper = badgeUserMapper;
        this.qaAnswerMapper = qaAnswerMapper;
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
        User user = requireUser(userId);
        user.setNickname(nickname.trim());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        Phase2Dtos.UserSimple simple = new Phase2Dtos.UserSimple();
        simple.setId(user.getId());
        simple.setUsername(user.getUsername());
        simple.setNickname(user.getNickname());
        return simple;
    }

    public void resetPassword(Long userId, String newPassword) {
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new AppException("新密码长度不能少于 6 位");
        }
        User user = requireUser(userId);
        user.setPasswordHash(encoder.encode(newPassword.trim()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    public void deactivateAccount(Long userId) {
        User user = requireUser(userId);
        if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new AppException("管理员账号不支持在前台直接注销");
        }

        LocalDateTime now = LocalDateTime.now();
        List<Content> contents = contentMapper.selectList(new LambdaQueryWrapper<Content>()
                .eq(Content::getAuthorId, userId));
        for (Content content : contents) {
            content.setStatus("OFFLINE");
            content.setVisibility("PRIVATE");
            content.setUpdatedAt(now);
            contentMapper.updateById(content);
        }

        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getUserId, userId));
        for (Comment comment : comments) {
            comment.setBody("该评论已随账号注销而移除");
            comment.setStatus(0);
            commentMapper.updateById(comment);
        }

        List<QaAnswer> answers = qaAnswerMapper.selectList(new LambdaQueryWrapper<QaAnswer>()
                .eq(QaAnswer::getUserId, userId));
        for (QaAnswer answer : answers) {
            answer.setBody("该回答已随账号注销而移除");
            answer.setStatus(0);
            answer.setUpdatedAt(now);
            qaAnswerMapper.updateById(answer);
        }

        favoriteRecordMapper.delete(new LambdaQueryWrapper<FavoriteRecord>().eq(FavoriteRecord::getUserId, userId));
        likeRecordMapper.delete(new LambdaQueryWrapper<LikeRecord>().eq(LikeRecord::getUserId, userId));
        followRelationMapper.delete(new LambdaQueryWrapper<FollowRelation>()
                .eq(FollowRelation::getFollowerUserId, userId)
                .or()
                .eq(FollowRelation::getTargetUserId, userId));
        notificationMapper.delete(new LambdaQueryWrapper<Notification>().eq(Notification::getUserId, userId));
        userBehaviorEventMapper.delete(new LambdaQueryWrapper<UserBehaviorEvent>().eq(UserBehaviorEvent::getUserId, userId));
        pointAccountMapper.delete(new LambdaQueryWrapper<PointAccount>().eq(PointAccount::getUserId, userId));
        pointLogMapper.delete(new LambdaQueryWrapper<PointLog>().eq(PointLog::getUserId, userId));
        badgeUserMapper.delete(new LambdaQueryWrapper<BadgeUser>().eq(BadgeUser::getUserId, userId));

        user.setNickname("已注销用户");
        user.setUsername("deleted_" + user.getId() + "_" + System.currentTimeMillis());
        user.setPasswordHash(encoder.encode(UUID.randomUUID().toString()));
        user.setStatus(0);
        user.setUpdatedAt(now);
        userMapper.updateById(user);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException("User not found");
        }
        return user;
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
