package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.response.StudentAdminResponse;
import com.foliopath360.lms.dto.response.StudentPerformanceResponse;
import com.foliopath360.lms.service.StudentReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Student reporting endpoints shared by SUPER_ADMIN and STAFF.
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
public class StudentReportController {

    private final StudentReportService studentReportService;

    @GetMapping("/students")
    public ResponseEntity<List<StudentAdminResponse>> getAllStudents(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(studentReportService.getAllStudents(search));
    }

    /**
     * Full student overview: account status, enrolled courses,
     * performance summary and mock test scores.
     */
    @GetMapping("/students/{userId}/performance")
    public ResponseEntity<StudentPerformanceResponse> getStudentPerformance(
            @PathVariable UUID userId
    ) {
        return ResponseEntity.ok(
                studentReportService.getStudentPerformance(userId)
        );
    }
}
