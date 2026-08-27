package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.AdminEnrollRequest;
import com.foliopath360.lms.dto.request.AdminKitEnrollRequest;
import com.foliopath360.lms.dto.response.EnrollmentResponse;
import com.foliopath360.lms.dto.response.InterviewKitEnrollmentResponse;
import com.foliopath360.lms.service.EnrollmentService;
import com.foliopath360.lms.service.InterviewKitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/super-admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final EnrollmentService enrollmentService;
    private final InterviewKitService interviewKitService;

    @PostMapping("/enrollments")
    public ResponseEntity<EnrollmentResponse> enrollStudent(
            @Valid @RequestBody AdminEnrollRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(enrollmentService.adminEnrollStudent(
                        request.getStudentId(),
                        request.getCourseId()
                ));
    }

    @PostMapping("/kit-enrollments")
    public ResponseEntity<InterviewKitEnrollmentResponse> enrollStudentInKit(
            @Valid @RequestBody AdminKitEnrollRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(interviewKitService.adminEnrollStudent(
                        request.getStudentId(),
                        request.getKitId()
                ));
    }
}
