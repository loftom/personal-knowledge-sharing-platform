package com.zhihu.platform.security;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/api/public/**",
            "/error"
    );

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(200);
            return true;
        }

        String path = request.getRequestURI();
        if ("GET".equalsIgnoreCase(request.getMethod()) && MATCHER.match("/api/interaction/comment/**", path)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(request.getMethod()) && MATCHER.match("/api/qa/**", path)) {
            return true;
        }
        if (PUBLIC_PATHS.stream().anyMatch(pattern -> MATCHER.match(pattern, path))) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }

            try {
                String token = authHeader.substring(7);
                Map<String, Object> claims = jwtUtil.parseToken(token);
                Object uidObj = claims.get("uid");
                Object roleObj = claims.get("role");
                if (uidObj == null) {
                    response.setStatus(401);
                    return false;
                }
                Long userId = uidObj instanceof Number ? ((Number) uidObj).longValue() : Long.parseLong(String.valueOf(uidObj));
                String role = roleObj == null ? "USER" : String.valueOf(roleObj);
                UserContext.set(userId, role);
                return true;
            } catch (Exception e) {
                response.setStatus(401);
                return false;
            }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
