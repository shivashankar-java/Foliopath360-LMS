package com.foliopath360.lms.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDashboardResponse {

    private long totalUsers;
    private long totalStudents;
    private long totalAdmins;
    private long totalCourses;
    private long activeCourses;
    private long totalEnrollments;
    private long activeEnrollments;
    private long completedEnrollments;
}
