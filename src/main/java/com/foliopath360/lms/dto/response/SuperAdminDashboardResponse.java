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

    // Interview Kits
    private long totalKits;
    private long publishedKits;
    private long draftKits;

    // Enrollments
    private long totalEnrollments;
    private long activeStudents;
    private long completedCourses;

    // Kit enrollments
    private long totalKitEnrollments;
    private long thisMonthKitEnrollments;
    private long thisMonthCourseEnrollments;

    // Monthly enrollments for chart
    private List<MonthlyEnrollmentResponse> monthlyEnrollments;
    private List<MonthlyEnrollmentResponse> monthlyCourseEnrollments;
    private List<MonthlyEnrollmentResponse> monthlyKitEnrollments;

    // Recent activity
    private List<RecentRegistrationResponse> recentRegistrations;
    private List<RecentEnrollmentResponse> recentEnrollments;

    // Revenue / payment reconciliation
    private BigDecimal totalRevenue;
    private BigDecimal kitRevenue;
    private List<MonthlyRevenueResponse> monthlyRevenue;
    private List<MonthlyRevenueResponse> monthlyKitRevenue;
    private List<PaymentTransactionResponse> transactions;
    private List<PaymentTransactionResponse> kitTransactions;
}
