package com.knowledge.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.knowledge.platform.common.AppException;
import com.knowledge.platform.domain.dto.Phase2Dtos;
import com.knowledge.platform.domain.entity.Notification;
import com.knowledge.platform.domain.entity.PrivateMessage;
import com.knowledge.platform.domain.entity.User;
import com.knowledge.platform.domain.mapper.NotificationMapper;
import com.knowledge.platform.domain.mapper.PrivateMessageMapper;
import com.knowledge.platform.domain.mapper.UserMapper;
import com.knowledge.platform.realtime.RealtimePushService;
import com.knowledge.platform.security.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class PrivateMessageService {

    private final PrivateMessageMapper privateMessageMapper;
    private final UserMapper userMapper;
    private final NotificationMapper notificationMapper;
    private final RealtimePushService realtimePushService;

    public PrivateMessageService(PrivateMessageMapper privateMessageMapper,
                                 UserMapper userMapper,
                                 NotificationMapper notificationMapper,
                                 RealtimePushService realtimePushService) {
        this.privateMessageMapper = privateMessageMapper;
        this.userMapper = userMapper;
        this.notificationMapper = notificationMapper;
        this.realtimePushService = realtimePushService;
    }

    public List<Phase2Dtos.PrivateConversationItem> conversations() {
        Long currentUserId = UserContext.getUserId();
        List<PrivateMessage> rows = privateMessageMapper.selectByUserId(currentUserId);

        Map<Long, PrivateMessage> latestByPeer = new LinkedHashMap<>();
        Map<Long, Long> unreadCountByPeer = new LinkedHashMap<>();
        for (PrivateMessage row : rows) {
            Long peerUserId = Objects.equals(row.getSenderUserId(), currentUserId)
                    ? row.getReceiverUserId()
                    : row.getSenderUserId();
            latestByPeer.putIfAbsent(peerUserId, row);
            if (Objects.equals(row.getReceiverUserId(), currentUserId) && (row.getIsRead() == null || row.getIsRead() == 0)) {
                unreadCountByPeer.merge(peerUserId, 1L, Long::sum);
            }
        }

        List<Long> peerIds = new ArrayList<>(latestByPeer.keySet());
        Map<Long, User> userMap = loadUserMap(peerIds);
        return peerIds.stream()
                .map(peerId -> toConversationItem(peerId, latestByPeer.get(peerId), unreadCountByPeer.getOrDefault(peerId, 0L), userMap.get(peerId)))
                .sorted(Comparator.comparing(Phase2Dtos.PrivateConversationItem::getLastMessageAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    @Transactional
    public List<Phase2Dtos.PrivateMessageItem> messagesWith(Long peerUserId) {
        Long currentUserId = UserContext.getUserId();
        User peer = assertActiveUser(peerUserId);
        List<PrivateMessage> rows = privateMessageMapper.selectConversation(currentUserId, peerUserId);

        privateMessageMapper.update(new LambdaUpdateWrapper<PrivateMessage>()
                .set(PrivateMessage::getIsRead, 1)
                .set(PrivateMessage::getReadAt, LocalDateTime.now())
                .eq(PrivateMessage::getSenderUserId, peerUserId)
                .eq(PrivateMessage::getReceiverUserId, currentUserId)
                .eq(PrivateMessage::getIsRead, 0));

        Map<Long, User> userMap = loadUserMap(List.of(currentUserId, peer.getId()));
        return rows.stream()
                .map(row -> toMessageItem(row, currentUserId, userMap))
                .collect(Collectors.toList());
    }

    @Transactional
    public Phase2Dtos.PrivateMessageItem send(Phase2Dtos.SendPrivateMessageRequest request) {
        Long currentUserId = UserContext.getUserId();
        if (request.getReceiverUserId() == null || Objects.equals(request.getReceiverUserId(), currentUserId)) {
            throw new AppException("不能给自己发送私信");
        }
        User receiver = assertActiveUser(request.getReceiverUserId());
        User sender = assertActiveUser(currentUserId);

        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) {
            throw new AppException("私信内容不能为空");
        }

        PrivateMessage row = new PrivateMessage();
        row.setSenderUserId(currentUserId);
        row.setReceiverUserId(receiver.getId());
        row.setContent(content);
        row.setIsRead(0);
        row.setCreatedAt(LocalDateTime.now());
        privateMessageMapper.insert(row);

        Notification notification = new Notification();
        notification.setUserId(receiver.getId());
        notification.setType("PRIVATE_MESSAGE");
        notification.setTitle(resolveDisplayName(sender) + " 发来一条私信");
        notification.setContent(content.length() > 80 ? content.substring(0, 80) + "..." : content);
        notification.setRelatedId(sender.getId());
        notification.setIsRead(0);
        notification.setCreatedAt(LocalDateTime.now());
        notificationMapper.insert(notification);

        realtimePushService.pushAfterCommit(List.of(currentUserId, receiver.getId()), "private-message", Map.of(
            "senderUserId", currentUserId,
            "receiverUserId", receiver.getId(),
            "messageId", row.getId()
        ));
        realtimePushService.pushAfterCommit(receiver.getId(), "notification", Map.of(
            "notificationType", "PRIVATE_MESSAGE",
            "relatedId", sender.getId()
        ));

        Map<Long, User> userMap = Map.of(
                currentUserId, sender,
                receiver.getId(), receiver
        );
        return toMessageItem(row, currentUserId, userMap);
    }

    public Phase2Dtos.PrivateMessageUnreadResponse unreadCount() {
        Long currentUserId = UserContext.getUserId();
        long unread = privateMessageMapper.countUnreadByUserId(currentUserId);
        Phase2Dtos.PrivateMessageUnreadResponse response = new Phase2Dtos.PrivateMessageUnreadResponse();
        response.setUnreadCount(unread);
        return response;
    }

    private Phase2Dtos.PrivateConversationItem toConversationItem(Long peerUserId,
                                                                  PrivateMessage latest,
                                                                  Long unreadCount,
                                                                  User peer) {
        Phase2Dtos.PrivateConversationItem item = new Phase2Dtos.PrivateConversationItem();
        item.setUserId(peerUserId);
        if (peer != null) {
            item.setUsername(peer.getUsername());
            item.setNickname(peer.getNickname());
            item.setDisplayName(resolveDisplayName(peer));
        } else {
            item.setDisplayName("用户 " + peerUserId);
        }
        item.setLastMessage(latest == null ? "" : latest.getContent());
        item.setLastMessageAt(latest == null ? null : latest.getCreatedAt());
        item.setUnreadCount(unreadCount);
        return item;
    }

    private Phase2Dtos.PrivateMessageItem toMessageItem(PrivateMessage row,
                                                        Long currentUserId,
                                                        Map<Long, User> userMap) {
        Phase2Dtos.PrivateMessageItem item = new Phase2Dtos.PrivateMessageItem();
        item.setId(row.getId());
        item.setSenderUserId(row.getSenderUserId());
        item.setReceiverUserId(row.getReceiverUserId());
        item.setSenderName(resolveDisplayName(userMap.get(row.getSenderUserId()), row.getSenderUserId()));
        item.setReceiverName(resolveDisplayName(userMap.get(row.getReceiverUserId()), row.getReceiverUserId()));
        item.setContent(row.getContent());
        item.setIsRead(row.getIsRead());
        item.setCreatedAt(row.getCreatedAt());
        item.setMine(Objects.equals(row.getSenderUserId(), currentUserId));
        return item;
    }

    private Map<Long, User> loadUserMap(List<Long> userIds) {
        List<Long> distinctIds = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            return Map.of();
        }
        return userMapper.selectBatchIds(distinctIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
    }

    private User assertActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
            throw new AppException("用户不存在或已停用");
        }
        return user;
    }

    private String resolveDisplayName(User user) {
        return resolveDisplayName(user, user == null ? null : user.getId());
    }

    private String resolveDisplayName(User user, Long fallbackUserId) {
        if (user != null) {
            String nickname = user.getNickname();
            if (nickname != null && !nickname.trim().isEmpty()) {
                return nickname.trim();
            }
            String username = user.getUsername();
            if (username != null && !username.trim().isEmpty()) {
                return username.trim();
            }
            fallbackUserId = user.getId();
        }
        return "用户 " + fallbackUserId;
    }
}
