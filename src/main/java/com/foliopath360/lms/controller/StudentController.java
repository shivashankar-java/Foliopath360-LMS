package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.response.CourseResponse;
import com.foliopath360.lms.dto.response.StudentDashboardResponse;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/courses")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<CourseResponse>> getAvailableCourses() {
        return ResponseEntity.ok(studentService.getAvailableCourses());
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentDashboardResponse> getDashboard(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(studentService.getDashboard(user));
    }
}
