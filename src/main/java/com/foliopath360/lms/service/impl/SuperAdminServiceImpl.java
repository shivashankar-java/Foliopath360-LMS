package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.request.StaffCreateRequest;
import com.foliopath360.lms.dto.request.StaffStatusUpdateRequest;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.StaffResponse;
import com.foliopath360.lms.dto.response.SuperAdminDashboardResponse;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.mapper.StaffMapper;
import com.foliopath360.lms.repository.*;
import com.foliopath360.lms.service.EmailService;
import com.foliopath360.lms.service.SuperAdminService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class SuperAdminServiceImpl implements SuperAdminService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final StaffRepository staffRepository;
    private final StaffMapper staffMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.staff-setup.link-base-url}")
    private String setupLinkBaseUrl;

    public SuperAdminServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            StaffRepository staffRepository,
            StaffMapper staffMapper,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.staffRepository = staffRepository;
        this.staffMapper = staffMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public SuperAdminDashboardResponse getDashboard() {

        long totalUsers = userRepository.count();
        long totalStaff = userRepository.countByRolesRoleName("STAFF");
        long totalStudents = userRepository.countByRolesRoleName("STUDENT");
        long totalSuperAdmins = userRepository.countByRolesRoleName("SUPER_ADMIN");

        return SuperAdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalStaff(totalStaff)
                .totalStudents(totalStudents)
                .totalSuperAdmins(totalSuperAdmins)
                .build();
    }

    @Override
    @Transactional
    public StaffResponse createStaff(StaffCreateRequest request) {

        // 1. Check email
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // 2. Check mobile number
        if (request.getMobileNumber() != null
                && !request.getMobileNumber().isBlank()
                && userRepository.existsByMobileNumber(request.getMobileNumber())) {

            throw new IllegalArgumentException("Mobile number already exists");
        }

        // 3. Get STAFF role
        Role staffRole = roleRepository.findByRoleName("STAFF")
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Role",
                                "roleName",
                                "STAFF"
                        )
                );

        // 4. Create user (inactive until staff sets password via email link)
        String setupToken = generateSetupToken();

        User user = User.builder()
                .username(request.getEmail())
                .email(request.getEmail())
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .mobileNumber(request.getMobileNumber())
                .status(UserStatus.INACTIVE)
                .enabled(false)
                .emailVerified(false)
                .mobileVerified(false)
                .setupToken(setupToken)
                .setupTokenExpiresAt(LocalDateTime.now().plusHours(24))
                .roles(new HashSet<>(Set.of(staffRole)))
                .build();

        // 5. Save user
        User savedUser = userRepository.save(user);

        // 6. Generate staff code
        String staffCode = generateStaffCode();

        // 7. Create staff profile
        Staff staff = staffMapper.toStaffEntity(request);

        staff.setUser(savedUser);
        staff.setStaffCode(staffCode);
        staff.setJoinedAt(LocalDateTime.now());

        // 8. Save staff
        Staff savedStaff = staffRepository.save(staff);

        // 9. Send setup email with one-time link
        String setupLink = setupLinkBaseUrl + "?token=" + setupToken;

        emailService.sendStaffSetupEmail(
                savedUser.getEmail(),
                savedUser.getFirstName(),
                setupLink
        );

        // 10. Build response
        StaffResponse response =
                staffMapper.toStaffResponse(savedStaff);

        response.setRoles(
                savedUser.getRoles()
                        .stream()
                        .map(Role::getRoleName)
                        .collect(Collectors.toSet())
        );

        return response;
    }

    @Override
    @Transactional
    public List<StaffResponse> getAllStaff() {

        return staffRepository.findAll().stream()
                .map(staff -> {
                    StaffResponse response = staffMapper.toStaffResponse(staff);
                    response.setRoles(staff.getUser().getRoles().stream()
                            .map(Role::getRoleName)
                            .collect(Collectors.toSet()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StaffResponse getStaffById(UUID userId) {

        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "userId", userId));

        StaffResponse response = staffMapper.toStaffResponse(staff);
        response.setRoles(staff.getUser().getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toSet()));

        return response;
    }

    @Override
    public StaffResponse updateStaff(UUID userId, StaffCreateRequest request) {

        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "userId", userId));

        User user = staff.getUser();

        if (!user.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMobileNumber(request.getMobileNumber());

        if (!user.getEmail().equals(request.getEmail())) {
            user.setEmail(request.getEmail());
            user.setUsername(request.getEmail());
        }

        userRepository.save(user);

        staff.setDesignation(request.getDesignation());
        staff.setDepartment(request.getDepartment());
        staff.setQualification(request.getQualification());
        staff.setSpecialization(request.getSpecialization());

        Staff savedStaff = staffRepository.save(staff);

        StaffResponse response = staffMapper.toStaffResponse(savedStaff);
        response.setRoles(user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toSet()));

        return response;
    }

    @Override
    public StaffResponse updateStaffStatus(UUID userId, StaffStatusUpdateRequest request) {

        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "userId", userId));

        User user = staff.getUser();
        user.setEnabled(request.getEnabled());

        if (!request.getEnabled()) {
            user.setStatus(UserStatus.INACTIVE);
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }

        userRepository.save(user);

        StaffResponse response = staffMapper.toStaffResponse(staff);
        response.setRoles(user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toSet()));

        return response;
    }

    @Override
    public MessageResponse deleteStaff(UUID userId) {

        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "userId", userId));

        staffRepository.delete(staff);
        userRepository.delete(staff.getUser());

        return MessageResponse.builder()
                .message("Staff deleted successfully")
                .build();
    }

    private String generateSetupToken() {

        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }

        return sb.toString();
    }

    private String generateStaffCode() {
        return "STF-" + System.currentTimeMillis() % 100000;
    }
}
