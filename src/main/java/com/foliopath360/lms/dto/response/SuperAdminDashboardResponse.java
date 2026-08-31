package com.foliopath360.lms.dto.response;

import lombok.*;

import java.math.BigDecimal;
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

    // Monthly enrollments for chart
    private List<MonthlyEnrollmentResponse> monthlyEnrollments;

    // Recent activity
    private List<RecentRegistrationResponse> recentRegistrations;
    private List<RecentEnrollmentResponse> recentEnrollments;

    // Revenue / payment reconciliation
    private BigDecimal totalRevenue;
    private List<MonthlyRevenueResponse> monthlyRevenue;
    private List<PaymentTransactionResponse> transactions;
}
