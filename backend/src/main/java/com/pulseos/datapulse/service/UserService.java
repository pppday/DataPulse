package com.pulseos.datapulse.service;

import com.pulseos.datapulse.dto.AuthResponse;
import com.pulseos.datapulse.dto.LoginRequest;
import com.pulseos.datapulse.entity.User;

/** 用户登录认证与当前登录态查询服务接口。 */
public interface UserService {
    AuthResponse login(LoginRequest request);
    User getCurrentUser();
}
