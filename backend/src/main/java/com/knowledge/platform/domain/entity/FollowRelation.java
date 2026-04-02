package com.knowledge.platform.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("follow_relation")
public class FollowRelation {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long followerUserId;
    private Long targetUserId;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
