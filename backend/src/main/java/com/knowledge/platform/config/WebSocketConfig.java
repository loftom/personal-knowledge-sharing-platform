package com.knowledge.platform.config;

import com.knowledge.platform.domain.mapper.UserMapper;
import com.knowledge.platform.realtime.RealtimeHandshakeInterceptor;
import com.knowledge.platform.realtime.RealtimePushService;
import com.knowledge.platform.realtime.RealtimeWebSocketHandler;
import com.knowledge.platform.security.JwtUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RealtimePushService realtimePushService;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public WebSocketConfig(RealtimePushService realtimePushService, JwtUtil jwtUtil, UserMapper userMapper) {
        this.realtimePushService = realtimePushService;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
    }

    @Bean
    public RealtimeHandshakeInterceptor realtimeHandshakeInterceptor() {
        return new RealtimeHandshakeInterceptor(jwtUtil, userMapper);
    }

    @Bean
    public RealtimeWebSocketHandler realtimeWebSocketHandler() {
        return new RealtimeWebSocketHandler(realtimePushService);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(realtimeWebSocketHandler(), "/ws/realtime")
                .addInterceptors(realtimeHandshakeInterceptor())
                .setAllowedOrigins("http://localhost:5173", "http://127.0.0.1:5173");
    }
}