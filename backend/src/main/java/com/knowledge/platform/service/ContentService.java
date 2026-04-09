package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.knowledge.platform.common.AppException;
import com.knowledge.platform.domain.dto.ContentDtos;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.domain.entity.AuditLog;
import com.knowledge.platform.domain.entity.Content;
import com.knowledge.platform.domain.entity.ContentTag;
import com.knowledge.platform.domain.entity.FollowRelation;
import com.knowledge.platform.domain.entity.User;
import com.knowledge.platform.domain.mapper.AuditLogMapper;
import com.knowledge.platform.domain.mapper.CategoryMapper;
import com.knowledge.platform.domain.mapper.ContentMapper;
import com.knowledge.platform.domain.mapper.ContentTagMapper;
import com.knowledge.platform.domain.mapper.FollowRelationMapper;
import com.knowledge.platform.domain.mapper.TagMapper;
import com.knowledge.platform.domain.mapper.UserMapper;
import com.knowledge.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContentService {

    private final ContentMapper contentMapper;
    private final ContentTagMapper contentTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;
    private final FollowRelationMapper followRelationMapper;
    private final AuditLogMapper auditLogMapper;
    private final AuditService auditService;
    private final QaService qaService;
    private final BehaviorService behaviorService;
    private final InteractionService interactionService;

    public ContentService(ContentMapper contentMapper,
                          ContentTagMapper contentTagMapper,
                          CategoryMapper categoryMapper,
                          TagMapper tagMapper,
                          UserMapper userMapper,
                          FollowRelationMapper followRelationMapper,
                          AuditLogMapper auditLogMapper,
                          AuditService auditService,
                          QaService qaService,
                          BehaviorService behaviorService,
                          InteractionService interactionService) {
        this.contentMapper = contentMapper;
        this.contentTagMapper = contentTagMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.userMapper = userMapper;
        this.followRelationMapper = followRelationMapper;
        this.auditLogMapper = auditLogMapper;
        this.auditService = auditService;
        this.qaService = qaService;
        this.behaviorService = behaviorService;
        this.interactionService = interactionService;
    }

    @Transactional
    public Long create(ContentDtos.CreateContentRequest request) {
        validateCategoryAndTags(request.getCategoryId(), request.getTagIds());
        Content content = new Content();
        content.setAuthorId(UserContext.getUserId());
        content.setType(request.getType());
        content.setTitle(request.getTitle());
        content.setSummary(request.getSummary());
        content.setBody(request.getBody());
        content.setCategoryId(request.getCategoryId());
        content.setVisibility(request.getVisibility());
        content.setViewCount(0L);
        content.setLikeCount(0L);
        content.setFavoriteCount(0L);
        content.setCreatedAt(LocalDateTime.now());
        content.setUpdatedAt(LocalDateTime.now());

        String requestedStatus = request.getStatus();
        String hitWord = auditService.hitSensitiveWord(request.getTitle() + " " + request.getSummary() + " " + request.getBody());
        if ("DRAFT".equalsIgnoreCase(requestedStatus)) {
            content.setStatus("DRAFT");
        } else if (hitWord != null) {
            content.setStatus("REJECTED");
        } else {
            content.setStatus("PENDING_REVIEW");
        }

        contentMapper.insert(content);
        saveTags(content.getId(), request.getTagIds());
        qaService.initQuestionIfNeeded(content);

        behaviorService.record(UserContext.getUserId(), "PUBLISH", "CONTENT", content.getId(), 2);

        if (hitWord != null) {
            AuditLog log = new AuditLog();
            log.setContentId(content.getId());
            log.setOperatorId(UserContext.getUserId());
            log.setAction("AUTO_BLOCK");
            log.setReason("Sensitive word hit: " + hitWord);
            log.setCreatedAt(LocalDateTime.now());
            auditLogMapper.insert(log);
        }
        return content.getId();
    }

    @Transactional
    public void update(Long id, ContentDtos.UpdateContentRequest request) {
        Content db = contentMapper.selectById(id);
        if (db == null) {
            throw new AppException("Content not found");
        }
        if (!db.getAuthorId().equals(UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new AppException("No permission to update content");
        }

        validateCategoryAndTags(request.getCategoryId(), request.getTagIds());

        db.setTitle(request.getTitle());
        db.setSummary(request.getSummary());
        db.setBody(request.getBody());
        db.setCategoryId(request.getCategoryId());
        db.setVisibility(request.getVisibility());
        db.setUpdatedAt(LocalDateTime.now());

        String requestedStatus = request.getStatus();
        String oldStatus = db.getStatus();
        String hitWord = auditService.hitSensitiveWord(request.getTitle() + " " + request.getSummary() + " " + request.getBody());
        if ("DRAFT".equalsIgnoreCase(requestedStatus)) {
            db.setStatus("DRAFT");
        } else if (hitWord != null) {
            db.setStatus("REJECTED");
            db.setPublishedAt(null);
        } else if (!"OFFLINE".equals(oldStatus) && !"PUBLISHED".equals(oldStatus)) {
            db.setStatus("PENDING_REVIEW");
            db.setPublishedAt(null);
        }

        contentMapper.updateById(db);
        contentTagMapper.delete(new LambdaQueryWrapper<ContentTag>().eq(ContentTag::getContentId, id));
        saveTags(id, request.getTagIds());

        if (hitWord != null) {
            AuditLog log = new AuditLog();
            log.setContentId(db.getId());
            log.setOperatorId(UserContext.getUserId());
            log.setAction("AUTO_BLOCK");
            log.setReason("Sensitive word hit: " + hitWord);
            log.setCreatedAt(LocalDateTime.now());
            auditLogMapper.insert(log);
        }
    }

    public void delete(Long id) {
        Content db = contentMapper.selectById(id);
        if (db == null) {
            return;
        }
        if (!db.getAuthorId().equals(UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new AppException("No permission to delete content");
        }
        contentMapper.deleteById(id);
        contentTagMapper.delete(new LambdaQueryWrapper<ContentTag>().eq(ContentTag::getContentId, id));
    }

    public Content detail(Long id, boolean increaseView) {
        Content content = contentMapper.selectById(id);
        if (content == null) {
            throw new AppException("Content not found");
        }
        Long currentUserId = UserContext.getUserId();
        if (!canViewContent(content, currentUserId)) {
            throw new AppException("Content is not visible");
        }

        if (increaseView) {
            contentMapper.update(new LambdaUpdateWrapper<Content>()
                    .setSql("view_count = view_count + 1")
                    .eq(Content::getId, id));
            content.setViewCount(content.getViewCount() + 1);
        }
        content.setAuthorName(resolveAuthorName(content.getAuthorId()));
        content.setLiked(interactionService.hasLikedContent(id, currentUserId));
        content.setFavorited(interactionService.hasFavoritedContent(id, currentUserId));
        behaviorService.record(currentUserId, "VIEW", "CONTENT", id, 1);
        return content;
    }

    public List<ContentDtos.ContentListItem> search(String keyword,
                                                    Long categoryId,
                                                    Long tagId,
                                                    String sortBy,
                                                    Integer page,
                                                    Integer size) {
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Content::getStatus, "PUBLISHED");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(Content::getTitle, keyword).or().like(Content::getBody, keyword));
        }
        if (categoryId != null) {
            wrapper.eq(Content::getCategoryId, categoryId);
        }
        if ("likes".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(Content::getLikeCount);
        } else if ("favorites".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(Content::getFavoriteCount);
        } else if ("views".equalsIgnoreCase(sortBy)) {
            wrapper.orderByDesc(Content::getViewCount);
        } else {
            wrapper.orderByDesc(Content::getPublishedAt);
        }

        List<Content> all = contentMapper.selectList(wrapper)
                .stream()
                .filter(content -> canViewContent(content, UserContext.getUserId()))
                .toList();
        if (tagId != null) {
            List<Long> taggedIds = contentTagMapper.selectList(new LambdaQueryWrapper<ContentTag>()
                            .eq(ContentTag::getTagId, tagId))
                    .stream()
                    .map(ContentTag::getContentId)
                    .toList();
            all = all.stream().filter(c -> taggedIds.contains(c.getId())).toList();
        }

        int safePage = Math.max(page == null ? 1 : page, 1);
        int safeSize = Math.max(size == null ? 10 : size, 1);
        int from = (safePage - 1) * safeSize;
        if (from >= all.size()) {
            return Collections.emptyList();
        }
        int to = Math.min(from + safeSize, all.size());
        return all.subList(from, to).stream().map(this::toItem).collect(Collectors.toList());
    }

    public List<ContentDtos.ContentListItem> myContents(String status) {
        LambdaQueryWrapper<Content> wrapper = new LambdaQueryWrapper<Content>()
                .eq(Content::getAuthorId, UserContext.getUserId())
                .orderByDesc(Content::getUpdatedAt);
        if (status != null && !status.isBlank()) {
            wrapper.eq(Content::getStatus, status);
        }
        return contentMapper.selectList(wrapper)
                .stream()
                .map(this::toItem)
                .collect(Collectors.toList());
    }

    public List<Phase2Dtos.AuditLogItem> auditLogs(Long contentId) {
        Content content = contentMapper.selectById(contentId);
        if (content == null) {
            throw new AppException("Content not found");
        }
        if (!content.getAuthorId().equals(UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new AppException("No permission to view audit logs");
        }
        return auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
                        .eq(AuditLog::getContentId, contentId)
                        .orderByDesc(AuditLog::getCreatedAt))
                .stream()
                .map(this::toAuditItem)
                .collect(Collectors.toList());
    }

    private ContentDtos.ContentListItem toItem(Content c) {
        ContentDtos.ContentListItem item = new ContentDtos.ContentListItem();
        item.setId(c.getId());
        item.setType(c.getType());
        item.setTitle(c.getTitle());
        item.setSummary(c.getSummary());
        item.setStatus(c.getStatus());
        item.setAuthorId(c.getAuthorId());
        item.setAuthorName(resolveAuthorName(c.getAuthorId()));
        item.setCategoryId(c.getCategoryId());
        item.setVisibility(c.getVisibility());
        item.setViewCount(c.getViewCount());
        item.setLikeCount(c.getLikeCount());
        item.setFavoriteCount(c.getFavoriteCount());
        item.setPublishedAt(c.getPublishedAt());
        item.setCreatedAt(c.getCreatedAt());
        return item;
    }

    private Phase2Dtos.AuditLogItem toAuditItem(AuditLog log) {
        Phase2Dtos.AuditLogItem item = new Phase2Dtos.AuditLogItem();
        item.setId(log.getId());
        item.setContentId(log.getContentId());
        item.setOperatorId(log.getOperatorId());
        item.setAction(log.getAction());
        item.setReason(log.getReason());
        item.setCreatedAt(log.getCreatedAt());
        return item;
    }

    private void validateCategoryAndTags(Long categoryId, List<Long> tagIds) {
        if (categoryMapper.selectById(categoryId) == null) {
            throw new AppException("Category not found");
        }
        if (tagIds != null) {
            for (Long tagId : tagIds) {
                if (tagMapper.selectById(tagId) == null) {
                    throw new AppException("Tag not found: " + tagId);
                }
            }
        }
    }

    private void saveTags(Long contentId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        for (Long tagId : tagIds) {
            ContentTag ct = new ContentTag();
            ct.setContentId(contentId);
            ct.setTagId(tagId);
            contentTagMapper.insert(ct);
        }
    }

    private String resolveAuthorName(Long authorId) {
        if (authorId == null) {
            return "匿名作者";
        }
        User user = userMapper.selectById(authorId);
        if (user == null) {
            return "用户 " + authorId;
        }
        String nickname = user.getNickname();
        if (nickname != null) {
            String trimmed = nickname.trim();
            if (!trimmed.isEmpty() && !trimmed.matches("[?？]+")) {
                return trimmed;
            }
        }
        String username = user.getUsername();
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        return "用户 " + authorId;
    }

    private boolean canViewContent(Content content, Long viewerUserId) {
        if (content == null) {
            return false;
        }
        if (UserContext.isAdmin()) {
            return true;
        }
        if (!"PUBLISHED".equals(content.getStatus())) {
            return viewerUserId != null && viewerUserId.equals(content.getAuthorId());
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
