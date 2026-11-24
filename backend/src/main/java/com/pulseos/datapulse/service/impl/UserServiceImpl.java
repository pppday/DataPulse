package com.pulseos.datapulse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.pulseos.datapulse.common.BusinessException;
import com.pulseos.datapulse.config.JwtTokenProvider;
import com.pulseos.datapulse.config.UserContext;
import com.pulseos.datapulse.dto.AuthResponse;
import com.pulseos.datapulse.dto.LoginRequest;
import com.pulseos.datapulse.entity.User;
import com.pulseos.datapulse.mapper.UserMapper;
import com.pulseos.datapulse.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 【用户登录认证与 RBAC 演示账号初始化服务】
 * 启动时幂等确保 `admin`（总架构师）与 `analyst`（数据分析师）两个演示账号存在且采用 BCrypt 加密密码；
 * 登录校验通过后调用 JwtTokenProvider 签发 24 小时有效的 Bearer JWT。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @PostConstruct
    public void initDemoUsers() {
        try {
            // 1. 确保系统总架构师 (admin) 存在且权限为 ROLE_ADMIN
            User admin = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin"));
            if (admin == null) {
                admin = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .realName("赵岚 (企业总架构师)")
                        .email("3512473224@qq.com")
                        .role("ROLE_ADMIN")
                        .avatar("https://api.dicebear.com/7.x/notionists/svg?seed=admin")
                        .status(1)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                userMapper.insert(admin);
                log.info("[RBAC初始化] 创建系统总架构师账号: admin (ROLE_ADMIN)");
            } else {
                admin.setRole("ROLE_ADMIN");
                admin.setPassword(passwordEncoder.encode("admin123"));
                userMapper.updateById(admin);
            }

            // 2. 确保数据分析师 (analyst) 存在且权限为 ROLE_ANALYST
            User analyst = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, "analyst"));
            if (analyst == null) {
                analyst = User.builder()
                        .username("analyst")
                        .password(passwordEncoder.encode("data123"))
                        .realName("李晨 (资深数据分析师)")
                        .email("ppday666@gmail.com")
                        .role("ROLE_ANALYST")
                        .avatar("https://api.dicebear.com/7.x/notionists/svg?seed=analyst")
                        .status(1)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                userMapper.insert(analyst);
                log.info("[RBAC初始化] 创建资深数据分析师账号: analyst (ROLE_ANALYST)");
            } else {
                analyst.setRole("ROLE_ANALYST");
                analyst.setPassword(passwordEncoder.encode("data123"));
                userMapper.updateById(analyst);
            }
        } catch (Exception e) {
            log.warn("[RBAC初始化] 初始化演示账号警告: {}", e.getMessage());
        }
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername().trim())
        );

        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw new BusinessException(403, "当前账号已被禁用，请联系总架构师");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        String token = tokenProvider.generateToken(
                user.getUsername(),
                user.getId(),
                user.getRealName(),
                user.getRole()
        );

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(86400000L)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .email(user.getEmail())
                .role(user.getRole())
                .avatar(user.getAvatar())
                .build();
    }

    @Override
    public User getCurrentUser() {
        Long userId = UserContext.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
}
