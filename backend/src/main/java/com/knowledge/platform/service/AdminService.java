package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.platform.common.AppException;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.domain.entity.AuditLog;
import com.knowledge.platform.domain.entity.Content;
import com.knowledge.platform.domain.entity.Tag;
import com.knowledge.platform.domain.entity.User;
import com.knowledge.platform.domain.mapper.AuditLogMapper;
import com.knowledge.platform.domain.mapper.ContentMapper;
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

    public AdminService(ContentMapper contentMapper,
                        AuditLogMapper auditLogMapper,
                        NotificationService notificationService,
                        PointService pointService,
                        UserMapper userMapper,
                        BehaviorService behaviorService,
                        TagMapper tagMapper) {
        this.contentMapper = contentMapper;
        this.auditLogMapper = auditLogMapper;
        this.notificationService = notificationService;
        this.pointService = pointService;
        this.userMapper = userMapper;
        this.behaviorService = behaviorService;
        this.tagMapper = tagMapper;
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

    public List<Phase2Dtos.UserPreferenceView> userPreferences() {
        requireAdmin();
        List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>().orderByAsc(User::getId));
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
        content.setStatus("REJECTED");
        contentMapper.updateById(content);
        saveAudit(contentId, "REJECT", reason == null ? "Audit rejected" : reason);
    }

    public void offline(Long contentId, String reason) {
        requireAdmin();
        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new AppException("Content not found");
        }
        content.setStatus("OFFLINE");
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
