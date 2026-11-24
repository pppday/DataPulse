package com.pulseos.datapulse.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户登录成功后的认证响应载荷（DTO）。
 * <p>
 * 返回签发好的无状态 JWT 令牌、过期时长以及当前登录人的基础档案与角色，供前端存入本地状态并控制管理员菜单可见性。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    private Long expiresIn;
    private Long userId;
    private String username;
    private String realName;
    private String email;
    private String role;
    private String avatar;
}
