package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.ForgotPasswordRequest;
import com.foliopath360.lms.dto.request.LoginRequest;
import com.foliopath360.lms.dto.request.LogoutRequest;
import com.foliopath360.lms.dto.request.RefreshTokenRequest;
import com.foliopath360.lms.dto.request.RegisterRequest;
import com.foliopath360.lms.dto.request.ResendOtpRequest;
import com.foliopath360.lms.dto.request.ResetPasswordRequest;
import com.foliopath360.lms.dto.request.SetPasswordRequest;
import com.foliopath360.lms.dto.request.VerifyOtpRequest;
import com.foliopath360.lms.dto.response.CaptchaResponse;
import com.foliopath360.lms.dto.response.LoginResponse;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.RefreshTokenResponse;
import com.foliopath360.lms.dto.response.RegisterResponse;
import com.foliopath360.lms.dto.response.UserResponse;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.service.AuthService;
import com.foliopath360.lms.service.CaptchaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    @GetMapping("/captcha")
    public ResponseEntity<CaptchaResponse> getCaptcha() {

        var result = captchaService.generateCaptcha();

        return ResponseEntity.ok(
                CaptchaResponse.builder()
                        .captchaId(result.captchaId())
                        .image("data:image/png;base64," + result.imageBase64())
                        .build()
        );
    }

    @PostMapping("/student/register")
    public ResponseEntity<RegisterResponse> registerStudent(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(authService.registerStudent(request));
    }

    @PostMapping("/student/verify-otp")
    public ResponseEntity<MessageResponse> verifyStudentOtp(
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        return ResponseEntity.ok(
                authService.verifyStudentOtp(request)
        );
    }

    @PostMapping("/student/resend-otp")
    public ResponseEntity<MessageResponse> resendStudentOtp(
            @Valid @RequestBody ResendOtpRequest request
    ) {
        return ResponseEntity.ok(
                authService.resendStudentOtp(request)
        );
    }

    @PostMapping("/staff/set-password")
    public ResponseEntity<MessageResponse> setStaffPassword(
            @Valid @RequestBody SetPasswordRequest request
    ) {
        return ResponseEntity.ok(
                authService.setStaffPassword(request)
        );
    }

    @PostMapping("/student/forgot-password")
    public ResponseEntity<MessageResponse> forgotStudentPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        return ResponseEntity.ok(
                authService.forgotStudentPassword(request)
        );
    }

    @PostMapping("/student/reset-password")
    public ResponseEntity<MessageResponse> resetStudentPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        return ResponseEntity.ok(
                authService.resetStudentPassword(request)
        );
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(
                authService.login(request)
        );
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(
                authService.refreshToken(request)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @Valid @RequestBody LogoutRequest request
    ) {
        return ResponseEntity.ok(
                authService.logout(request)
        );
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(
                authService.getCurrentUser(user)
        );
    }
}
