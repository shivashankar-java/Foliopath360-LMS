package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.response.CourseResponse;
import com.foliopath360.lms.dto.response.StudentDashboardResponse;
import com.foliopath360.lms.dto.response.StudentEnrolledCourseResponse;
import com.foliopath360.lms.dto.response.UserResponse;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.mapper.CourseMapper;
import com.foliopath360.lms.repository.CourseRepository;
import com.foliopath360.lms.repository.EnrollmentRepository;
import com.foliopath360.lms.service.StudentService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentServiceImpl implements StudentService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseMapper courseMapper;

    public StudentServiceImpl(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            CourseMapper courseMapper
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.courseMapper = courseMapper;
    }

    @Override
    @Transactional
    public List<CourseResponse> getAvailableCourses() {

        return courseRepository.findByStatus(CourseStatus.PUBLISHED)
                .stream()
                .map(courseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StudentDashboardResponse getDashboard(User user) {

        List<CourseResponse> availableCourses = getAvailableCourses();

        List<Enrollment> enrollments =
                enrollmentRepository.findByUserId(user.getId());

        List<StudentEnrolledCourseResponse> enrolledCourses =
                enrollments.stream()
                        .map(this::mapToEnrolledCourseResponse)
                        .collect(Collectors.toList());

        long completedCourses = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();

        long inProgressCourses = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.IN_PROGRESS)
                .count();

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(
                        user.getRoles().stream()
                                .map(Role::getRoleName)
                                .collect(Collectors.toSet())
                )
                .build();

        return StudentDashboardResponse.builder()
                .studentInfo(userResponse)
                .totalEnrolledCourses(enrollments.size())
                .completedCourses(completedCourses)
                .inProgressCourses(inProgressCourses)
                .availableCourses(availableCourses)
                .enrolledCourses(enrolledCourses)
                .build();
    }

    private StudentEnrolledCourseResponse mapToEnrolledCourseResponse(
            Enrollment enrollment
    ) {
        Course course = enrollment.getCourse();

        return StudentEnrolledCourseResponse.builder()
                .courseId(course.getId())
                .courseCode(course.getCourseCode())
                .title(course.getTitle())
                .slug(course.getSlug())
                .shortDescription(course.getShortDescription())
                .thumbnailUrl(course.getThumbnailUrl())
                .level(course.getLevel() != null
                        ? course.getLevel().name() : null)
                .enrollmentStatus(enrollment.getStatus().name())
                .progressPercentage(enrollment.getProgressPercentage())
                .enrolledAt(enrollment.getEnrolledAt())
                .completedAt(enrollment.getCompletedAt())
                .build();
    }
}
