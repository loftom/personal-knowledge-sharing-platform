package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.knowledge.platform.common.AppException;
import com.knowledge.platform.domain.entity.PointAccount;
import com.knowledge.platform.domain.entity.PointLog;
import com.knowledge.platform.domain.entity.RedeemItem;
import com.knowledge.platform.domain.entity.RedeemRecord;
import com.knowledge.platform.domain.mapper.PointAccountMapper;
import com.knowledge.platform.domain.mapper.PointLogMapper;
import com.knowledge.platform.domain.mapper.RedeemItemMapper;
import com.knowledge.platform.domain.mapper.RedeemRecordMapper;
import com.knowledge.platform.security.UserContext;
import jakarta.annotation.PostConstruct;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class RedeemService {

    private final RedeemItemMapper redeemItemMapper;
    private final RedeemRecordMapper redeemRecordMapper;
    private final PointAccountMapper pointAccountMapper;
    private final PointLogMapper pointLogMapper;

    public RedeemService(RedeemItemMapper redeemItemMapper,
                         RedeemRecordMapper redeemRecordMapper,
                         PointAccountMapper pointAccountMapper,
                         PointLogMapper pointLogMapper) {
        this.redeemItemMapper = redeemItemMapper;
        this.redeemRecordMapper = redeemRecordMapper;
        this.pointAccountMapper = pointAccountMapper;
        this.pointLogMapper = pointLogMapper;
    }

    @PostConstruct
    public void ensureDefaultItems() {
        try {
            List<RedeemItem> defaults = Arrays.asList(
                item("内容推广", "让你的一篇内容在首页推荐位展示24小时", 50, "Promotion"),
                item("个性签名装饰", "个人主页签名获得特殊彩色样式", 30, "Signature"),
                item("金色头像框", "获得专属金色头像框装饰", 80, "GoldFrame"),
                item("内容置顶卡", "让你的一篇内容在个人主页置顶展示", 40, "Pin")
            );
            for (RedeemItem ri : defaults) {
                long count = redeemItemMapper.selectCount(new LambdaQueryWrapper<RedeemItem>()
                        .eq(RedeemItem::getName, ri.getName()));
                if (count == 0) {
                    ri.setEnabled(1);
                    ri.setCreatedAt(LocalDateTime.now());
                    redeemItemMapper.insert(ri);
                }
            }
        } catch (Exception ignored) {
            // 表不存在时跳过，通过 schema_additions.sql 或 schema.sql 手动创建后重启即可
        }
    }

    private RedeemItem item(String name, String description, int pointCost, String icon) {
        RedeemItem ri = new RedeemItem();
        ri.setName(name);
        ri.setDescription(description);
        ri.setPointCost(pointCost);
        ri.setIcon(icon);
        return ri;
    }

    // ---- User facing ----

    public List<RedeemItem> availableItems() {
        return redeemItemMapper.selectList(new LambdaQueryWrapper<RedeemItem>()
                .eq(RedeemItem::getEnabled, 1)
                .orderByAsc(RedeemItem::getPointCost));
    }

    @Transactional
    public void redeem(Long itemId) {
        Long userId = UserContext.getUserId();
        RedeemItem item = redeemItemMapper.selectById(itemId);
        if (item == null || item.getEnabled() == null || item.getEnabled() != 1) {
            throw new AppException("兑换项不存在或已下架");
        }

        // Idempotency guard: bizKey without timestamp so it properly prevents duplicate redemptions
        String bizKey = "redeem:" + itemId + ":" + userId;
        PointLog log = new PointLog();
        log.setUserId(userId);
        log.setChangeAmount(-item.getPointCost());
        log.setReason("REDEEM");
        log.setBizKey(bizKey);
        log.setCreatedAt(LocalDateTime.now());
        try {
            pointLogMapper.insert(log);
        } catch (DuplicateKeyException e) {
            throw new AppException("请勿重复兑换");
        }

        // Atomic balance deduction to prevent double-spend
        int updated = pointAccountMapper.update(null,
                new LambdaUpdateWrapper<PointAccount>()
                        .eq(PointAccount::getUserId, userId)
                        .ge(PointAccount::getBalance, item.getPointCost())
                        .setSql("balance = balance - " + item.getPointCost()));
        if (updated == 0) {
            throw new AppException("积分不足");
        }

        RedeemRecord record = new RedeemRecord();
        record.setUserId(userId);
        record.setItemId(item.getId());
        record.setItemName(item.getName());
        record.setPointCost(item.getPointCost());
        record.setStatus("COMPLETED");
        record.setCreatedAt(LocalDateTime.now());
        redeemRecordMapper.insert(record);
    }

    public List<RedeemRecord> myRedeemRecords() {
        Long userId = UserContext.getUserId();
        return redeemRecordMapper.selectList(new LambdaQueryWrapper<RedeemRecord>()
                .eq(RedeemRecord::getUserId, userId)
                .orderByDesc(RedeemRecord::getCreatedAt));
    }

    // ---- Admin management ----

    private void requireAdmin() {
        if (!UserContext.isAdmin()) {
            throw new AppException("Admin role required");
        }
    }

    public List<RedeemItem> allItems() {
        requireAdmin();
        return redeemItemMapper.selectList(new LambdaQueryWrapper<RedeemItem>()
                .orderByAsc(RedeemItem::getPointCost));
    }

    public RedeemItem addItem(String name, String description, Integer pointCost, String icon) {
        requireAdmin();
        if (name == null || name.isBlank()) throw new AppException("名称不能为空");
        if (pointCost == null || pointCost <= 0) throw new AppException("积分消耗必须大于0");
        RedeemItem item = new RedeemItem();
        item.setName(name.trim());
        item.setDescription(description);
        item.setPointCost(pointCost);
        item.setIcon(icon);
        item.setEnabled(1);
        item.setCreatedAt(LocalDateTime.now());
        redeemItemMapper.insert(item);
        return item;
    }

    public void updateItem(Long id, String name, String description, Integer pointCost, String icon, Integer enabled) {
        requireAdmin();
        RedeemItem item = redeemItemMapper.selectById(id);
        if (item == null) throw new AppException("兑换项不存在");
        if (name != null && !name.isBlank()) item.setName(name.trim());
        if (description != null) item.setDescription(description);
        if (pointCost != null && pointCost > 0) item.setPointCost(pointCost);
        if (icon != null) item.setIcon(icon);
        if (enabled != null) item.setEnabled(enabled);
        redeemItemMapper.updateById(item);
    }

    public void deleteItem(Long id) {
        requireAdmin();
        redeemItemMapper.deleteById(id);
    }
}
