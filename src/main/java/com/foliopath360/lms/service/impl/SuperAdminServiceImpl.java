package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.request.ResetPasswordByAdminRequest;
import com.foliopath360.lms.dto.request.StaffCreateRequest;
import com.foliopath360.lms.dto.request.StaffStatusUpdateRequest;
import com.foliopath360.lms.dto.request.StudentStatusUpdateRequest;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.RecentEnrollmentResponse;
import com.foliopath360.lms.dto.response.RecentRegistrationResponse;
import com.foliopath360.lms.dto.response.StaffResponse;
import com.foliopath360.lms.dto.response.StudentAdminResponse;
import com.foliopath360.lms.dto.response.StudentProfileAdminResponse;
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
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentProfileRepository studentProfileRepository;
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
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            StudentProfileRepository studentProfileRepository,
            StaffMapper staffMapper,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.staffRepository = staffRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentProfileRepository = studentProfileRepository;
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

        // Courses
        long totalCourses = courseRepository.count();
        long publishedCourses = courseRepository.countByStatus(CourseStatus.PUBLISHED);
        long draftCourses = courseRepository.countByStatus(CourseStatus.DRAFT);

        // Enrollments
        long totalEnrollments = enrollmentRepository.count();
        long activeStudents = enrollmentRepository.countActiveStudents(
                EnumSet.of(EnrollmentStatus.ENROLLED, EnrollmentStatus.IN_PROGRESS)
        );
        long completedCourses = enrollmentRepository.countByStatus(
                EnrollmentStatus.COMPLETED
        );

        // Recent registrations (latest 10 users)
        List<RecentRegistrationResponse> recentRegistrations =
                userRepository.findTop10ByOrderByCreatedDtDesc()
                        .stream()
                        .map(user -> RecentRegistrationResponse.builder()
                                .userId(user.getId())
                                .firstName(user.getFirstName())
                                .lastName(user.getLastName())
                                .email(user.getEmail())
                                .roles(user.getRoles().stream()
                                        .map(Role::getRoleName)
                                        .collect(Collectors.toSet()))
                                .status(user.getStatus().name())
                                .registeredAt(user.getCreatedDt())
                                .build())
                        .collect(Collectors.toList());

        // Recent enrollments (latest 10)
        List<RecentEnrollmentResponse> recentEnrollments =
                enrollmentRepository.findTop10ByOrderByEnrolledAtDesc()
                        .stream()
                        .map(enrollment -> {
                            User student = enrollment.getUser();
                            Course course = enrollment.getCourse();

                            return RecentEnrollmentResponse.builder()
                                    .enrollmentId(enrollment.getId())
                                    .courseId(course.getId())
                                    .courseCode(course.getCourseCode())
                                    .courseTitle(course.getTitle())
                                    .studentName(student.getFirstName() + " "
                                            + student.getLastName())
                                    .studentEmail(student.getEmail())
                                    .enrollmentStatus(enrollment.getStatus().name())
                                    .progressPercentage(enrollment.getProgressPercentage())
                                    .enrolledAt(enrollment.getEnrolledAt())
                                    .build();
                        })
                        .collect(Collectors.toList());

        return SuperAdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalStaff(totalStaff)
                .totalStudents(totalStudents)
                .totalSuperAdmins(totalSuperAdmins)
                .totalCourses(totalCourses)
                .publishedCourses(publishedCourses)
                .draftCourses(draftCourses)
                .totalEnrollments(totalEnrollments)
                .activeStudents(activeStudents)
                .completedCourses(completedCourses)
                .recentRegistrations(recentRegistrations)
                .recentEnrollments(recentEnrollments)
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
    public MessageResponse resendSetupLink(UUID userId) {

        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff", "userId", userId));

        User user = staff.getUser();

        if (Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException(
                    "Staff account is already active. No setup link needed."
            );
        }

        String setupToken = generateSetupToken();

        user.setSetupToken(setupToken);
        user.setSetupTokenExpiresAt(LocalDateTime.now().plusHours(24));

        userRepository.save(user);

        String setupLink = setupLinkBaseUrl + "?token=" + setupToken;

        emailService.sendStaffSetupEmail(
                user.getEmail(),
                user.getFirstName(),
                setupLink
        );

        return MessageResponse.builder()
                .message("A new setup link has been sent to " + user.getEmail())
                .build();
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

    @Override
    public MessageResponse resetStaffPassword(
            UUID userId, ResetPasswordByAdminRequest request
    ) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = findUserWithRole(userId, "STAFF", "Staff");

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return MessageResponse.builder()
                .message("Staff password reset successfully")
                .build();
    }

    // ------------------------------------------------------------------
    // Student management
    // ------------------------------------------------------------------

    @Override
    public List<StudentAdminResponse> getAllStudents(String search) {

        List<User> students = (search == null || search.isBlank())
                ? userRepository.findAllStudents()
                : userRepository.searchStudents(search.trim());

        return students.stream()
                .map(this::toStudentAdminResponse)
                .collect(Collectors.toList());
    }

    @Override
    public StudentAdminResponse getStudentDetail(UUID userId) {

        User user = findUserWithRole(userId, "STUDENT", "Student");

        return toStudentAdminResponse(user);
    }

    @Override
    public StudentProfileAdminResponse getStudentProfile(UUID userId) {

        User user = findUserWithRole(userId, "STUDENT", "Student");

        StudentProfile profile = studentProfileRepository
                .findByUserId(userId)
                .orElse(null);

        StudentProfileAdminResponse.StudentProfileAdminResponseBuilder builder =
                StudentProfileAdminResponse.builder()
                        .userId(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .firstName(user.getFirstName())
                        .lastName(user.getLastName())
                        .mobileNumber(user.getMobileNumber())
                        .enabled(user.getEnabled())
                        .status(user.getStatus().name())
                        .emailVerified(user.getEmailVerified())
                        .registeredAt(user.getCreatedDt());

        if (profile != null) {
            builder.studentCode(profile.getStudentCode())
                    .dateOfBirth(profile.getDateOfBirth())
                    .gender(profile.getGender() != null
                            ? profile.getGender().name() : null)
                    .qualification(profile.getQualification())
                    .occupation(profile.getOccupation())
                    .bio(profile.getBio())
                    .address(profile.getAddress())
                    .city(profile.getCity())
                    .state(profile.getState())
                    .country(profile.getCountry())
                    .joinedAt(profile.getJoinedAt());
        }

        return builder.build();
    }

    @Override
    public StudentAdminResponse updateStudentStatus(
            UUID userId, StudentStatusUpdateRequest request
    ) {

        User user = findUserWithRole(userId, "STUDENT", "Student");

        user.setEnabled(request.getEnabled());
        user.setStatus(Boolean.TRUE.equals(request.getEnabled())
                ? UserStatus.ACTIVE : UserStatus.INACTIVE);

        userRepository.save(user);

        return toStudentAdminResponse(user);
    }

    @Override
    public MessageResponse resetStudentPassword(
            UUID userId, ResetPasswordByAdminRequest request
    ) {

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = findUserWithRole(userId, "STUDENT", "Student");

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return MessageResponse.builder()
                .message("Student password reset successfully")
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private User findUserById(UUID userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "id", userId));
    }

    private User findUserWithRole(UUID userId, String roleName, String label) {

        User user = findUserById(userId);

        boolean hasRole = user.getRoles().stream()
                .anyMatch(role -> role.getRoleName().equals(roleName));

        if (!hasRole) {
            throw new IllegalArgumentException(
                    label + " account not found with id: " + userId
            );
        }

        return user;
    }

    private StudentAdminResponse toStudentAdminResponse(User user) {

        return StudentAdminResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .mobileNumber(user.getMobileNumber())
                .enabled(user.getEnabled())
                .status(user.getStatus().name())
                .emailVerified(user.getEmailVerified())
                .roles(user.getRoles().stream()
                        .map(Role::getRoleName)
                        .collect(Collectors.toSet()))
                .registeredAt(user.getCreatedDt())
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
