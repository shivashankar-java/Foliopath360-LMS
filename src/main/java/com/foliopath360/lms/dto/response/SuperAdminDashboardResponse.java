package com.foliopath360.lms.dto.response;

import lombok.*;

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
}
