package com.knowledge.platform.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("favorite_record")
public class FavoriteRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long contentId;
    private LocalDateTime createdAt;
}
