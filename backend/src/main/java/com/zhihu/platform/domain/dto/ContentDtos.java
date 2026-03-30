package com.zhihu.platform.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class ContentDtos {

    @Data
    public static class CreateContentRequest {
        @NotBlank
        private String type;
        @NotBlank
        private String title;
        private String summary;
        @NotBlank
        private String body;
        // optional: DRAFT / PENDING_REVIEW / PUBLISHED
        private String status;
        @NotNull
        private Long categoryId;
        private String visibility = "PUBLIC";
        private List<Long> tagIds;
    }

    @Data
    public static class UpdateContentRequest {
        @NotBlank
        private String title;
        private String summary;
        @NotBlank
        private String body;
        @NotNull
        private Long categoryId;
        private String visibility = "PUBLIC";
        private List<Long> tagIds;
        // optional explicit status change
        private String status;
    }

    @Data
    public static class ContentListItem {
        private Long id;
        private String type;
        private String title;
        private String summary;
        private String status;
        private Long authorId;
        private String authorName;
        private Long categoryId;
        private String visibility;
        private Long viewCount;
        private Long likeCount;
        private Long favoriteCount;
        private LocalDateTime publishedAt;
        private LocalDateTime createdAt;
    }

    @Data
    public static class CommentRequest {
        @NotBlank
        private String body;
        private Long parentId;
    }

    @Data
    public static class CommentView {
        private Long id;
        private Long contentId;
        private Long userId;
        private String username;
        private String nickname;
        private String displayName;
        private Long parentId;
        private String body;
        private Long likeCount;
        private Integer status;
        private LocalDateTime createdAt;
    }

    @Data
    public static class ToggleRequest {
        @NotNull
        private Long targetId;
        @NotBlank
        private String targetType;
    }
}
