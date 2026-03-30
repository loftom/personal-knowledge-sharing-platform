package com.zhihu.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhihu.platform.domain.dto.Phase2Dtos;
import com.zhihu.platform.domain.entity.Content;
import com.zhihu.platform.domain.entity.ContentTag;
import com.zhihu.platform.domain.entity.User;
import com.zhihu.platform.domain.mapper.ContentMapper;
import com.zhihu.platform.domain.mapper.ContentTagMapper;
import com.zhihu.platform.domain.mapper.UserMapper;
import com.zhihu.platform.security.UserContext;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendService {

    private final ContentMapper contentMapper;
    private final ContentTagMapper contentTagMapper;
    private final UserMapper userMapper;
    private final BehaviorService behaviorService;

    public RecommendService(ContentMapper contentMapper,
                            ContentTagMapper contentTagMapper,
                            UserMapper userMapper,
                            BehaviorService behaviorService) {
        this.contentMapper = contentMapper;
        this.contentTagMapper = contentTagMapper;
        this.userMapper = userMapper;
        this.behaviorService = behaviorService;
    }

    public List<Phase2Dtos.RecommendItem> recommend(Integer page, Integer size) {
        Long userId = UserContext.getUserId();
        Map<Long, Double> tagPreference = behaviorService.buildUserTagPreference(userId);
        List<Content> candidates = contentMapper.selectList(new LambdaQueryWrapper<Content>()
                .eq(Content::getStatus, "PUBLISHED")
                .orderByDesc(Content::getPublishedAt)
                .last("LIMIT 300"));

        List<Phase2Dtos.RecommendItem> ranked = new ArrayList<>();
        for (Content c : candidates) {
            double score = score(c, tagPreference);
            Phase2Dtos.RecommendItem item = new Phase2Dtos.RecommendItem();
            item.setContentId(c.getId());
            item.setTitle(c.getTitle());
            item.setSummary(c.getSummary());
            item.setType(c.getType());
            item.setAuthorId(c.getAuthorId());
            item.setAuthorName(resolveAuthorName(c.getAuthorId()));
            item.setScore(score);
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

    private double score(Content content, Map<Long, Double> tagPreference) {
        double hotScore = content.getLikeCount() * 1.2 + content.getFavoriteCount() * 1.5 + content.getViewCount() * 0.02;
        double freshness = 0.0;
        if (content.getPublishedAt() != null) {
            long hours = Math.max(1, Duration.between(content.getPublishedAt(), LocalDateTime.now()).toHours());
            freshness = 72.0 / hours;
        }

        List<Long> tags = contentTagMapper.selectList(new LambdaQueryWrapper<ContentTag>()
                        .eq(ContentTag::getContentId, content.getId()))
                .stream().map(ContentTag::getTagId).collect(Collectors.toList());
        double tagMatch = 0.0;
        for (Long tagId : tags) {
            tagMatch += tagPreference.getOrDefault(tagId, 0.0);
        }

        return tagMatch * 1.8 + hotScore * 0.6 + freshness * 0.8;
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
}
