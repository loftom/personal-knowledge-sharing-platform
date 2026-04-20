package com.knowledge.platform.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RealtimePushService {

    private final ObjectMapper objectMapper;
    private final Map<Long, Set<WebSocketSession>> sessionsByUser = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionIndex = new ConcurrentHashMap<>();

    public RealtimePushService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(Long userId, WebSocketSession session) {
        sessionsByUser.computeIfAbsent(userId, key -> ConcurrentHashMap.newKeySet()).add(session);
        sessionIndex.put(session.getId(), userId);
        send(session, event("connected", Map.of("userId", userId)));
    }

    public void unregister(WebSocketSession session) {
        Long userId = sessionIndex.remove(session.getId());
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                sessionsByUser.remove(userId, sessions);
            }
        }
    }

    public void push(Long userId, String type, Object payload) {
        Set<WebSocketSession> sessions = sessionsByUser.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        Object message = event(type, payload);
        for (WebSocketSession session : sessions) {
            send(session, message);
        }
    }

    public void push(Collection<Long> userIds, String type, Object payload) {
        if (userIds == null) {
            return;
        }
        for (Long userId : userIds) {
            if (userId != null) {
                push(userId, type, payload);
            }
        }
    }

    public void pushAfterCommit(Long userId, String type, Object payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push(userId, type, payload);
                }
            });
            return;
        }
        push(userId, type, payload);
    }

    public void pushAfterCommit(Collection<Long> userIds, String type, Object payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push(userIds, type, payload);
                }
            });
            return;
        }
        push(userIds, type, payload);
    }

    private Map<String, Object> event(String type, Object payload) {
        return Map.of(
                "type", type,
                "data", payload,
                "timestamp", Instant.now().toEpochMilli()
        );
    }

    private void send(WebSocketSession session, Object payload) {
        if (!session.isOpen()) {
            unregister(session);
            return;
        }
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        } catch (IOException e) {
            unregister(session);
        }
    }
}