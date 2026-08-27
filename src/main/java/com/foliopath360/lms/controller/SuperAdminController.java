package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.ResetPasswordByAdminRequest;
import com.foliopath360.lms.dto.request.StaffCreateRequest;
import com.foliopath360.lms.dto.request.StaffStatusUpdateRequest;
import com.foliopath360.lms.dto.request.StudentStatusUpdateRequest;
import com.foliopath360.lms.dto.response.EnrollmentResponse;
import com.foliopath360.lms.dto.response.InterviewKitEnrollmentResponse;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.StaffResponse;
import com.foliopath360.lms.dto.response.StudentAdminResponse;
import com.foliopath360.lms.dto.response.StudentProfileAdminResponse;
import com.foliopath360.lms.dto.response.SuperAdminDashboardResponse;
import com.foliopath360.lms.service.EnrollmentService;
import com.foliopath360.lms.service.InterviewKitService;
import com.foliopath360.lms.service.SuperAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/super-admin")

@PreAuthorize("hasRole('SUPER_ADMIN')")
public class SuperAdminController {

    private final SuperAdminService superAdminService;
    private final EnrollmentService enrollmentService;
    private final InterviewKitService interviewKitService;

    public SuperAdminController(SuperAdminService superAdminService, EnrollmentService enrollmentService, InterviewKitService interviewKitService) {
        this.superAdminService = superAdminService;
        this.enrollmentService = enrollmentService;
        this.interviewKitService = interviewKitService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<SuperAdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(superAdminService.getDashboard());
    }

    @PostMapping("/staff")
    public ResponseEntity<StaffResponse> createStaff(
            @Valid @RequestBody StaffCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(superAdminService.createStaff(request));
    }

    @GetMapping("/staff")
    public ResponseEntity<List<StaffResponse>> getAllStaff() {
        return ResponseEntity.ok(superAdminService.getAllStaff());
    }

    @GetMapping("/staff/{id}")
    public ResponseEntity<StaffResponse> getStaffById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(superAdminService.getStaffById(id));
    }

    @PutMapping("/staff/{id}")
    public ResponseEntity<StaffResponse> updateStaff(
            @PathVariable UUID id,
            @Valid @RequestBody StaffCreateRequest request
    ) {
        return ResponseEntity.ok(superAdminService.updateStaff(id, request));
    }

    @PatchMapping("/staff/{id}/status")
    public ResponseEntity<StaffResponse> updateStaffStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StaffStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(superAdminService.updateStaffStatus(id, request));
    }

    @PostMapping("/staff/{id}/resend-setup-link")
    public ResponseEntity<MessageResponse> resendSetupLink(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(superAdminService.resendSetupLink(id));
    }

    @PatchMapping("/staff/{id}/reset-password")
    public ResponseEntity<MessageResponse> resetStaffPassword(
            @PathVariable UUID id,
            @Valid @RequestBody ResetPasswordByAdminRequest request
    ) {
        return ResponseEntity.ok(
                superAdminService.resetStaffPassword(id, request)
        );
    }

    // ---------------- Student management ----------------

    @GetMapping("/students")
    public ResponseEntity<List<StudentAdminResponse>> getAllStudents(
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(superAdminService.getAllStudents(search));
    }

    @GetMapping("/students/{id}")
    public ResponseEntity<StudentAdminResponse> getStudentDetail(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(superAdminService.getStudentDetail(id));
    }

    @GetMapping("/students/{id}/profile")
    public ResponseEntity<StudentProfileAdminResponse> getStudentProfile(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(superAdminService.getStudentProfile(id));
    }

    @PatchMapping("/students/{id}/status")
    public ResponseEntity<StudentAdminResponse> updateStudentStatus(
            @PathVariable UUID id,
            @Valid @RequestBody StudentStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(
                superAdminService.updateStudentStatus(id, request)
        );
    }

    @PatchMapping("/students/{id}/reset-password")
    public ResponseEntity<MessageResponse> resetStudentPassword(
            @PathVariable UUID id,
            @Valid @RequestBody ResetPasswordByAdminRequest request
    ) {
        return ResponseEntity.ok(
                superAdminService.resetStudentPassword(id, request)
        );
    }

    @GetMapping("/students/{id}/enrollments")
    public ResponseEntity<List<EnrollmentResponse>> getStudentEnrollments(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(enrollmentService.getEnrollmentsByUserId(id));
    }

    @GetMapping("/students/{id}/kit-enrollments")
    public ResponseEntity<List<InterviewKitEnrollmentResponse>> getStudentKitEnrollments(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(interviewKitService.getStudentKitEnrollments(id));
    }

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<MessageResponse> deleteStaff(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(superAdminService.deleteStaff(id));
    }
}
