package com.pulseos.datapulse.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户账号密码登录请求体（DTO）。
 * <p>
 * 由 {@code POST /api/v1/auth/login} 接收并做非空校验。
 */
@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;
}
