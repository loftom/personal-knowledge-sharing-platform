package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.domain.entity.Content;
import com.knowledge.platform.domain.entity.ContentTag;
import com.knowledge.platform.domain.entity.FollowRelation;
import com.knowledge.platform.domain.entity.User;
import com.knowledge.platform.domain.mapper.ContentMapper;
import com.knowledge.platform.domain.mapper.ContentTagMapper;
import com.knowledge.platform.domain.mapper.FollowRelationMapper;
import com.knowledge.platform.domain.mapper.UserMapper;
import com.knowledge.platform.security.UserContext;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendService {

    private final ContentMapper contentMapper;
    private final ContentTagMapper contentTagMapper;
    private final FollowRelationMapper followRelationMapper;
    private final UserMapper userMapper;
    private final BehaviorService behaviorService;

    public RecommendService(ContentMapper contentMapper,
                            ContentTagMapper contentTagMapper,
                            FollowRelationMapper followRelationMapper,
                            UserMapper userMapper,
                            BehaviorService behaviorService) {
        this.contentMapper = contentMapper;
        this.contentTagMapper = contentTagMapper;
        this.followRelationMapper = followRelationMapper;
        this.userMapper = userMapper;
        this.behaviorService = behaviorService;
    }

    public List<Phase2Dtos.RecommendItem> recommend(Integer page, Integer size) {
        Long userId = UserContext.getUserId();
        Map<Long, Double> tagPreference = behaviorService.buildUserTagPreference(userId);
        List<Content> candidates = contentMapper.selectList(new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, "PUBLISHED")
                .orderByDesc(Content::getPublishedAt)
                .last("LIMIT 300"))
                .stream()
                .filter(content -> canRecommend(content, userId))
                .toList();

        List<Phase2Dtos.RecommendItem> ranked = new ArrayList<>();
        for (Content c : candidates) {
            double tagMatch = tagMatch(c, tagPreference);
            double hotScore = hotScore(c);
            double freshness = freshnessScore(c);
            double score = score(tagMatch, hotScore, freshness);
            Phase2Dtos.RecommendItem item = new Phase2Dtos.RecommendItem();
            item.setContentId(c.getId());
            item.setTitle(c.getTitle());
            item.setSummary(c.getSummary());
            item.setType(c.getType());
            item.setAuthorId(c.getAuthorId());
            item.setAuthorName(resolveAuthorName(c.getAuthorId()));
            item.setScore(round(score));
            item.setReasonText(buildReasonText(tagMatch, hotScore, freshness));
            item.setReasonTags(buildReasonTags(c, tagMatch, hotScore, freshness));
            item.setViewCount(c.getViewCount());
            item.setLikeCount(c.getLikeCount());
            item.setFavoriteCount(c.getFavoriteCount());
            item.setPublishedAt(c.getPublishedAt());
            ranked.add(item);
        }

        ranked.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safeSize = Math.max(size == null ? 10 : size, 1);
        int from = (safePage - 1) * safeSize;
        if (from >= ranked.size()) {
            return Collections.emptyList();
        }
        int to = Math.min(from + safeSize, ranked.size());
        return ranked.subList(from, to);
    }

    private double score(double tagMatch, double hotScore, double freshness) {
        return tagMatch * 1.8 + hotScore * 0.6 + freshness * 0.8;
    }

    private double hotScore(Content content) {
        return nullSafe(content.getLikeCount()) * 1.2 + nullSafe(content.getFavoriteCount()) * 1.5 + nullSafe(content.getViewCount()) * 0.02;
    }

    private double freshnessScore(Content content) {
        double freshness = 0.0;
        if (content.getPublishedAt() != null) {
            long hours = Math.max(1, Duration.between(content.getPublishedAt(), LocalDateTime.now()).toHours());
            freshness = 72.0 / hours;
        }
        return freshness;
    }

    private double tagMatch(Content content, Map<Long, Double> tagPreference) {
        List<Long> tags = contentTagMapper.selectList(new LambdaQueryWrapper<ContentTag>()
                        .eq(ContentTag::getContentId, content.getId()))
                .stream().map(ContentTag::getTagId).collect(Collectors.toList());
        double tagMatch = 0.0;
        for (Long tagId : tags) {
            tagMatch += tagPreference.getOrDefault(tagId, 0.0);
        }
        return tagMatch;
    }

    private String buildReasonText(double tagMatch, double hotScore, double freshness) {
        List<String> reasons = new ArrayList<>();
        if (tagMatch > 0.5) {
            reasons.add("兴趣标签匹配");
        }
        if (freshness > 0.5) {
            reasons.add("近期发布");
        }
        if (hotScore > 20) {
            reasons.add("互动热度较高");
        }
        if (reasons.isEmpty()) {
            reasons.add("综合热度排序");
        }
        return String.join("、", reasons);
    }

    private List<String> buildReasonTags(Content content, double tagMatch, double hotScore, double freshness) {
        List<String> tags = new ArrayList<>();
        if (tagMatch > 0.5) {
            tags.add("兴趣匹配");
        }
        if (freshness > 0.5) {
            tags.add("新近发布");
        }
        if (hotScore > 20) {
            tags.add("高热度");
        }
        if (content.getType() != null && !content.getType().isBlank()) {
            tags.add(content.getType().equalsIgnoreCase("QUESTION") ? "问答内容" : content.getType().equalsIgnoreCase("TUTORIAL") ? "教程内容" : "文章内容");
        }
        return tags.stream().distinct().limit(4).toList();
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
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
            if (!trimmed.isEmpty() && !trimmed.matches("[?锟�]+")) {
                return trimmed;
            }
        }
        String username = user.getUsername();
        if (username != null && !username.trim().isEmpty()) {
            return username.trim();
        }
        return "用户 " + authorId;
    }

    private boolean canRecommend(Content content, Long viewerUserId) {
        if (content == null) {
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
