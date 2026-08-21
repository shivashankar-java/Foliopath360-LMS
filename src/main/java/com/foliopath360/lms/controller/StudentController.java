package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.StudentProfileUpdateRequest;
import com.foliopath360.lms.dto.response.StudentDashboardResponse;
import com.foliopath360.lms.dto.response.StudentProfileResponse;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/dashboard")
    public ResponseEntity<StudentDashboardResponse> getDashboard(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(studentService.getDashboard(user));
    }

    @GetMapping("/profile")
    public ResponseEntity<StudentProfileResponse> getProfile(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(studentService.getProfile(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<StudentProfileResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody StudentProfileUpdateRequest request
    ) {
        return ResponseEntity.ok(studentService.updateProfile(user, request));
    }
}
