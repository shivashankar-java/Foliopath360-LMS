package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.response.AdminDashboardResponse;
import com.foliopath360.lms.entity.CourseStatus;
import com.foliopath360.lms.entity.EnrollmentStatus;
import com.foliopath360.lms.repository.CourseRepository;
import com.foliopath360.lms.repository.EnrollmentRepository;
import com.foliopath360.lms.repository.UserRepository;
import com.foliopath360.lms.service.AdminService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AdminServiceImpl(
            UserRepository userRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository
    ) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Override
    @Transactional
    public AdminDashboardResponse getDashboardStats() {

        long totalUsers = userRepository.count();
        long totalStudents = userRepository.countByRolesRoleName("STUDENT");
        long totalAdmins = userRepository.countByRolesRoleName("ADMIN");
        long totalCourses = courseRepository.count();
        long activeCourses =
                courseRepository.findByStatus(CourseStatus.PUBLISHED).size();
        long totalEnrollments = enrollmentRepository.count();
        long activeEnrollments =
                enrollmentRepository.countByStatus(EnrollmentStatus.ENROLLED)
                        + enrollmentRepository.countByStatus(
                                EnrollmentStatus.IN_PROGRESS
                        );
        long completedEnrollments =
                enrollmentRepository.countByStatus(EnrollmentStatus.COMPLETED);

        return AdminDashboardResponse.builder()
                .totalUsers(totalUsers)
                .totalStudents(totalStudents)
                .totalAdmins(totalAdmins)
                .totalCourses(totalCourses)
                .activeCourses(activeCourses)
                .totalEnrollments(totalEnrollments)
                .activeEnrollments(activeEnrollments)
                .completedEnrollments(completedEnrollments)
                .build();
    }
}
