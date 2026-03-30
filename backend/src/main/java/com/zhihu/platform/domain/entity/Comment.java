package com.zhihu.platform.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contentId;
    private Long userId;
    private Long parentId;
    private String body;
    private Long likeCount;
    private Integer status;
    private LocalDateTime createdAt;
}
