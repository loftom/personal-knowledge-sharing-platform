package com.zhihu.platform.security;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expire-seconds}")
    private long expireSeconds;

    public String createToken(Long userId, String role) {
        // Simple token format for MVP: UID:<userId>:ROLE:<role>
        return "UID:" + userId + ":ROLE:" + role;
    }

    public Map<String, Object> parseToken(String token) {
        // Parse the simple token format produced by createToken
        Map<String, Object> claims = new HashMap<>();
        if (token == null) return claims;
        try {
            String[] parts = token.split(":");
            for (int i = 0; i < parts.length - 1; i++) {
                if ("UID".equalsIgnoreCase(parts[i]) && i + 1 < parts.length) {
                    claims.put("uid", Long.parseLong(parts[i + 1]));
                }
                if ("ROLE".equalsIgnoreCase(parts[i]) && i + 1 < parts.length) {
                    claims.put("role", parts[i + 1]);
                }
            }
        } catch (Exception e) {
            // ignore and return empty claims
        }
        return claims;
    }
}
