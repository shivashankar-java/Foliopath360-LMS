package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.ForgotPasswordRequest;
import com.foliopath360.lms.dto.request.LoginRequest;
import com.foliopath360.lms.dto.request.LogoutRequest;
import com.foliopath360.lms.dto.request.RefreshTokenRequest;
import com.foliopath360.lms.dto.request.RegisterRequest;
import com.foliopath360.lms.dto.request.ResendOtpRequest;
import com.foliopath360.lms.dto.request.ResetPasswordRequest;
import com.foliopath360.lms.dto.request.SetPasswordRequest;
import com.foliopath360.lms.dto.request.VerifyOtpRequest;
import com.foliopath360.lms.dto.response.*;
import com.foliopath360.lms.entity.User;

public interface AuthService {

    RegisterResponse registerStudent(RegisterRequest request);

    MessageResponse verifyStudentOtp(VerifyOtpRequest request);

    MessageResponse resendStudentOtp(ResendOtpRequest request);

    MessageResponse forgotStudentPassword(ForgotPasswordRequest request);

    MessageResponse resetStudentPassword(ResetPasswordRequest request);

    MessageResponse setStaffPassword(SetPasswordRequest request);

    LoginResponse login(LoginRequest request);

    RefreshTokenResponse refreshToken(RefreshTokenRequest request);

    MessageResponse logout(LogoutRequest request);

    UserResponse getCurrentUser(User user);
}
