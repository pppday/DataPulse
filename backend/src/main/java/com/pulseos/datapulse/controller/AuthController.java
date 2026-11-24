package com.pulseos.datapulse.controller;

import com.pulseos.datapulse.common.Result;
import com.pulseos.datapulse.dto.AuthResponse;
import com.pulseos.datapulse.dto.LoginRequest;
import com.pulseos.datapulse.entity.User;
import com.pulseos.datapulse.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户身份认证与登录态查询控制器。
 * <p>
 * 挂载在 {@code /api/v1/auth/**} 白名单下，提供账号密码登录签发 JWT 以及获取当前登录用户资料接口。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** 校验账号密码并签发无状态 JWT 令牌，同时返回用户基础资料与角色信息 */
    @PostMapping("/login")
    public Result<AuthResponse> login(@RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    /** 根据当前请求携带的 JWT 返回登录用户详细信息（已自动脱敏去除密码字段） */
    @GetMapping("/me")
    public Result<User> me() {
        return Result.success(userService.getCurrentUser());
    }
}
