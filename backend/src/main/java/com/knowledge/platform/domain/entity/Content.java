package com.knowledge.platform.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("content")
public class Content {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long authorId;
    private String type;
    private String title;
    private String summary;
    private String body;
    private Long categoryId;
    private String visibility;
    private String status;
    private Long viewCount;
    private Long likeCount;
    private Long favoriteCount;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String authorName;

    @TableField(exist = false)
    private Boolean liked;

    @TableField(exist = false)
    private Boolean favorited;
}
