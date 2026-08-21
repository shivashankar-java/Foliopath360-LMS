package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.response.CourseProgressResponse;
import com.foliopath360.lms.dto.response.EnrollmentResponse;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.StudentEnrolledCourseResponse;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // ---------------- Enrollment ----------------

    @PostMapping("/enrollments/{courseId}")
    public ResponseEntity<EnrollmentResponse> enroll(
            @AuthenticationPrincipal User student,
            @PathVariable UUID courseId
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(enrollmentService.enroll(student, courseId));
    }

    @GetMapping("/enrollments")
    public ResponseEntity<List<StudentEnrolledCourseResponse>> getMyEnrollments(
            @AuthenticationPrincipal User student
    ) {
        return ResponseEntity.ok(
                enrollmentService.getMyEnrollments(student)
        );
    }

    @GetMapping("/enrollments/{courseId}")
    public ResponseEntity<EnrollmentResponse> getEnrollment(
            @AuthenticationPrincipal User student,
            @PathVariable UUID courseId
    ) {
        return ResponseEntity.ok(
                enrollmentService.getEnrollment(student, courseId)
        );
    }

    @DeleteMapping("/enrollments/{courseId}")
    public ResponseEntity<EnrollmentResponse> dropEnrollment(
            @AuthenticationPrincipal User student,
            @PathVariable UUID courseId
    ) {
        return ResponseEntity.ok(
                enrollmentService.dropEnrollment(student, courseId)
        );
    }

    // ---------------- Lesson progress ----------------

    @PostMapping("/progress/lessons/{lessonId}/complete")
    public ResponseEntity<CourseProgressResponse> markLessonCompleted(
            @AuthenticationPrincipal User student,
            @PathVariable UUID lessonId
    ) {
        return ResponseEntity.ok(
                enrollmentService.markLessonCompleted(student, lessonId)
        );
    }

    @DeleteMapping("/progress/lessons/{lessonId}/complete")
    public ResponseEntity<CourseProgressResponse> markLessonIncomplete(
            @AuthenticationPrincipal User student,
            @PathVariable UUID lessonId
    ) {
        return ResponseEntity.ok(
                enrollmentService.markLessonIncomplete(student, lessonId)
        );
    }

    @GetMapping("/progress/courses/{courseId}")
    public ResponseEntity<CourseProgressResponse> getCourseProgress(
            @AuthenticationPrincipal User student,
            @PathVariable UUID courseId
    ) {
        return ResponseEntity.ok(
                enrollmentService.getCourseProgress(student, courseId)
        );
    }
}
