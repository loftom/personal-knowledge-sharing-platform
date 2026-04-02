package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.platform.common.AppException;
import com.knowledge.platform.domain.entity.FollowRelation;
import com.knowledge.platform.domain.entity.User;
import com.knowledge.platform.domain.mapper.FollowRelationMapper;
import com.knowledge.platform.domain.mapper.UserMapper;
import com.knowledge.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class FollowService {

    private final FollowRelationMapper followRelationMapper;
    private final UserMapper userMapper;
    private final BehaviorService behaviorService;

    public FollowService(FollowRelationMapper followRelationMapper,
                         UserMapper userMapper,
                         BehaviorService behaviorService) {
        this.followRelationMapper = followRelationMapper;
        this.userMapper = userMapper;
        this.behaviorService = behaviorService;
    }

    @Transactional
    public boolean toggleFollow(Long targetUserId) {
        Long userId = UserContext.getUserId();
        if (userId.equals(targetUserId)) {
            throw new AppException("不能关注自己");
        }
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new AppException("用户不存在");
        }
        FollowRelation relation = followRelationMapper.selectOne(new LambdaQueryWrapper<FollowRelation>()
                .eq(FollowRelation::getFollowerUserId, userId)
                .eq(FollowRelation::getTargetUserId, targetUserId));
        if (relation == null) {
            FollowRelation create = new FollowRelation();
            create.setFollowerUserId(userId);
            create.setTargetUserId(targetUserId);
            create.setStatus(1);
            create.setCreatedAt(LocalDateTime.now());
            create.setUpdatedAt(LocalDateTime.now());
            followRelationMapper.insert(create);
            behaviorService.record(userId, "FOLLOW", "USER", targetUserId, 2);
            return true;
        }
        relation.setStatus(relation.getStatus() != null && relation.getStatus() == 1 ? 0 : 1);
        relation.setUpdatedAt(LocalDateTime.now());
        followRelationMapper.updateById(relation);
        if (relation.getStatus() == 1) {
            behaviorService.record(userId, "FOLLOW", "USER", targetUserId, 2);
        }
        return relation.getStatus() == 1;
    }
}
