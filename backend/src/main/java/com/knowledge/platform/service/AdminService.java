package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.platform.common.AppException;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.domain.entity.AuditLog;
import com.knowledge.platform.domain.entity.Comment;
import com.knowledge.platform.domain.entity.Content;
import com.knowledge.platform.domain.entity.FollowRelation;
import com.knowledge.platform.domain.entity.Tag;
import com.knowledge.platform.domain.entity.User;
import com.knowledge.platform.domain.mapper.AuditLogMapper;
import com.knowledge.platform.domain.mapper.CommentMapper;
import com.knowledge.platform.domain.mapper.ContentMapper;
import com.knowledge.platform.domain.mapper.FollowRelationMapper;
import com.knowledge.platform.domain.mapper.TagMapper;
import com.knowledge.platform.domain.mapper.UserMapper;
import com.knowledge.platform.security.UserContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final ContentMapper contentMapper;
    private final AuditLogMapper auditLogMapper;
    private final NotificationService notificationService;
    private final PointService pointService;
    private final UserMapper userMapper;
    private final BehaviorService behaviorService;
    private final TagMapper tagMapper;
    private final CommentMapper commentMapper;
    private final FollowRelationMapper followRelationMapper;

    public AdminService(ContentMapper contentMapper,
                        AuditLogMapper auditLogMapper,
                        NotificationService notificationService,
                        PointService pointService,
                        UserMapper userMapper,
                        BehaviorService behaviorService,
                        TagMapper tagMapper,
                        CommentMapper commentMapper,
                        FollowRelationMapper followRelationMapper) {
        this.contentMapper = contentMapper;
        this.auditLogMapper = auditLogMapper;
        this.notificationService = notificationService;
        this.pointService = pointService;
        this.userMapper = userMapper;
        this.behaviorService = behaviorService;
        this.tagMapper = tagMapper;
        this.commentMapper = commentMapper;
        this.followRelationMapper = followRelationMapper;
    }

    public List<Content> pendingList() {
        requireAdmin();
        return listByStatus("PENDING_REVIEW");
    }

    public List<Content> listByStatus(String status) {
        requireAdmin();
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<Content>()
                .orderByDesc(Content::getCreatedAt);
        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            wrapper.eq(Content::getStatus, status);
        }
        return contentMapper.selectList(wrapper).stream()
                .peek(item -> item.setAuthorName(resolveAuthorName(item.getAuthorId())))
                .toList();
    }

    public Phase2Dtos.AuditOverview overview() {
        requireAdmin();
        Phase2Dtos.AuditOverview overview = new Phase2Dtos.AuditOverview();
        overview.setPendingCount(countByStatus("PENDING_REVIEW"));
        overview.setPublishedCount(countByStatus("PUBLISHED"));
        overview.setOfflineCount(countByStatus("OFFLINE"));
        overview.setArticlePendingCount(contentMapper.selectCount(new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, "PENDING_REVIEW")
                .eq(Content::getType, "ARTICLE")));
        overview.setQuestionPendingCount(contentMapper.selectCount(new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, "PENDING_REVIEW")
                .eq(Content::getType, "QUESTION")));
        return overview;
    }

    public Phase2Dtos.AdminDashboardResponse dashboard() {
        requireAdmin();
        List<Content> contents = contentMapper.selectList(new LambdaQueryWrapper<Content>());
        long activeUserCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1));
        long deletedUserCount = userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 0));
        long totalUserCount = activeUserCount;
        long totalContentCount = contents.size();
        List<Content> publishedContents = contents.stream()
                .filter(item -> "PUBLISHED".equals(item.getStatus()))
                .toList();
        long publishedContentCount = publishedContents.size();
        long articleCount = publishedContents.stream()
                .filter(item -> "ARTICLE".equals(item.getType()))
                .count();
        long questionCount = publishedContents.stream()
                .filter(item -> "QUESTION".equals(item.getType()))
                .count();
        long totalViewCount = publishedContents.stream().mapToLong(item -> nullSafe(item.getViewCount())).sum();
        long totalLikeCount = publishedContents.stream().mapToLong(item -> nullSafe(item.getLikeCount())).sum();
        long totalFavoriteCount = publishedContents.stream().mapToLong(item -> nullSafe(item.getFavoriteCount())).sum();
        long totalCommentCount = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getStatus, 1));
        long totalFollowerCount = followRelationMapper.selectCount(new LambdaQueryWrapper<FollowRelation>()
                .eq(FollowRelation::getStatus, 1));

        Phase2Dtos.AdminDashboardResponse response = new Phase2Dtos.AdminDashboardResponse();
        response.setTotalUserCount(totalUserCount);
        response.setActiveUserCount(activeUserCount);
        response.setDeletedUserCount(deletedUserCount);
        response.setTotalContentCount(totalContentCount);
        response.setPublishedContentCount(publishedContentCount);
        response.setArticleCount(articleCount);
        response.setQuestionCount(questionCount);
        response.setTotalViewCount(totalViewCount);
        response.setTotalLikeCount(totalLikeCount);
        response.setTotalFavoriteCount(totalFavoriteCount);
        response.setTotalCommentCount(totalCommentCount);
        response.setTotalFollowerCount(totalFollowerCount);
        return response;
    }

    public List<Phase2Dtos.UserPreferenceView> userPreferences() {
        requireAdmin();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getStatus, 1)
                .orderByAsc(User::getId));
        List<Tag> tags = tagMapper.selectList(new LambdaQueryWrapper<Tag>().orderByAsc(Tag::getId));
        Map<Long, String> tagNameMap = tags.stream().collect(Collectors.toMap(Tag::getId, Tag::getName));

        List<Phase2Dtos.UserPreferenceView> result = new ArrayList<>();
        for (User user : users) {
            Map<Long, Double> rawPreference = behaviorService.buildUserTagPreference(user.getId());
            List<Phase2Dtos.UserPreferenceTag> preferenceTags = rawPreference.entrySet().stream()
                    .filter(entry -> entry.getValue() != null && entry.getValue() > 0)
                    .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                    .limit(8)
                    .map(entry -> {
                        Phase2Dtos.UserPreferenceTag tag = new Phase2Dtos.UserPreferenceTag();
                        tag.setTagId(entry.getKey());
                        tag.setTagName(tagNameMap.getOrDefault(entry.getKey(), "标签 " + entry.getKey()));
                        tag.setWeight(entry.getValue());
                        return tag;
                    })
                    .toList();

            double totalWeight = rawPreference.values().stream().mapToDouble(Double::doubleValue).sum();
            Phase2Dtos.UserPreferenceView item = new Phase2Dtos.UserPreferenceView();
            item.setUserId(user.getId());
            item.setUsername(user.getUsername());
            item.setNickname(user.getNickname());
            item.setDisplayName(resolveDisplayName(user));
            item.setRole(user.getRole());
            item.setTotalWeight(totalWeight);
            item.setTagCount(preferenceTags.size());
            item.setPreferences(preferenceTags);
            result.add(item);
        }

        result.sort(Comparator.comparing(Phase2Dtos.UserPreferenceView::getTotalWeight, Comparator.nullsLast(Double::compareTo)).reversed());
        return result;
    }

    public void approve(Long contentId) {
        requireAdmin();
        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new AppException("Content not found");
        }
        if (!"PENDING_REVIEW".equals(content.getStatus())) {
            throw new AppException("Only pending content can be approved");
        }
        content.setStatus("PUBLISHED");
        if (content.getPublishedAt() == null) {
            content.setPublishedAt(LocalDateTime.now());
        }
        contentMapper.updateById(content);
        notificationService.notifyFollowersNewContent(content);
        pointService.rewardPublishApproved(content.getAuthorId(), content.getId());
        saveAudit(contentId, "APPROVE", "Audit passed");
    }

    public void reject(Long contentId, String reason) {
        requireAdmin();
        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new AppException("Content not found");
        }
        if (!"PENDING_REVIEW".equals(content.getStatus())) {
            throw new AppException("Only pending content can be rejected");
        }
        content.setStatus("REJECTED");
        content.setPublishedAt(null);
        content.setUpdatedAt(LocalDateTime.now());
        contentMapper.updateById(content);
        saveAudit(contentId, "REJECT", reason == null ? "Audit rejected" : reason);
    }

    public void offline(Long contentId, String reason) {
        requireAdmin();
        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new AppException("Content not found");
        }
        if (!"PUBLISHED".equals(content.getStatus())) {
            throw new AppException("Only published content can be taken offline");
        }
        content.setStatus("OFFLINE");
        content.setUpdatedAt(LocalDateTime.now());
        contentMapper.updateById(content);
        pointService.penalizeOffline(content.getAuthorId(), content.getId());
        saveAudit(contentId, "OFFLINE", reason == null ? "Admin offline" : reason);
    }

    private void saveAudit(Long contentId, String action, String reason) {
        AuditLog log = new AuditLog();
        log.setContentId(contentId);
        log.setOperatorId(UserContext.getUserId());
        log.setAction(action);
        log.setReason(reason);
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw new AppException("Admin role required");
        }
    }

    private long countByStatus(String status) {
        return contentMapper.selectCount(new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, status));
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private String resolveAuthorName(Long authorId) {
        if (authorId == null) {
            return "匿名用户";
        }
        User user = userMapper.selectById(authorId);
        if (user == null) {
            return "用户 " + authorId;
        }
        return resolveDisplayName(user);
    }

    private String resolveDisplayName(User user) {
        if (user == null) {
            return "匿名用户";
        }
        String nickname = user.getNickname();
        if (nickname != null && !nickname.trim().isEmpty() && !nickname.trim().matches("^[?]+$")) {
            return nickname.trim();
        }
        String username = user.getUsername();
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        return "用户 " + user.getId();
    }
}
