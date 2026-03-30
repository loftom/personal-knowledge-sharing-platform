package com.zhihu.platform.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhihu.platform.domain.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
