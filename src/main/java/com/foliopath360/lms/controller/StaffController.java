package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.response.StaffDashboardResponse;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.service.StaffLmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STAFF')")
public class StaffController {

    private final StaffLmsService staffLmsService;

    @GetMapping("/dashboard")
    public ResponseEntity<StaffDashboardResponse> getDashboard(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(staffLmsService.getDashboard(user));
    }
}
