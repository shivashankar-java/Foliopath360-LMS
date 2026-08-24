package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.MockTestAttemptRequest;
import com.foliopath360.lms.dto.request.MockTestRequest;
import com.foliopath360.lms.dto.response.MockTestAttemptResponse;
import com.foliopath360.lms.dto.response.MockTestResponse;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.service.MockTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/mock-tests")
@RequiredArgsConstructor
public class MockTestController {

    private final MockTestService mockTestService;

    /**
     * Test-taking view. Correct answers are never exposed here
     * unless the caller is SUPER_ADMIN / STAFF.
     */
    @GetMapping("/{mockTestId}")
    public ResponseEntity<MockTestResponse> getMockTestById(
            @PathVariable UUID mockTestId,
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(
                mockTestService.getMockTestById(mockTestId, requester)
        );
    }

    @PutMapping("/{mockTestId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<MockTestResponse> updateMockTest(
            @PathVariable UUID mockTestId,
            @Valid @RequestBody MockTestRequest request
    ) {
        return ResponseEntity.ok(
                mockTestService.updateMockTest(mockTestId, request)
        );
    }

    @DeleteMapping("/{mockTestId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<Void> deleteMockTest(@PathVariable UUID mockTestId) {
        mockTestService.deleteMockTest(mockTestId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Submit a student attempt. Grading happens server-side;
     * response matches the frontend contract:
     * { attemptId, score, total, percentage, passed }.
     * Each student may attempt a mock test only once (409 on retry).
     */
    @PostMapping("/{mockTestId}/attempts")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<MockTestAttemptResponse> submitAttempt(
            @AuthenticationPrincipal User student,
            @PathVariable UUID mockTestId,
            @RequestBody MockTestAttemptRequest request
    ) {
        return ResponseEntity.ok(
                mockTestService.submitAttempt(student, mockTestId, request)
        );
    }

    /**
     * The current student's existing attempt for this test (404 if none yet).
     */
    @GetMapping("/{mockTestId}/my-attempt")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<MockTestAttemptResponse> getMyAttempt(
            @AuthenticationPrincipal User student,
            @PathVariable UUID mockTestId
    ) {
        return mockTestService.getMyAttempt(student, mockTestId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
