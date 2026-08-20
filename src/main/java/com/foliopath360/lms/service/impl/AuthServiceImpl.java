package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.config.JwtService;
import com.foliopath360.lms.dto.request.LoginRequest;
import com.foliopath360.lms.dto.request.LogoutRequest;
import com.foliopath360.lms.dto.request.RefreshTokenRequest;
import com.foliopath360.lms.dto.request.RegisterRequest;
import com.foliopath360.lms.dto.response.*;
import com.foliopath360.lms.entity.RefreshToken;
import com.foliopath360.lms.entity.Role;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.entity.UserStatus;
import com.foliopath360.lms.mapper.AuthMapper;
import com.foliopath360.lms.repository.RefreshTokenRepository;
import com.foliopath360.lms.repository.RoleRepository;
import com.foliopath360.lms.repository.UserRepository;
import com.foliopath360.lms.service.AuthService;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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

    private final AuthMapper authMapper;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            RefreshTokenRepository refreshTokenRepository,
            AuthMapper authMapper,
            JwtService jwtService,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authMapper = authMapper;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public RegisterResponse register(RegisterRequest request) {
        return registerStudent(request);
    }

    @Override
    public RegisterResponse registerAdmin(RegisterRequest request) {
        return registerUser(request, "ADMIN", "Admin registered successfully");
    }

    @Override
    public RegisterResponse registerStudent(RegisterRequest request) {
        return registerUser(
                request, "STUDENT", "Student registered successfully"
        );
    }

    private RegisterResponse registerUser(
            RegisterRequest request,
            String roleName,
            String successMessage
    ) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        if (request.getMobileNumber() != null
                && !request.getMobileNumber().isBlank()) {
            if (userRepository.existsByMobileNumber(
                    request.getMobileNumber()
            )) {
                throw new RuntimeException(
                        "Mobile number already exists"
                );
            }
        }

        Role role = roleRepository
                .findByRoleName(roleName)
                .orElseThrow(() ->
                        new RuntimeException(
                                roleName + " role not found"
                        ));

        User user = authMapper.toEntity(request);

        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setStatus(UserStatus.ACTIVE);
        user.setEnabled(true);
        user.setEmailVerified(false);
        user.setMobileVerified(false);

        user.setRoles(new HashSet<>());
        user.getRoles().add(role);

        User savedUser = userRepository.save(user);

        RegisterResponse response =
                authMapper.toRegisterResponse(savedUser);

        response.setRole(roleName);
        response.setMessage(successMessage);

        return response;
    }

    @Override
    public LoginResponse login(LoginRequest request) {

        User user = userRepository
                .findByUsername(request.getUsernameOrEmail())
                .or(() ->
                        userRepository.findByEmail(
                                request.getUsernameOrEmail()
                        )
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Invalid username/email or password"
                        ));

        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new RuntimeException("User account is disabled");
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException(
                    "User account is not active"
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new RuntimeException(
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
                                new RuntimeException(
                                        "Invalid refresh token"
                                ));

        if (Boolean.TRUE.equals(refreshToken.getRevoked())) {
            throw new RuntimeException(
                    "Refresh token has been revoked"
            );
        }

        if (refreshToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);

            throw new RuntimeException(
                    "Refresh token has expired"
            );
        }

        String newAccessToken =
                jwtService.generateAccessToken(
                        refreshToken.getUser()
                );

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
                                new RuntimeException(
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
