package com.zhihu.platform.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhihu.platform.domain.entity.Notification;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NotificationMapper extends BaseMapper<Notification> {
}
