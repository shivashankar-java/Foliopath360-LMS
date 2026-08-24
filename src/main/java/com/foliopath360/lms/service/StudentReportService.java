package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.response.StudentAdminResponse;
import com.foliopath360.lms.dto.response.StudentPerformanceResponse;

import java.util.List;
import java.util.UUID;

/**
 * Read-only student reporting used by SUPER_ADMIN and STAFF dashboards.
 */
public interface StudentReportService {

    List<StudentAdminResponse> getAllStudents(String search);

    StudentPerformanceResponse getStudentPerformance(UUID userId);
}
