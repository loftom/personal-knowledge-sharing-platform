package com.zhihu.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhihu.platform.common.AppException;
import com.zhihu.platform.domain.dto.Phase2Dtos;
import com.zhihu.platform.domain.entity.AuditLog;
import com.zhihu.platform.domain.entity.Content;
import com.zhihu.platform.domain.entity.User;
import com.zhihu.platform.domain.mapper.AuditLogMapper;
import com.zhihu.platform.domain.mapper.ContentMapper;
import com.zhihu.platform.domain.mapper.UserMapper;
import com.zhihu.platform.security.UserContext;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminService {

    private final ContentMapper contentMapper;
    private final AuditLogMapper auditLogMapper;
    private final NotificationService notificationService;
    private final PointService pointService;
    private final UserMapper userMapper;

    public AdminService(ContentMapper contentMapper,
                        AuditLogMapper auditLogMapper,
                        NotificationService notificationService,
                        PointService pointService,
                        UserMapper userMapper) {
        this.contentMapper = contentMapper;
        this.auditLogMapper = auditLogMapper;
        this.notificationService = notificationService;
        this.pointService = pointService;
        this.userMapper = userMapper;
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
        String nickname = user.getNickname();
        if (nickname != null && !nickname.trim().isEmpty() && !nickname.trim().matches("^[?]+$")) {
            return nickname.trim();
        }
        String username = user.getUsername();
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        return "用户 " + authorId;
    }
}
