package com.zhihu.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhihu.platform.domain.entity.ContentTag;
import com.zhihu.platform.domain.entity.UserBehaviorEvent;
import com.zhihu.platform.domain.mapper.ContentTagMapper;
import com.zhihu.platform.domain.mapper.UserBehaviorEventMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BehaviorService {

    private final UserBehaviorEventMapper behaviorEventMapper;
    private final ContentTagMapper contentTagMapper;

    public BehaviorService(UserBehaviorEventMapper behaviorEventMapper, ContentTagMapper contentTagMapper) {
        this.behaviorEventMapper = behaviorEventMapper;
        this.contentTagMapper = contentTagMapper;
    }

    public void record(Long userId, String eventType, String targetType, Long targetId, Integer weight) {
        if (userId == null || targetId == null) {
            return;
        }
        UserBehaviorEvent event = new UserBehaviorEvent();
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setTargetType(targetType);
        event.setTargetId(targetId);
        event.setEventWeight(weight == null ? 1 : weight);
        event.setCreatedAt(LocalDateTime.now());
        behaviorEventMapper.insert(event);
    }

    public Map<Long, Double> buildUserTagPreference(Long userId) {
        Map<Long, Double> tagWeights = new HashMap<>();
        if (userId == null) {
            return tagWeights;
        }
        List<UserBehaviorEvent> events = behaviorEventMapper.selectList(new LambdaQueryWrapper<UserBehaviorEvent>()
                .eq(UserBehaviorEvent::getUserId, userId)
                .orderByDesc(UserBehaviorEvent::getCreatedAt)
                .last("LIMIT 500"));

        for (UserBehaviorEvent event : events) {
            if (!"CONTENT".equalsIgnoreCase(event.getTargetType())) {
                continue;
            }
            double base = event.getEventWeight() == null ? 1 : event.getEventWeight();
            List<ContentTag> tags = contentTagMapper.selectList(new LambdaQueryWrapper<ContentTag>()
                    .eq(ContentTag::getContentId, event.getTargetId()));
            for (ContentTag tag : tags) {
                tagWeights.put(tag.getTagId(), tagWeights.getOrDefault(tag.getTagId(), 0D) + base);
            }
        }
        return tagWeights;
    }
}
