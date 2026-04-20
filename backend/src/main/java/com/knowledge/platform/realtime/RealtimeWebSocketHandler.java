package com.knowledge.platform.realtime;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class RealtimeWebSocketHandler extends TextWebSocketHandler {

    private final RealtimePushService realtimePushService;

    public RealtimeWebSocketHandler(RealtimePushService realtimePushService) {
        this.realtimePushService = realtimePushService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Object userIdValue = session.getAttributes().get("userId");
        if (!(userIdValue instanceof Long userId)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("missing user id"));
            return;
        }
        realtimePushService.register(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Client messages are not required for the demo channel.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        realtimePushService.unregister(session);
    }
}