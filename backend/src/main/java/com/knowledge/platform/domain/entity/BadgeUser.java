package com.knowledge.platform.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("badge_user")
public class BadgeUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String badgeCode;
    private String badgeName;
    private LocalDateTime awardedAt;
}
