package com.zhihu.platform.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("point_account")
public class PointAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long balance;
    private Integer levelNo;
    private LocalDateTime updatedAt;
}
