package com.pulseos.datapulse.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulseos.datapulse.common.Result;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

/**
 * Spring Security 无状态安全防护与鉴权链配置。
 * <p>
 * 核心设计思路：
 * 1. 前后端分离 + JWT 认证：关闭 CSRF、关闭 HttpSession（{@code STATELESS}），每次请求独立验签；
 * 2. 开启 {@code @EnableMethodSecurity}，支持在 Controller 方法上用 {@code @PreAuthorize("hasRole('ADMIN')")} 做细粒度权限管控；
 * 3. 明确区分 401 与 403 响应：未登录/Token 过期走 {@code authenticationEntryPoint} 返回 401，
 *    已登录但角色权限不足走 {@code accessDeniedHandler} 返回 403，且均按统一 {@link Result} JSON 格式输出。
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;

    /** 使用 BCrypt 强哈希算法加密与比对用户密码（自带随机盐值，抗彩虹表） */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 组装 HTTP 安全过滤链：定义白名单接口、401/403 统一 JSON 响应以及 JWT 过滤器挂载顺序。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 纯 REST Token 接口不依赖浏览器 Cookie 会话，直接关闭 CSRF 防护
                .csrf(AbstractHttpConfigurer::disable)
                // 声明为无状态会话，服务器不创建也不存储 HttpSession
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        // 401 未认证入口：请求没带 Token、Token 无效或已过期时，直接返回 JSON 401 提醒前端跳回登录页
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    Result.error(401, "用户未登录或登录凭证已过期，请重新登录")
                            ));
                        })
                        // 403 无权限处理器：已通过登录认证但角色不满足要求（如普通分析师改数据源配置），返回 JSON 403
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(objectMapper.writeValueAsString(
                                    Result.error(403, "操作权限不足 (403 Forbidden)：当前操作仅限企业管理员 (ROLE_ADMIN)")
                            ));
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // 仅放行登录/注册认证接口、健康探针以及默认错误页，其余业务 API 必须登录认证
                        .requestMatchers("/api/v1/auth/**", "/api/v1/health", "/error", "/favicon.ico").permitAll()
                        .anyRequest().authenticated()
                )
                // 把自定义 JWT 过滤器插在 Spring Security 默认表单登录过滤器之前执行
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
