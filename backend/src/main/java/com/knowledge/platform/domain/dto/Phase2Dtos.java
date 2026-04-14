package com.knowledge.platform.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class Phase2Dtos {

    @Data
    public static class QuestionListItem {
        private Long id;
        private String title;
        private String summary;
        private Long authorId;
        private String authorName;
        private Long answerCount;
        private String status;
        private Long bestAnswerId;
        private Long viewCount;
        private LocalDateTime createdAt;
    }

    @Data
    public static class CreateAnswerRequest {
        @NotBlank
        private String body;
    }

    @Data
    public static class PickBestAnswerRequest {
        @NotNull
        private Long answerId;
    }

    @Data
    public static class QuestionStateResponse {
        private Long questionId;
        private String status;
        private Long bestAnswerId;
        private LocalDateTime resolvedAt;
    }

    @Data
    public static class AnswerItem {
        private Long id;
        private Long questionId;
        private Long userId;
        private String username;
        private String nickname;
        private String displayName;
        private String body;
        private Long likeCount;
        private Integer isBest;
        private LocalDateTime createdAt;
    }

    @Data
    public static class AuditOverview {
        private Long pendingCount;
        private Long publishedCount;
        private Long offlineCount;
        private Long articlePendingCount;
        private Long questionPendingCount;
    }

    @Data
    public static class AdminDashboardResponse {
        private Long totalUserCount;
        private Long activeUserCount;
        private Long deletedUserCount;
        private Long totalContentCount;
        private Long publishedContentCount;
        private Long articleCount;
        private Long questionCount;
        private Long totalViewCount;
        private Long totalLikeCount;
        private Long totalFavoriteCount;
        private Long totalCommentCount;
        private Long totalFollowerCount;
    }

    @Data
    public static class RecommendItem {
        private Long contentId;
        private String title;
        private String summary;
        private String type;
        private Long authorId;
        private String authorName;
        private Double score;
        private Long viewCount;
        private Long likeCount;
        private Long favoriteCount;
        private LocalDateTime publishedAt;
    }

    @Data
    public static class UserSimple {
        private Long id;
        private String username;
        private String nickname;
    }

    @Data
    public static class CategoryOption {
        private Long id;
        private Long parentId;
        private String name;
    }

    @Data
    public static class TagOption {
        private Long id;
        private String name;
    }

    @Data
    public static class UpdateNicknameRequest {
        @NotBlank
        private String nickname;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank
        private String newPassword;
    }

    @Data
    public static class CreateCategoryRequest {
        @NotBlank
        private String name;
        private Long parentId = 0L;
        private Integer sort = 0;
        private Integer enabled = 1;
    }

    @Data
    public static class CreateTagRequest {
        @NotBlank
        private String name;
    }

    @Data
    public static class PersonalStats {
        private Long contentCount;
        private Long favoriteCount;
        private Long totalLikesReceived;
        private Long totalFavoritesReceived;
        private Long totalCommentsReceived;
        private Long followerCount;
        private Long followingCount;
    }

    @Data
    public static class PersonalSpaceResponse {
        private UserSimple user;
        private PersonalStats stats;
        private List<ContentDtos.ContentListItem> works;
        private List<ContentDtos.ContentListItem> favorites;
    }

    @Data
    public static class NotificationItem {
        private Long id;
        private String type;
        private String title;
        private String content;
        private Long relatedId;
        private Integer isRead;
        private LocalDateTime createdAt;
    }

    @Data
    public static class SendPrivateMessageRequest {
        @NotNull
        private Long receiverUserId;
        @NotBlank
        private String content;
    }

    @Data
    public static class PrivateConversationItem {
        private Long userId;
        private String username;
        private String nickname;
        private String displayName;
        private String lastMessage;
        private LocalDateTime lastMessageAt;
        private Long unreadCount;
    }

    @Data
    public static class PrivateMessageItem {
        private Long id;
        private Long senderUserId;
        private String senderName;
        private Long receiverUserId;
        private String receiverName;
        private String content;
        private Integer isRead;
        private LocalDateTime createdAt;
        private Boolean mine;
    }

    @Data
    public static class PrivateMessageUnreadResponse {
        private Long unreadCount;
    }

    @Data
    public static class AuditLogItem {
        private Long id;
        private Long contentId;
        private Long operatorId;
        private String action;
        private String reason;
        private LocalDateTime createdAt;
    }

    @Data
    public static class PointOverview {
        private Long balance;
        private Integer levelNo;
        private List<String> badges;
    }

    @Data
    public static class PointLogItem {
        private Long id;
        private Integer changeAmount;
        private String reason;
        private LocalDateTime createdAt;
    }

    @Data
    public static class TrendPoint {
        private String dateLabel;
        private Long publishedCount;
        private Long views;
        private Long likes;
        private Long favorites;
        private Long pointDelta;
    }

    @Data
    public static class AnalyticsReportResponse {
        private Long userId;
        private Long publishedContentCount;
        private Long totalViews;
        private Long totalLikes;
        private Long totalFavorites;
        private Long totalCommentsReceived;
        private Long followerCount;
        private Double likeRate;
        private Double favoriteRate;
        private Double influenceScore;
        private PointOverview points;
        private List<TrendPoint> trends;
        private List<PointLogItem> recentPointLogs;
    }

    @Data
    public static class UserPreferenceTag {
        private Long tagId;
        private String tagName;
        private Double weight;
    }

    @Data
    public static class UserPreferenceView {
        private Long userId;
        private String username;
        private String nickname;
        private String displayName;
        private String role;
        private Double totalWeight;
        private Integer tagCount;
        private List<UserPreferenceTag> preferences;
    }
}
