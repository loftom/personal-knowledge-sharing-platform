package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.knowledge.platform.common.AppException;
import com.knowledge.platform.domain.dto.ContentDtos;
import com.knowledge.platform.domain.entity.Comment;
import com.knowledge.platform.domain.entity.Content;
import com.knowledge.platform.domain.entity.FavoriteRecord;
import com.knowledge.platform.domain.entity.FollowRelation;
import com.knowledge.platform.domain.entity.LikeRecord;
import com.knowledge.platform.domain.entity.User;
import com.knowledge.platform.domain.mapper.CommentMapper;
import com.knowledge.platform.domain.mapper.ContentMapper;
import com.knowledge.platform.domain.mapper.FavoriteRecordMapper;
import com.knowledge.platform.domain.mapper.FollowRelationMapper;
import com.knowledge.platform.domain.mapper.LikeRecordMapper;
import com.knowledge.platform.domain.mapper.UserMapper;
import com.knowledge.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class InteractionService {

    private final LikeRecordMapper likeRecordMapper;
    private final FavoriteRecordMapper favoriteRecordMapper;
    private final CommentMapper commentMapper;
    private final ContentMapper contentMapper;
    private final FollowRelationMapper followRelationMapper;
    private final UserMapper userMapper;
    private final BehaviorService behaviorService;
    private final NotificationService notificationService;
    private final PointService pointService;

    public InteractionService(LikeRecordMapper likeRecordMapper,
                              FavoriteRecordMapper favoriteRecordMapper,
                              CommentMapper commentMapper,
                              ContentMapper contentMapper,
                              FollowRelationMapper followRelationMapper,
                              UserMapper userMapper,
                              BehaviorService behaviorService,
                              NotificationService notificationService,
                              PointService pointService) {
        this.likeRecordMapper = likeRecordMapper;
        this.favoriteRecordMapper = favoriteRecordMapper;
        this.commentMapper = commentMapper;
        this.contentMapper = contentMapper;
        this.followRelationMapper = followRelationMapper;
        this.userMapper = userMapper;
        this.behaviorService = behaviorService;
        this.notificationService = notificationService;
        this.pointService = pointService;
    }

    @Transactional
    public boolean toggleLike(ContentDtos.ToggleRequest request) {
        Long userId = UserContext.getUserId();
        if ("CONTENT".equalsIgnoreCase(request.getTargetType())) {
            Content content = contentMapper.selectById(request.getTargetId());
            if (!canInteractWithContent(content, userId)) {
                throw new AppException("Content is not visible");
            }
        }
        LikeRecord exists = likeRecordMapper.selectOne(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, request.getTargetId())
                .eq(LikeRecord::getTargetType, request.getTargetType()));

        if (exists == null) {
            LikeRecord record = new LikeRecord();
            record.setUserId(userId);
            record.setTargetId(request.getTargetId());
            record.setTargetType(request.getTargetType());
            record.setCreatedAt(LocalDateTime.now());
            likeRecordMapper.insert(record);
            if ("CONTENT".equalsIgnoreCase(request.getTargetType())) {
                Content content = contentMapper.selectById(request.getTargetId());
                contentMapper.update(new LambdaUpdateWrapper<Content>()
                        .setSql("like_count = like_count + 1")
                        .eq(Content::getId, request.getTargetId()));
                behaviorService.record(userId, "LIKE", "CONTENT", request.getTargetId(), 3);
                if (content != null) {
                    pointService.rewardContentLiked(content.getAuthorId(), content.getId(), userId);
                }
            } else {
                commentMapper.update(new LambdaUpdateWrapper<Comment>()
                        .setSql("like_count = like_count + 1")
                        .eq(Comment::getId, request.getTargetId()));
            }
            return true;
        }

        likeRecordMapper.deleteById(exists.getId());
        if ("CONTENT".equalsIgnoreCase(request.getTargetType())) {
            contentMapper.update(new LambdaUpdateWrapper<Content>()
                    .setSql("like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END")
                    .eq(Content::getId, request.getTargetId()));
        } else {
            commentMapper.update(new LambdaUpdateWrapper<Comment>()
                    .setSql("like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END")
                    .eq(Comment::getId, request.getTargetId()));
        }
        return false;
    }

    @Transactional
    public boolean toggleFavorite(Long contentId) {
        Long userId = UserContext.getUserId();
        Content content = contentMapper.selectById(contentId);
        if (!canInteractWithContent(content, userId)) {
            throw new AppException("Content is not visible");
        }
        FavoriteRecord exists = favoriteRecordMapper.selectOne(new LambdaQueryWrapper<FavoriteRecord>()
                .eq(FavoriteRecord::getUserId, userId)
                .eq(FavoriteRecord::getContentId, contentId));

        if (exists == null) {
            FavoriteRecord record = new FavoriteRecord();
            record.setUserId(userId);
            record.setContentId(contentId);
            record.setCreatedAt(LocalDateTime.now());
            favoriteRecordMapper.insert(record);
            contentMapper.update(new LambdaUpdateWrapper<Content>()
                    .setSql("favorite_count = favorite_count + 1")
                    .eq(Content::getId, contentId));
            behaviorService.record(userId, "FAVORITE", "CONTENT", contentId, 4);
            return true;
        }

        favoriteRecordMapper.deleteById(exists.getId());
        contentMapper.update(new LambdaUpdateWrapper<Content>()
                .setSql("favorite_count = CASE WHEN favorite_count > 0 THEN favorite_count - 1 ELSE 0 END")
                .eq(Content::getId, contentId));
        return false;
    }

    public Long comment(Long contentId, ContentDtos.CommentRequest request) {
        Content content = contentMapper.selectById(contentId);
        if (!canInteractWithContent(content, UserContext.getUserId())) {
            throw new AppException("Content is not commentable");
        }
        Comment comment = new Comment();
        comment.setContentId(contentId);
        comment.setUserId(UserContext.getUserId());
        comment.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        comment.setBody(request.getBody());
        comment.setLikeCount(0L);
        comment.setStatus(1);
        comment.setCreatedAt(LocalDateTime.now());
        commentMapper.insert(comment);
        behaviorService.record(UserContext.getUserId(), "COMMENT", "CONTENT", contentId, 4);
        notificationService.notifyCommentOrReply(content, comment);
        return comment.getId();
    }

    public List<ContentDtos.CommentView> listComments(Long contentId) {
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getContentId, contentId)
                .eq(Comment::getStatus, 1)
                .orderByAsc(Comment::getCreatedAt));
        List<Long> userIds = comments.stream()
                .map(Comment::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, User> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMap = userMapper.selectBatchIds(userIds).stream()
                    .collect(Collectors.toMap(User::getId, user -> user));
        }
        final Map<Long, User> finalUserMap = userMap;
        return comments.stream()
                .map(comment -> toCommentView(comment, finalUserMap.get(comment.getUserId())))
                .toList();
    }

    public boolean hasLikedContent(Long contentId, Long userId) {
        if (contentId == null || userId == null) {
            return false;
        }
        return likeRecordMapper.selectCount(new LambdaQueryWrapper<LikeRecord>()
                .eq(LikeRecord::getUserId, userId)
                .eq(LikeRecord::getTargetId, contentId)
                .eq(LikeRecord::getTargetType, "CONTENT")) > 0;
    }

    public boolean hasFavoritedContent(Long contentId, Long userId) {
        if (contentId == null || userId == null) {
            return false;
        }
        return favoriteRecordMapper.selectCount(new LambdaQueryWrapper<FavoriteRecord>()
                .eq(FavoriteRecord::getUserId, userId)
                .eq(FavoriteRecord::getContentId, contentId)) > 0;
    }

    private ContentDtos.CommentView toCommentView(Comment comment, User user) {
        ContentDtos.CommentView view = new ContentDtos.CommentView();
        view.setId(comment.getId());
        view.setContentId(comment.getContentId());
        view.setUserId(comment.getUserId());
        view.setParentId(comment.getParentId());
        view.setBody(comment.getBody());
        view.setLikeCount(comment.getLikeCount());
        view.setStatus(comment.getStatus());
        view.setCreatedAt(comment.getCreatedAt());
        if (user != null) {
            view.setUsername(user.getUsername());
            view.setNickname(user.getNickname());
            view.setDisplayName(resolveDisplayName(user.getNickname(), user.getId()));
        } else {
            view.setDisplayName("用户 " + comment.getUserId());
        }
        return view;
    }

    private String resolveDisplayName(String nickname, Long userId) {
        if (nickname != null) {
            String trimmed = nickname.trim();
            if (!trimmed.isEmpty() && !trimmed.matches("[?？]+")) {
                return trimmed;
            }
        }
        return "用户 " + userId;
    }

    private boolean canInteractWithContent(Content content, Long viewerUserId) {
        if (content == null || !"PUBLISHED".equals(content.getStatus())) {
            return false;
        }
        String visibility = content.getVisibility();
        if (visibility == null || visibility.isBlank() || "PUBLIC".equalsIgnoreCase(visibility)) {
            return true;
        }
        if (viewerUserId == null) {
            return false;
        }
        if (viewerUserId.equals(content.getAuthorId())) {
            return true;
        }
        if ("PRIVATE".equalsIgnoreCase(visibility)) {
            return false;
        }
        if ("FOLLOWERS".equalsIgnoreCase(visibility)) {
            return followRelationMapper.selectCount(new LambdaQueryWrapper<FollowRelation>()
                    .eq(FollowRelation::getFollowerUserId, viewerUserId)
                    .eq(FollowRelation::getTargetUserId, content.getAuthorId())
                    .eq(FollowRelation::getStatus, 1)) > 0;
        }
        return false;
    }
}
