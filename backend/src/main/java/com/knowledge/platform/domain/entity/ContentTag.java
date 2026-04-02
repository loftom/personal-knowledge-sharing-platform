package com.knowledge.platform.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("content_tag")
public class ContentTag {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long contentId;
    private Long tagId;
}
