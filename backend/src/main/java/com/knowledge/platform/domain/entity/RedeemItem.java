package com.knowledge.platform.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("redeem_item")
public class RedeemItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String description;
    private Integer pointCost;
    private String icon;
    private Integer enabled;
    private LocalDateTime createdAt;
}
