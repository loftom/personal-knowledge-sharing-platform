package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.knowledge.platform.common.AppException;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.domain.entity.Comment;
import com.knowledge.platform.domain.entity.QaAnswer;
import com.knowledge.platform.domain.entity.Content;
import com.knowledge.platform.domain.entity.FollowRelation;
import com.knowledge.platform.domain.entity.Notification;
import com.knowledge.platform.domain.mapper.CommentMapper;
import com.knowledge.platform.domain.mapper.FollowRelationMapper;
import com.knowledge.platform.domain.mapper.NotificationMapper;
import com.knowledge.platform.realtime.RealtimePushService;
import com.knowledge.platform.security.UserContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final FollowRelationMapper followRelationMapper;
    private final CommentMapper commentMapper;
    private final RealtimePushService realtimePushService;

    public NotificationService(NotificationMapper notificationMapper,
                               FollowRelationMapper followRelationMapper,
                               CommentMapper commentMapper,
                               RealtimePushService realtimePushService) {
        this.notificationMapper = notificationMapper;
        this.followRelationMapper = followRelationMapper;
        this.commentMapper = commentMapper;
        this.realtimePushService = realtimePushService;
    }

    public void notifyBestAnswer(QaAnswer answer, Content content) {
        if (answer == null || content == null) return;
        Notification n = new Notification();
        n.setUserId(answer.getUserId());
        n.setType("BEST_ANSWER");
        n.setTitle("你的回答被采纳为最佳答案");
        n.setContent(content.getTitle());
        n.setRelatedId(answer.getId());
        n.setIsRead(0);
        n.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(n);
        realtimePushService.pushAfterCommit(n.getUserId(), "notification", Map.of(
            "notificationType", n.getType(),
            "relatedId", n.getRelatedId()
        ));
    }

    public void notifyFollowersNewContent(Content content) {
        if (content == null || content.getAuthorId() == null) {
            return;
        }
        List<FollowRelation> followers = followRelationMapper.selectList(new LambdaQueryWrapper<FollowRelation>()
                .eq(FollowRelation::getTargetUserId, content.getAuthorId())
                .eq(FollowRelation::getStatus, 1));
        for (FollowRelation relation : followers) {
            Notification n = new Notification();
            n.setUserId(relation.getFollowerUserId());
            n.setType("NEW_CONTENT");
            n.setTitle("你关注的作者发布了新内容");
            n.setContent(content.getTitle());
            n.setRelatedId(content.getId());
            n.setIsRead(0);
            n.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(n);
            realtimePushService.pushAfterCommit(n.getUserId(), "notification", Map.of(
                    "notificationType", n.getType(),
                    "relatedId", n.getRelatedId()
            ));
        }
    }

    public void notifyCommentOrReply(Content content, Comment comment) {
        if (content == null || comment == null) {
            return;
        }
        Long actorId = comment.getUserId();
        Long contentAuthorId = content.getAuthorId();

        if (!actorId.equals(contentAuthorId)) {
            Notification n = new Notification();
            n.setUserId(contentAuthorId);
            n.setType("COMMENT");
            n.setTitle("你的内容有新评论");
            n.setContent(comment.getBody());
            n.setRelatedId(content.getId());
            n.setIsRead(0);
            n.setCreatedAt(LocalDateTime.now());
            notificationMapper.insert(n);
            realtimePushService.pushAfterCommit(n.getUserId(), "notification", Map.of(
                    "notificationType", n.getType(),
                    "relatedId", n.getRelatedId()
            ));
        }

        if (comment.getParentId() != null && comment.getParentId() > 0) {
            Comment parent = commentMapper.selectById(comment.getParentId());
            if (parent != null && !parent.getUserId().equals(actorId) && !parent.getUserId().equals(contentAuthorId)) {
                Notification reply = new Notification();
                reply.setUserId(parent.getUserId());
                reply.setType("REPLY");
                reply.setTitle("你的评论收到回复");
                reply.setContent(comment.getBody());
                reply.setRelatedId(content.getId());
                reply.setIsRead(0);
                reply.setCreatedAt(LocalDateTime.now());
                notificationMapper.insert(reply);
                realtimePushService.pushAfterCommit(reply.getUserId(), "notification", Map.of(
                        "notificationType", reply.getType(),
                        "relatedId", reply.getRelatedId()
                ));
            }
        }
    }

    public List<Phase2Dtos.NotificationItem> myNotifications(Boolean unreadOnly, Integer page, Integer size) {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .orderByDesc(Notification::getCreatedAt);
        if (Boolean.TRUE.equals(unreadOnly)) {
            wrapper.eq(Notification::getIsRead, 0);
        }
        List<Notification> all = notificationMapper.selectList(wrapper);
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safeSize = Math.max(size == null ? 20 : size, 1);
        int from = (safePage - 1) * safeSize;
        if (from >= all.size()) {
            return Collections.emptyList();
        }
        int to = Math.min(from + safeSize, all.size());
        return all.subList(from, to).stream().map(this::toItem).collect(Collectors.toList());
    }

    public void markRead(Long id) {
        Long userId = UserContext.getUserId();
        Notification n = notificationMapper.selectById(id);
        if (n == null || !userId.equals(n.getUserId())) {
            throw new AppException("通知不存在");
        }
        n.setIsRead(1);
        n.setReadAt(LocalDateTime.now());
        notificationMapper.updateById(n);
        realtimePushService.pushAfterCommit(userId, "notification-read", Map.of("notificationId", id));
    }

    public void markAllRead() {
        Long userId = UserContext.getUserId();
        notificationMapper.update(new LambdaUpdateWrapper<Notification>()
                .set(Notification::getIsRead, 1)
                .set(Notification::getReadAt, LocalDateTime.now())
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        realtimePushService.pushAfterCommit(userId, "notification-read", Map.of("notificationId", 0));
    }

    private Phase2Dtos.NotificationItem toItem(Notification n) {
        Phase2Dtos.NotificationItem item = new Phase2Dtos.NotificationItem();
        item.setId(n.getId());
        item.setType(n.getType());
        item.setTitle(n.getTitle());
        item.setContent(n.getContent());
        item.setRelatedId(n.getRelatedId());
        item.setIsRead(n.getIsRead());
        item.setCreatedAt(n.getCreatedAt());
        return item;
    }
}
