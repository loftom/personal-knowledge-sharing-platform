package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.domain.entity.BadgeUser;
import com.knowledge.platform.domain.entity.PointAccount;
import com.knowledge.platform.domain.entity.PointLog;
import com.knowledge.platform.domain.mapper.BadgeUserMapper;
import com.knowledge.platform.domain.mapper.PointAccountMapper;
import com.knowledge.platform.domain.mapper.PointLogMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PointService {

    private final PointAccountMapper pointAccountMapper;
    private final PointLogMapper pointLogMapper;
    private final BadgeUserMapper badgeUserMapper;

    public PointService(PointAccountMapper pointAccountMapper,
                        PointLogMapper pointLogMapper,
                        BadgeUserMapper badgeUserMapper) {
        this.pointAccountMapper = pointAccountMapper;
        this.pointLogMapper = pointLogMapper;
        this.badgeUserMapper = badgeUserMapper;
    }

    @Transactional
    public void rewardPublishApproved(Long userId, Long contentId) {
        changePoints(userId, 10, "PUBLISH_APPROVED", "publish:" + contentId);
    }

    @Transactional
    public void rewardContentLiked(Long userId, Long contentId, Long likerUserId) {
        if (userId == null || userId.equals(likerUserId)) {
            return;
        }
        changePoints(userId, 2, "CONTENT_LIKED", "content-like:" + contentId + ":" + likerUserId);
    }

    @Transactional
    public void rewardBestAnswer(Long userId, Long answerId) {
        changePoints(userId, 30, "BEST_ANSWER", "best-answer:" + answerId);
    }

    @Transactional
    public void penalizeOffline(Long userId, Long contentId) {
        changePoints(userId, -20, "CONTENT_OFFLINE", "offline:" + contentId);
    }

    public Phase2Dtos.PointOverview overview(Long userId) {
        PointAccount account = ensureAccount(userId);
        Phase2Dtos.PointOverview overview = new Phase2Dtos.PointOverview();
        overview.setBalance(account.getBalance());
        overview.setLevelNo(account.getLevelNo());
        overview.setBadges(badgeUserMapper.selectList(new LambdaQueryWrapper<BadgeUser>()
                        .eq(BadgeUser::getUserId, userId)
                        .orderByAsc(BadgeUser::getAwardedAt))
                .stream()
                .map(BadgeUser::getBadgeName)
                .toList());
        return overview;
    }

    public List<Phase2Dtos.PointLogItem> recentLogs(Long userId, int limit) {
        int safeLimit = Math.max(1, limit);
        return pointLogMapper.selectList(new LambdaQueryWrapper<PointLog>()
                        .eq(PointLog::getUserId, userId)
                        .orderByDesc(PointLog::getCreatedAt)
                        .last("LIMIT " + safeLimit))
                .stream()
                .map(log -> {
                    Phase2Dtos.PointLogItem item = new Phase2Dtos.PointLogItem();
                    item.setId(log.getId());
                    item.setChangeAmount(log.getChangeAmount());
                    item.setReason(log.getReason());
                    item.setCreatedAt(log.getCreatedAt());
                    return item;
                })
                .toList();
    }

    private void changePoints(Long userId, int amount, String reason, String bizKey) {
        if (userId == null) {
            return;
        }

        PointLog log = new PointLog();
        log.setUserId(userId);
        log.setChangeAmount(amount);
        log.setReason(reason);
        log.setBizKey(bizKey);
        log.setCreatedAt(LocalDateTime.now());

        try {
            pointLogMapper.insert(log);
        } catch (DuplicateKeyException ignored) {
            return;
        }

        PointAccount account = ensureAccount(userId);
        long nextBalance = Math.max(0L, (account.getBalance() == null ? 0L : account.getBalance()) + amount);
        account.setBalance(nextBalance);
        account.setLevelNo(resolveLevel(nextBalance));
        account.setUpdatedAt(LocalDateTime.now());
        pointAccountMapper.updateById(account);
        refreshBadges(userId, nextBalance);
    }

    private PointAccount ensureAccount(Long userId) {
        PointAccount account = pointAccountMapper.selectOne(new LambdaQueryWrapper<PointAccount>()
                .eq(PointAccount::getUserId, userId));
        if (account != null) {
            return account;
        }

        PointAccount create = new PointAccount();
        create.setUserId(userId);
        create.setBalance(0L);
        create.setLevelNo(1);
        create.setUpdatedAt(LocalDateTime.now());
        try {
            pointAccountMapper.insert(create);
            return create;
        } catch (DuplicateKeyException ignored) {
            return pointAccountMapper.selectOne(new LambdaQueryWrapper<PointAccount>()
                    .eq(PointAccount::getUserId, userId));
        }
    }

    private int resolveLevel(long balance) {
        if (balance >= 200) {
            return 4;
        }
        if (balance >= 100) {
            return 3;
        }
        if (balance >= 30) {
            return 2;
        }
        return 1;
    }

    private void refreshBadges(Long userId, long balance) {
        if (balance >= 30) {
            awardBadge(userId, "ACTIVE_CREATOR", "Active Creator");
        }
        if (balance >= 100) {
            awardBadge(userId, "QUALITY_AUTHOR", "Quality Author");
        }
        if (balance >= 200) {
            awardBadge(userId, "COMMUNITY_STAR", "Community Star");
        }
    }

    private void awardBadge(Long userId, String badgeCode, String badgeName) {
        BadgeUser exists = badgeUserMapper.selectOne(new LambdaQueryWrapper<BadgeUser>()
                .eq(BadgeUser::getUserId, userId)
                .eq(BadgeUser::getBadgeCode, badgeCode));
        if (exists != null) {
            return;
        }
        BadgeUser badge = new BadgeUser();
        badge.setUserId(userId);
        badge.setBadgeCode(badgeCode);
        badge.setBadgeName(badgeName);
        badge.setAwardedAt(LocalDateTime.now());
        badgeUserMapper.insert(badge);
    }
}
