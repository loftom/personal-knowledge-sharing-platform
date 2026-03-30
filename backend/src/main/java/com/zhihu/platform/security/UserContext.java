package com.zhihu.platform.security;

public class UserContext {
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE_HOLDER = new ThreadLocal<>();

    public static void set(Long userId, String role) {
        USER_ID_HOLDER.set(userId);
        ROLE_HOLDER.set(role);
    }

    public static Long getUserId() {
        return USER_ID_HOLDER.get();
    }

    public static String getRole() {
        return ROLE_HOLDER.get();
    }

    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(ROLE_HOLDER.get());
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        ROLE_HOLDER.remove();
    }
}
