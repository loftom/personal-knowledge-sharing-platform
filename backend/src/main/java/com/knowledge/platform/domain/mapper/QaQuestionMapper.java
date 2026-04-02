package com.knowledge.platform.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.platform.domain.entity.QaQuestion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QaQuestionMapper extends BaseMapper<QaQuestion> {
}
