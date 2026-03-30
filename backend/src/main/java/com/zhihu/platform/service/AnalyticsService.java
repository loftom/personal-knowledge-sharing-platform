package com.zhihu.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhihu.platform.domain.dto.Phase2Dtos;
import com.zhihu.platform.domain.entity.Comment;
import com.zhihu.platform.domain.entity.Content;
import com.zhihu.platform.domain.entity.FollowRelation;
import com.zhihu.platform.domain.mapper.CommentMapper;
import com.zhihu.platform.domain.mapper.ContentMapper;
import com.zhihu.platform.domain.mapper.FollowRelationMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class AnalyticsService {

    private final ContentMapper contentMapper;
    private final CommentMapper commentMapper;
    private final FollowRelationMapper followRelationMapper;
    private final PointService pointService;

    public AnalyticsService(ContentMapper contentMapper,
                            CommentMapper commentMapper,
                            FollowRelationMapper followRelationMapper,
                            PointService pointService) {
        this.contentMapper = contentMapper;
        this.commentMapper = commentMapper;
        this.followRelationMapper = followRelationMapper;
        this.pointService = pointService;
    }

    public Phase2Dtos.AnalyticsReportResponse personalReport(Long userId) {
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

    private long nullSafe(Long value) {
        return value == null ? 0L : value;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
