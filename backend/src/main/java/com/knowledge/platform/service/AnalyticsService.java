package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.platform.common.AppException;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.domain.entity.*;
import com.knowledge.platform.domain.mapper.*;
import com.knowledge.platform.security.UserContext;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final ContentMapper contentMapper;
    private final CommentMapper commentMapper;
    private final FollowRelationMapper followRelationMapper;
    private final ContentTagMapper contentTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final PointService pointService;

    public AnalyticsService(ContentMapper contentMapper,
                            CommentMapper commentMapper,
                            FollowRelationMapper followRelationMapper,
                            ContentTagMapper contentTagMapper,
                            CategoryMapper categoryMapper,
                            TagMapper tagMapper,
                            PointService pointService) {
        this.contentMapper = contentMapper;
        this.commentMapper = commentMapper;
        this.followRelationMapper = followRelationMapper;
        this.contentTagMapper = contentTagMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.pointService = pointService;
    }

    private void requireOwnOrAdmin(Long userId) {
        if (!userId.equals(UserContext.getUserId()) && !UserContext.isAdmin()) {
            throw new AppException("无权查看其他用户的数据");
        }
    }

    public Phase2Dtos.AnalyticsReportResponse personalReport(Long userId) {
        requireOwnOrAdmin(userId);
        List<Content> works = contentMapper.selectList(new LambdaQueryWrapper<Content>()
                .eq(Content::getAuthorId, userId)
                .eq(Content::getStatus, "PUBLISHED"));

        long totalViews = works.stream().mapToLong(content -> nullSafe(content.getViewCount())).sum();
        long totalLikes = works.stream().mapToLong(content -> nullSafe(content.getLikeCount())).sum();
        long totalFavorites = works.stream().mapToLong(content -> nullSafe(content.getFavoriteCount())).sum();
        long publishedContentCount = works.size();
        long totalCommentsReceived = 0L;
        if (!works.isEmpty()) {
            totalCommentsReceived = commentMapper.selectCount(new LambdaQueryWrapper<Comment>()
                    .in(Comment::getContentId, works.stream().map(Content::getId).toList())
                    .eq(Comment::getStatus, 1));
        }
        long followerCount = followRelationMapper.selectCount(new LambdaQueryWrapper<FollowRelation>()
                .eq(FollowRelation::getTargetUserId, userId)
                .eq(FollowRelation::getStatus, 1));

        double likeRate = totalViews == 0 ? 0D : round((double) totalLikes / totalViews);
        double favoriteRate = totalViews == 0 ? 0D : round((double) totalFavorites / totalViews);
        double influenceScore = round(totalViews * 0.35 + totalLikes * 0.25 + totalFavorites * 0.2 + followerCount * 0.2);

        Phase2Dtos.AnalyticsReportResponse response = new Phase2Dtos.AnalyticsReportResponse();
        response.setUserId(userId);
        response.setPublishedContentCount(publishedContentCount);
        response.setTotalViews(totalViews);
        response.setTotalLikes(totalLikes);
        response.setTotalFavorites(totalFavorites);
        response.setTotalCommentsReceived(totalCommentsReceived);
        response.setFollowerCount(followerCount);
        response.setLikeRate(likeRate);
        response.setFavoriteRate(favoriteRate);
        response.setInfluenceScore(influenceScore);
        response.setPoints(pointService.overview(userId));
        response.setTrends(buildTrends(works, userId));
        response.setRecentPointLogs(pointService.recentLogs(userId, 8));
        return response;
    }

    private List<Phase2Dtos.TrendPoint> buildTrends(List<Content> works, Long userId) {
        Map<LocalDate, Phase2Dtos.TrendPoint> bucket = new TreeMap<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Phase2Dtos.TrendPoint point = new Phase2Dtos.TrendPoint();
            point.setDateLabel(day.toString());
            point.setPublishedCount(0L);
            point.setViews(0L);
            point.setLikes(0L);
            point.setFavorites(0L);
            point.setPointDelta(0L);
            bucket.put(day, point);
        }

        for (Content content : works) {
            LocalDate day = content.getCreatedAt() == null ? null : content.getCreatedAt().toLocalDate();
            if (day == null || !bucket.containsKey(day)) {
                continue;
            }
            Phase2Dtos.TrendPoint point = bucket.get(day);
            point.setPublishedCount(point.getPublishedCount() + 1);
            point.setViews(point.getViews() + nullSafe(content.getViewCount()));
            point.setLikes(point.getLikes() + nullSafe(content.getLikeCount()));
            point.setFavorites(point.getFavorites() + nullSafe(content.getFavoriteCount()));
        }

        for (Phase2Dtos.PointLogItem log : pointService.recentLogs(userId, 50)) {
            LocalDate day = log.getCreatedAt() == null ? null : log.getCreatedAt().toLocalDate();
            if (day == null || !bucket.containsKey(day)) {
                continue;
            }
            Phase2Dtos.TrendPoint point = bucket.get(day);
            point.setPointDelta(point.getPointDelta() + log.getChangeAmount());
        }

        return bucket.values().stream().toList();
    }

    public Phase2Dtos.InfluenceReportResponse influenceReport(Long userId) {
        requireOwnOrAdmin(userId);
        List<Content> works = contentMapper.selectList(new LambdaQueryWrapper<Content>()
                .eq(Content::getAuthorId, userId)
                .eq(Content::getStatus, "PUBLISHED"));
        long publishedContentCount = works.size();
        long totalViews = 0L;
        long totalLikes = 0L;
        long totalFavorites = 0L;
        double totalInfluence = 0D;

        List<Long> contentIds = works.stream().map(Content::getId).toList();
        Map<Long, Long> commentCountMap = new HashMap<>();
        if (!contentIds.isEmpty()) {
            List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                    .in(Comment::getContentId, contentIds)
                    .eq(Comment::getStatus, 1));
            commentCountMap = comments.stream()
                    .collect(Collectors.groupingBy(Comment::getContentId, Collectors.counting()));
        }

        // Resolve category and tag names
        Map<Long, Category> categoryMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
        Map<Long, Long> categoryWorkCount = new HashMap<>();
        List<ContentTag> allContentTags = new ArrayList<>();
        if (!contentIds.isEmpty()) {
            allContentTags = contentTagMapper.selectList(new LambdaQueryWrapper<ContentTag>()
                    .in(ContentTag::getContentId, contentIds));
        }
        Map<Long, Long> tagUsageCount = allContentTags.stream()
                .collect(Collectors.groupingBy(ContentTag::getTagId, Collectors.counting()));

        Map<Long, String> tagNameMap = new HashMap<>();
        if (!tagUsageCount.isEmpty()) {
            tagNameMap = tagMapper.selectBatchIds(tagUsageCount.keySet()).stream()
                    .collect(Collectors.toMap(Tag::getId, Tag::getName));
        }

        // Build per-content influence items
        List<Phase2Dtos.ContentInfluenceItem> items = new ArrayList<>();
        for (Content c : works) {
            Phase2Dtos.ContentInfluenceItem item = new Phase2Dtos.ContentInfluenceItem();
            item.setContentId(c.getId());
            item.setTitle(c.getTitle());
            item.setType(c.getType());
            item.setViewCount(nullSafe(c.getViewCount()));
            item.setLikeCount(nullSafe(c.getLikeCount()));
            item.setFavoriteCount(nullSafe(c.getFavoriteCount()));
            item.setCommentCount(commentCountMap.getOrDefault(c.getId(), 0L));
            double score = round(nullSafe(c.getViewCount()) * 0.35
                    + nullSafe(c.getLikeCount()) * 0.30
                    + nullSafe(c.getFavoriteCount()) * 0.20
                    + item.getCommentCount() * 0.15);
            item.setInfluenceScore(score);
            item.setPublishedAt(c.getPublishedAt());

            totalViews += nullSafe(c.getViewCount());
            totalLikes += nullSafe(c.getLikeCount());
            totalFavorites += nullSafe(c.getFavoriteCount());
            totalInfluence += score;

            Category cat = categoryMap.get(c.getCategoryId());
            if (cat != null) {
                categoryWorkCount.merge(c.getCategoryId(), 1L, Long::sum);
            }

            items.add(item);
        }

        items.sort((a, b) -> Double.compare(b.getInfluenceScore(), a.getInfluenceScore()));

        Phase2Dtos.InfluenceReportResponse response = new Phase2Dtos.InfluenceReportResponse();
        response.setUserId(userId);
        response.setPublishedContentCount(publishedContentCount);
        response.setItems(items);
        response.setBestPerformer(items.isEmpty() ? null : items.get(0));
        response.setTotalViews(totalViews);
        response.setTotalLikes(totalLikes);
        response.setTotalFavorites(totalFavorites);
        response.setAvgInfluenceScore(items.isEmpty() ? 0D
                : round(totalInfluence / items.size()));

        // Best category by work count
        String bestCategory = null;
        long bestCatCount = 0;
        for (Map.Entry<Long, Long> entry : categoryWorkCount.entrySet()) {
            if (entry.getValue() > bestCatCount) {
                bestCatCount = entry.getValue();
                Category cat = categoryMap.get(entry.getKey());
                if (cat != null) bestCategory = cat.getName();
            }
        }
        response.setBestCategory(bestCategory);

        // Best tag by usage across all content
        String bestTag = null;
        long bestTagCount = 0;
        for (Map.Entry<Long, Long> entry : tagUsageCount.entrySet()) {
            if (entry.getValue() > bestTagCount) {
                bestTagCount = entry.getValue();
                bestTag = tagNameMap.getOrDefault(entry.getKey(), null);
            }
        }
        response.setBestTag(bestTag);

        return response;
    }

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
