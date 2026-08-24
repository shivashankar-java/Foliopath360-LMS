package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.response.StudentAdminResponse;
import com.foliopath360.lms.dto.response.StudentEnrolledCourseResponse;
import com.foliopath360.lms.dto.response.StudentMockTestScoreResponse;
import com.foliopath360.lms.dto.response.StudentPerformanceResponse;
import com.foliopath360.lms.entity.Enrollment;
import com.foliopath360.lms.entity.EnrollmentStatus;
import com.foliopath360.lms.entity.MockTestAttempt;
import com.foliopath360.lms.entity.Role;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.mapper.EnrollmentMapper;
import com.foliopath360.lms.repository.EnrollmentRepository;
import com.foliopath360.lms.repository.MockTestAttemptRepository;
import com.foliopath360.lms.repository.StudentProfileRepository;
import com.foliopath360.lms.repository.UserRepository;
import com.foliopath360.lms.service.StudentReportService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StudentReportServiceImpl implements StudentReportService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final MockTestAttemptRepository mockTestAttemptRepository;
    private final EnrollmentMapper enrollmentMapper;

    @Override
    @Transactional
    public List<StudentAdminResponse> getAllStudents(String search) {

        List<User> students = (search == null || search.isBlank())
                ? userRepository.findAllStudents()
                : userRepository.searchStudents(search.trim());

        return students.stream()
                .map(this::toStudentAdminResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudentPerformanceResponse getStudentPerformance(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User", "id", userId));

        boolean isStudent = user.getRoles().stream()
                .anyMatch(role -> role.getRoleName().equals("STUDENT"));
        if (!isStudent) {
            throw new IllegalArgumentException(
                    "Student account not found with id: " + userId);
        }

        String studentCode = studentProfileRepository.findByUserId(userId)
                .map(profile -> profile.getStudentCode())
                .orElse(null);

        List<Enrollment> enrollments =
                enrollmentRepository.findByUserId(userId);

        List<StudentEnrolledCourseResponse> enrolledCourses =
                enrollments.stream()
                        .filter(e -> e.getStatus() != EnrollmentStatus.DROPPED)
                        .map(enrollmentMapper::toEnrolledCourseResponse)
                        .collect(Collectors.toList());

        long completedCourses = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();

        long inProgressCourses = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.IN_PROGRESS)
                .count();

        double averageProgress = enrollments.isEmpty() ? 0.0 :
                enrollments.stream()
                        .filter(e -> e.getStatus() != EnrollmentStatus.DROPPED)
                        .mapToInt(Enrollment::getProgressPercentage)
                        .average()
                        .orElse(0.0);

        List<MockTestAttempt> attempts =
                mockTestAttemptRepository
                        .findByUserIdOrderBySubmittedAtDesc(userId);

        long passedAttempts = attempts.stream()
                .filter(a -> Boolean.TRUE.equals(a.getPassed()))
                .count();

        List<StudentMockTestScoreResponse> scores = attempts.stream()
                .map(this::toScoreResponse)
                .collect(Collectors.toList());

        return StudentPerformanceResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .mobileNumber(user.getMobileNumber())
                .enabled(user.getEnabled())
                .status(user.getStatus().name())
                .studentCode(studentCode)
                .registeredAt(user.getCreatedDt())
                .totalEnrolledCourses((long) enrolledCourses.size())
                .completedCourses(completedCourses)
                .inProgressCourses(inProgressCourses)
                .averageProgressPercentage(Math.round(averageProgress * 10.0) / 10.0)
                .totalMockTestsTaken((long) attempts.size())
                .mockTestsPassed(passedAttempts)
                .enrolledCourses(enrolledCourses)
                .mockTestScores(scores)
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private StudentMockTestScoreResponse toScoreResponse(MockTestAttempt attempt) {

        var mockTest = attempt.getMockTest();
        var module = mockTest != null ? mockTest.getModule() : null;
        var course = module != null ? module.getCourse() : null;

        return StudentMockTestScoreResponse.builder()
                .attemptId(attempt.getId())
                .mockTestId(mockTest != null ? mockTest.getId() : null)
                .testTitle(mockTest != null ? mockTest.getTitle() : null)
                .moduleName(module != null ? module.getTitle() : null)
                .courseTitle(course != null ? course.getTitle() : null)
                .score(attempt.getScore())
                .totalQuestions(attempt.getTotalQuestions())
                .percentage(attempt.getPercentage())
                .passed(attempt.getPassed())
                .submittedAt(attempt.getSubmittedAt())
                .build();
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
}
