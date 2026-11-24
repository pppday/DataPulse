package com.pulseos.datapulse.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pulseos.datapulse.entity.User;
import com.pulseos.datapulse.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * 无状态 JWT 认证过滤器（每次 HTTP 请求仅执行一次）。
 * <p>
 * 挂载在 Spring Security 过滤器链的 {@code UsernamePasswordAuthenticationFilter} 之前：
 * 从请求头或 URL 参数中提取并校验 JWT，验签通过后实时回查数据库确认账号处于启用状态，
 * 再把认证信息写入 {@link SecurityContextHolder} 与线程本地变量 {@link UserContext}。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            // 第一步：从请求头（Authorization: Bearer xxx）或 URL 参数（?token=xxx）提取 JWT
            String jwt = getJwtFromRequest(request);

            // 第二步：只要携带了合法且未过期的 JWT，就解析出用户名并回查数据库确认用户实时状态
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                Claims claims = tokenProvider.getClaimsFromToken(jwt);
                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                User user = userMapper.selectOne(
                        new LambdaQueryWrapper<User>().eq(User::getUsername, username)
                );

                // 仅当账号存在且未被禁用（status == 1）时才放行认证，防止已封禁账号拿旧 Token 继续操作
                if (user != null && user.getStatus() == 1) {
                    // 优先采用数据库中的最新角色，兼容管理员在后台实时调整权限的场景
                    String actualRole = user.getRole() != null ? user.getRole() : (role != null ? role : "ROLE_ANALYST");
                    String roleWithPrefix = actualRole.startsWith("ROLE_") ? actualRole : "ROLE_" + actualRole;
                    String roleWithoutPrefix = roleWithPrefix.substring(5);

                    // 同时注入带 ROLE_ 前缀与不带前缀的权限标识，兼容 hasRole('ADMIN') 和 hasAuthority('ROLE_ADMIN') 两种写法
                    java.util.List<SimpleGrantedAuthority> authorities = java.util.Arrays.asList(
                            new SimpleGrantedAuthority(roleWithPrefix),
                            new SimpleGrantedAuthority(roleWithoutPrefix)
                    );
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 写入 Spring Security 上下文供鉴权使用，同时写入 ThreadLocal 方便业务代码随时获取当前用户
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    UserContext.setCurrentUser(user);
                }
            }
            // 继续放行给后续过滤器（若未认证且访问受保护接口，Security 会统一拦截返回 401）
            filterChain.doFilter(request, response);
        } finally {
            // 极其关键：请求结束后必须清理 ThreadLocal，防止 Tomcat 线程池复用导致内存泄漏或用户信息串号
            UserContext.clear();
        }
    }

    /**
     * 提取请求中的 JWT 字符串：优先读标准 {@code Authorization: Bearer <token>} 请求头，
     * 若没有再兜底读取 URL 参数 {@code token}（用于兼容浏览器直接触发导出下载等不便带 Header 的场景）。
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        String paramToken = request.getParameter("token");
        if (StringUtils.hasText(paramToken)) {
            return paramToken;
        }
        return null;
    }
}
