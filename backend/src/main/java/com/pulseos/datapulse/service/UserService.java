package com.pulseos.datapulse.service;

import com.pulseos.datapulse.dto.AuthResponse;
import com.pulseos.datapulse.dto.LoginRequest;
import com.pulseos.datapulse.entity.User;

public interface UserService {
    AuthResponse login(LoginRequest request);
    User getCurrentUser();
}
