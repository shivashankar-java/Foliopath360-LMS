package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.InterviewKitQuestionRequest;
import com.foliopath360.lms.dto.request.InterviewKitRequest;
import com.foliopath360.lms.dto.response.InterviewKitEnrollmentResponse;
import com.foliopath360.lms.dto.response.InterviewKitQuestionResponse;
import com.foliopath360.lms.dto.response.InterviewKitResponse;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.StudentEnrolledKitResponse;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.service.InterviewKitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/interview-kits")
@RequiredArgsConstructor
public class InterviewKitController {

    private final InterviewKitService interviewKitService;

    // ── Admin CRUD ──────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<InterviewKitResponse> createKit(
            @Valid @RequestBody InterviewKitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewKitService.createKit(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<InterviewKitResponse> updateKit(
            @PathVariable UUID id,
            @Valid @RequestBody InterviewKitRequest request) {
        return ResponseEntity.ok(interviewKitService.updateKit(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<Void> deleteKit(@PathVariable UUID id) {
        interviewKitService.deleteKit(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<InterviewKitResponse> publishKit(@PathVariable UUID id) {
        return ResponseEntity.ok(interviewKitService.publishKit(id));
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<InterviewKitResponse> unpublishKit(@PathVariable UUID id) {
        return ResponseEntity.ok(interviewKitService.unpublishKit(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<List<InterviewKitResponse>> getAllKits() {
        return ResponseEntity.ok(interviewKitService.getAllKits());
    }

    @GetMapping("/published")
    public ResponseEntity<List<InterviewKitResponse>> getPublishedKits() {
        return ResponseEntity.ok(interviewKitService.getPublishedKits());
    }

    @GetMapping("/{id}")
    public ResponseEntity<InterviewKitResponse> getKitById(@PathVariable UUID id) {
        return ResponseEntity.ok(interviewKitService.getKitById(id));
    }

    // ── Admin Question Management ───────────────────────────────────

    @GetMapping("/{id}/questions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<List<InterviewKitQuestionResponse>> getQuestions(
            @PathVariable UUID id) {
        return ResponseEntity.ok(interviewKitService.getQuestions(id));
    }

    @PostMapping("/{id}/questions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<InterviewKitQuestionResponse> addQuestion(
            @PathVariable UUID id,
            @Valid @RequestBody InterviewKitQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewKitService.addQuestion(id, request));
    }

    @PutMapping("/{id}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<InterviewKitQuestionResponse> updateQuestion(
            @PathVariable UUID id,
            @PathVariable UUID questionId,
            @Valid @RequestBody InterviewKitQuestionRequest request) {
        return ResponseEntity.ok(interviewKitService.updateQuestion(id, questionId, request));
    }

    @DeleteMapping("/{id}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable UUID id,
            @PathVariable UUID questionId) {
        interviewKitService.deleteQuestion(id, questionId);
        return ResponseEntity.noContent().build();
    }

    // ── Student ─────────────────────────────────────────────────────

    @GetMapping("/student/enrolled")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<StudentEnrolledKitResponse>> getMyEnrolledKits(
            @AuthenticationPrincipal User student) {
        return ResponseEntity.ok(interviewKitService.getMyEnrolledKits(student));
    }

    @GetMapping("/student/enrollment/{kitId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<InterviewKitEnrollmentResponse> getMyKitEnrollment(
            @AuthenticationPrincipal User student,
            @PathVariable UUID kitId) {
        return ResponseEntity.ok(interviewKitService.getMyKitEnrollment(student, kitId));
    }

    @PostMapping("/student/enroll/{kitId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<InterviewKitEnrollmentResponse> enrollInKit(
            @AuthenticationPrincipal User student,
            @PathVariable UUID kitId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(interviewKitService.enrollInKit(student, kitId));
    }

    @DeleteMapping("/student/enroll/{kitId}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<InterviewKitEnrollmentResponse> dropKitEnrollment(
            @AuthenticationPrincipal User student,
            @PathVariable UUID kitId) {
        return ResponseEntity.ok(interviewKitService.dropKitEnrollment(student, kitId));
    }
}
