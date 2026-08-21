package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SuperAdminDashboardResponse {

    private long totalUsers;
    private long totalStaff;
    private long totalStudents;
    private long totalSuperAdmins;

    // Courses
    private long totalCourses;
    private long publishedCourses;
    private long draftCourses;

    // Enrollments
    private long totalEnrollments;
    private long activeStudents;
    private long completedCourses;

    // Recent activity
    private List<RecentRegistrationResponse> recentRegistrations;
    private List<RecentEnrollmentResponse> recentEnrollments;
}
