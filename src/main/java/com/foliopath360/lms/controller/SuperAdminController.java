package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.StaffCreateRequest;
import com.foliopath360.lms.dto.request.StaffStatusUpdateRequest;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.StaffResponse;
import com.foliopath360.lms.dto.response.SuperAdminDashboardResponse;
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

    public SuperAdminController(SuperAdminService superAdminService) {
        this.superAdminService = superAdminService;
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

    @DeleteMapping("/staff/{id}")
    public ResponseEntity<MessageResponse> deleteStaff(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(superAdminService.deleteStaff(id));
    }
}
