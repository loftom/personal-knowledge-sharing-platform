package com.knowledge.platform.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.platform.domain.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
