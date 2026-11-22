package com.pulseos.datapulse.config;

import com.pulseos.datapulse.entity.User;

public class UserContext {
    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    public static void setCurrentUser(User user) {
        CURRENT_USER.set(user);
    }

    public static User getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static Long getCurrentUserId() {
        User user = CURRENT_USER.get();
        return user != null ? user.getId() : 1L;
    }

    public static String getCurrentUsername() {
        User user = CURRENT_USER.get();
        return user != null ? user.getUsername() : "system";
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
