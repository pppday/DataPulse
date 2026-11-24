package com.pulseos.datapulse.config;

import com.pulseos.datapulse.entity.User;

/**
 * 基于 {@link ThreadLocal} 的当前请求登录用户上下文工具类。
 * <p>
 * 由 {@link JwtAuthFilter} 在每次请求验签通过后写入当前用户，并在 {@code finally} 块中强制清理，
 * 让 Service 层无需层层透传 {@code HttpServletRequest} 就能直接获取当前操作人的 ID 和用户名（如记录查询审计日志）。
 */
public class UserContext {
    /** 线程私有存储槽：每个处理请求的 Tomcat 线程互不干扰 */
    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    /** 绑定当前线程的登录用户（仅由认证过滤器调用） */
    public static void setCurrentUser(User user) {
        CURRENT_USER.set(user);
    }

    /** 获取当前登录用户完整对象，未登录时返回 {@code null} */
    public static User getCurrentUser() {
        return CURRENT_USER.get();
    }

    /** 获取当前用户 ID，未登录或后台任务调用时兜底返回 1L（系统默认账号） */
    public static Long getCurrentUserId() {
        User user = CURRENT_USER.get();
        return user != null ? user.getId() : 1L;
    }

    /** 获取当前用户名，未登录时兜底返回 {@code "system"} */
    public static String getCurrentUsername() {
        User user = CURRENT_USER.get();
        return user != null ? user.getUsername() : "system";
    }

    /** 清空当前线程绑定的用户信息，防止线程池复用造成内存泄漏或跨请求用户串号 */
    public static void clear() {
        CURRENT_USER.remove();
    }
}
