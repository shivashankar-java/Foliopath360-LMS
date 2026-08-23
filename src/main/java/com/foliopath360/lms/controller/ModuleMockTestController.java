package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.MockTestRequest;
import com.foliopath360.lms.dto.response.MockTestResponse;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.service.MockTestService;
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
@RequestMapping("/api/modules/{moduleId}/mock-tests")
@RequiredArgsConstructor
public class ModuleMockTestController {

    private final MockTestService mockTestService;

    /**
     * Public listing of a module's mock tests.
     * Correct answers are included only for SUPER_ADMIN / STAFF requests.
     */
    @GetMapping
    public ResponseEntity<List<MockTestResponse>> getMockTestsByModule(
            @PathVariable UUID moduleId,
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(
                mockTestService.getMockTestsByModule(moduleId, requester)
        );
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'STAFF')")
    public ResponseEntity<MockTestResponse> createMockTest(
            @PathVariable UUID moduleId,
            @Valid @RequestBody MockTestRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(mockTestService.createMockTest(moduleId, request));
    }
}
