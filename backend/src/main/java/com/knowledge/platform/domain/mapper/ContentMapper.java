package com.knowledge.platform.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.platform.domain.entity.Content;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ContentMapper extends BaseMapper<Content> {

    List<Content> searchFulltext(@Param("keyword") String keyword,
                                  @Param("categoryId") Long categoryId,
                                  @Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate,
                                  @Param("sortBy") String sortBy);

    long searchFulltextCount(@Param("keyword") String keyword,
                              @Param("categoryId") Long categoryId,
                              @Param("startDate") LocalDateTime startDate,
                              @Param("endDate") LocalDateTime endDate);
}
