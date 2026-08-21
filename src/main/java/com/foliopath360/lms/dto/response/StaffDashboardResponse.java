package com.foliopath360.lms.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffDashboardResponse {

    private String staffCode;
    private String designation;
    private String department;
    private long totalStudents;
}
