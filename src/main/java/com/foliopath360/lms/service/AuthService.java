package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.LoginRequest;
import com.foliopath360.lms.dto.request.LogoutRequest;
import com.foliopath360.lms.dto.request.RefreshTokenRequest;
import com.foliopath360.lms.dto.request.RegisterRequest;
import com.foliopath360.lms.dto.response.*;
import com.foliopath360.lms.entity.User;

public interface AuthService {

    RegisterResponse register(RegisterRequest request);

    RegisterResponse registerAdmin(RegisterRequest request);

    RegisterResponse registerStudent(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    RefreshTokenResponse refreshToken(RefreshTokenRequest request);

    MessageResponse logout(LogoutRequest request);

    UserResponse getCurrentUser(User user);
}
