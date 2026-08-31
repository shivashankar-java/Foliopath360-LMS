package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.AdminEnrollRequest;
import com.foliopath360.lms.dto.request.AdminKitEnrollRequest;
import com.foliopath360.lms.dto.request.AdminUnenrollRequest;
import com.foliopath360.lms.dto.response.EnrollmentResponse;
import com.foliopath360.lms.dto.response.InterviewKitEnrollmentResponse;
import com.foliopath360.lms.dto.response.StudentPaymentInfoResponse;
import com.foliopath360.lms.service.EnrollmentService;
import com.foliopath360.lms.service.InterviewKitService;
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
@RequiredArgsConstructor
public class AdminController {

    private final EnrollmentService enrollmentService;
    private final InterviewKitService interviewKitService;

    @PostMapping("/enrollments")
    public ResponseEntity<EnrollmentResponse> enrollStudent(
            @Valid @RequestBody AdminEnrollRequest request
    ) {
        boolean recordsPayment = request.getAmountPaid() != null
                || request.getDiscountAmount() != null
                || request.getPaymentMethod() != null;

        EnrollmentResponse response;
        if (recordsPayment) {
            response = enrollmentService.adminEnrollStudentWithPayment(
                    request.getStudentId(),
                    request.getCourseId(),
                    request.getDiscountAmount(),
                    request.getAmountPaid(),
                    request.getPaymentMethod()
            );
        } else {
            response = enrollmentService.adminEnrollStudent(
                    request.getStudentId(),
                    request.getCourseId()
            );
        }

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/enrollments/unenroll")
    public ResponseEntity<EnrollmentResponse> unenrollStudent(
            @Valid @RequestBody AdminUnenrollRequest request
    ) {
        return ResponseEntity.ok(
                enrollmentService.adminUnenrollStudentWithRefund(
                        request.getStudentId(),
                        request.getCourseId(),
                        request.getRefundAmount()
                )
        );
    }

    @GetMapping("/students/{id}/payments")
    public ResponseEntity<List<StudentPaymentInfoResponse>> getStudentPayments(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(enrollmentService.getStudentPaymentInfo(id));
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
