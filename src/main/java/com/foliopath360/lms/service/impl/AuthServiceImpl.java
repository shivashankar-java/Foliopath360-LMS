package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.config.JwtService;
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
import com.foliopath360.lms.entity.OtpCode;
import com.foliopath360.lms.entity.OtpPurpose;
import com.foliopath360.lms.entity.RefreshToken;
import com.foliopath360.lms.entity.Role;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.entity.UserStatus;
import com.foliopath360.lms.mapper.AuthMapper;
import com.foliopath360.lms.repository.OtpCodeRepository;
import com.foliopath360.lms.repository.RefreshTokenRepository;
import com.foliopath360.lms.repository.RoleRepository;
import com.foliopath360.lms.repository.UserRepository;
import com.foliopath360.lms.service.AuthService;
import com.foliopath360.lms.service.CaptchaService;
import com.foliopath360.lms.service.EmailService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpCodeRepository otpCodeRepository;

    private final AuthMapper authMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CaptchaService captchaService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.expiry-minutes}")
    private long otpExpiryMinutes;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            OtpCodeRepository otpCodeRepository,
            AuthMapper authMapper,
            JwtService jwtService,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            CaptchaService captchaService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.authMapper = authMapper;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.captchaService = captchaService;
    }

    @Override
    public RegisterResponse registerStudent(RegisterRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (request.getMobileNumber() != null
                && !request.getMobileNumber().isBlank()) {
            if (userRepository.existsByMobileNumber(
                    request.getMobileNumber()
            )) {
                throw new IllegalArgumentException(
                        "Mobile number already exists"
                );
            }
        }

        Role studentRole = roleRepository
                .findByRoleName("STUDENT")
                .orElseThrow(() ->
                        new RuntimeException("STUDENT role not found"));

        User user = authMapper.toEntity(request);

        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setStatus(UserStatus.INACTIVE);
        user.setEnabled(false);
        user.setEmailVerified(false);
        user.setMobileVerified(false);

        user.setRoles(new HashSet<>());
        user.getRoles().add(studentRole);

        User savedUser = userRepository.save(user);

        String otp = createAndSendOtp(
                savedUser, OtpPurpose.EMAIL_VERIFICATION
        );

        RegisterResponse response =
                authMapper.toRegisterResponse(savedUser);

        response.setRole("STUDENT");
        response.setMessage(
                "Student registered successfully. "
                        + "An OTP has been sent to your email. "
                        + "Please verify to activate your account."
        );

        return response;
    }

    @Override
    public MessageResponse verifyStudentOtp(VerifyOtpRequest request) {

        OtpCode otpCode = getValidOtp(
                request.getEmail(),
                request.getOtp(),
                OtpPurpose.EMAIL_VERIFICATION
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException("No account found with this email"));

        otpCode.setUsed(true);
        otpCode.setUsedAt(LocalDateTime.now());

        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);

        return MessageResponse.builder()
                .message("Email verified successfully. You can now login.")
                .build();
    }

    @Override
    public MessageResponse resendStudentOtp(ResendOtpRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException("No account found with this email"));

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new IllegalArgumentException("Email is already verified. Please login.");
        }

        createAndSendOtp(user, OtpPurpose.EMAIL_VERIFICATION);

        return MessageResponse.builder()
                .message("A new OTP has been sent to your email.")
                .build();
    }

    @Override
    public MessageResponse forgotStudentPassword(ForgotPasswordRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElse(null);

        if (user == null || !hasRole(user, "STUDENT")) {
            // Do not reveal whether the account exists
            return MessageResponse.builder()
                    .message("If an account exists with this email, "
                            + "a password reset OTP has been sent.")
                    .build();
        }

        createAndSendOtp(user, OtpPurpose.PASSWORD_RESET);

        return MessageResponse.builder()
                .message("If an account exists with this email, "
                        + "a password reset OTP has been sent.")
                .build();
    }

    @Override
    public MessageResponse resetStudentPassword(ResetPasswordRequest request) {

        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        OtpCode otpCode = getValidOtp(
                request.getEmail(),
                request.getOtp(),
                OtpPurpose.PASSWORD_RESET
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new IllegalArgumentException("No account found with this email"));

        if (!hasRole(user, "STUDENT")) {
            throw new IllegalArgumentException(
                    "Password reset is only available for student accounts"
            );
        }

        otpCode.setUsed(true);
        otpCode.setUsedAt(LocalDateTime.now());

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );

        userRepository.save(user);

        return MessageResponse.builder()
                .message("Password reset successfully. You can now login with your new password.")
                .build();
    }

    private String createAndSendOtp(User user, OtpPurpose purpose) {

        // Housekeeping: remove expired OTPs
        otpCodeRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        String otp = generateOtp();

        otpCodeRepository.save(
                OtpCode.builder()
                        .email(user.getEmail())
                        .otp(otp)
                        .purpose(purpose)
                        .expiresAt(LocalDateTime.now().plusMinutes(otpExpiryMinutes))
                        .build()
        );

        if (purpose == OtpPurpose.PASSWORD_RESET) {
            emailService.sendPasswordResetOtpEmail(
                    user.getEmail(), user.getFirstName(), otp
            );
        } else {
            emailService.sendOtpEmail(
                    user.getEmail(), user.getFirstName(), otp
            );
        }

        return otp;
    }

    private OtpCode getValidOtp(String email, String otp, OtpPurpose purpose) {

        OtpCode otpCode = otpCodeRepository
                .findFirstByEmailAndPurposeAndUsedFalseOrderByCreatedDtDesc(email, purpose)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No pending OTP found. Please request a new one."));

        if (!otpCode.getOtp().equals(otp)) {
            throw new IllegalArgumentException("Invalid OTP");
        }

        if (otpCode.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        return otpCode;
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRoles().stream()
                .anyMatch(role -> role.getRoleName().equals(roleName));
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    @Override
    public MessageResponse setStaffPassword(SetPasswordRequest request) {

        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = userRepository.findBySetupToken(request.getToken())
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid or expired setup link"));

        if (user.getSetupTokenExpiresAt() == null
                || user.getSetupTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Setup link has expired. Contact the administrator.");
        }

        user.setPassword(
                passwordEncoder.encode(request.getNewPassword())
        );
        user.setSetupToken(null);
        user.setSetupTokenExpiresAt(null);
        user.setEnabled(true);
        user.setStatus(UserStatus.ACTIVE);

        userRepository.save(user);

        return MessageResponse.builder()
                .message("Password set successfully. Your account is now active. Please login.")
                .build();
    }

    @Override
    public LoginResponse login(LoginRequest request) {

        if (!captchaService.validateCaptcha(
                request.getCaptchaId(),
                request.getCaptchaCode()
        )) {
            throw new IllegalArgumentException(
                    "Invalid or expired captcha. Please try again."
            );
        }

        User user = userRepository
                .findByUsername(request.getUsernameOrEmail())
                .or(() ->
                        userRepository.findByEmail(
                                request.getUsernameOrEmail()
                        )
                )
                .orElseThrow(() ->
                        new BadCredentialsException(
                                "Invalid username/email or password"
                        ));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BadCredentialsException("User account is disabled");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException(
                    "User account is not active"
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new BadCredentialsException(
                    "Invalid username/email or password"
            );
        }

        String accessToken =
                jwtService.generateAccessToken(user);

        String refreshTokenValue =
                UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenValue)
                .user(user)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        UserResponse userResponse =
                buildUserResponse(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .tokenType("Bearer")
                .expiresIn(
                        jwtService.getAccessTokenExpiration() / 1000
                )
                .user(userResponse)
                .build();
    }

    @Override
    public RefreshTokenResponse refreshToken(
            RefreshTokenRequest request
    ) {

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByToken(request.getRefreshToken())
                        .orElseThrow(() ->
                                new BadCredentialsException(
                                        "Invalid refresh token"
                                ));

        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            throw new BadCredentialsException(
                    "Refresh token has been revoked"
            );
        }

        if (refreshToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);

            throw new BadCredentialsException(
                    "Refresh token has expired"
            );
        }

        User user = refreshToken.getUser();

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new BadCredentialsException("User account is disabled");
        }

        String newAccessToken =
                jwtService.generateAccessToken(user);

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(
                        jwtService.getAccessTokenExpiration() / 1000
                )
                .build();
    }

    @Override
    public MessageResponse logout(LogoutRequest request) {

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByToken(request.getRefreshToken())
                        .orElseThrow(() ->
                                new BadCredentialsException(
                                        "Invalid refresh token"
                                ));

        refreshToken.setRevoked(true);

        refreshTokenRepository.save(refreshToken);

        return MessageResponse.builder()
                .message("Logout successful")
                .build();
    }

    @Override
    public UserResponse getCurrentUser(User user) {
        return buildUserResponse(user);
    }

    private UserResponse buildUserResponse(User user) {

        UserResponse userResponse =
                authMapper.toUserResponse(user);

        userResponse.setRoles(
                user.getRoles()
                        .stream()
                        .map(Role::getRoleName)
                        .collect(Collectors.toSet())
        );

        return userResponse;
    }
}
