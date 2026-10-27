package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.response.CourseProgressResponse;
import com.foliopath360.lms.dto.response.EnrollmentResponse;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.ModuleProgressResponse;
import com.foliopath360.lms.dto.response.StudentEnrolledCourseResponse;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.service.EnrollmentService;
import com.foliopath360.lms.service.ModuleProgressService;
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
    private final ModuleProgressService moduleProgressService;

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

    // ---------------- Sequential module progress ----------------
    //
    // Module status is authoritative server-side and driven only by passed
    // mock tests:
    //   Enrollment -> Module 1 OPEN -> pass Mock Test 1 -> Module 1 COMPLETED
    //   -> Module 2 OPEN -> ... -> final test passed -> 100%.

    /**
     * Sequential progress for one course: every module's status
     * (LOCKED / OPEN / COMPLETED), the current module, and
     * {@code progressPercentage = completed modules / total modules * 100}.
     */
    @GetMapping("/progress/courses/{courseId}/modules")
    public ResponseEntity<ModuleProgressResponse> getCourseModuleProgress(
            @AuthenticationPrincipal User student,
            @PathVariable UUID courseId
    ) {
        return ResponseEntity.ok(
                moduleProgressService.getCourseProgress(student, courseId)
        );
    }

    /**
     * Same payload, resolved from a module id (the containing course drives
     * the sequence).
     */
    @GetMapping("/progress/modules/{moduleId}")
    public ResponseEntity<ModuleProgressResponse> getModuleProgress(
            @AuthenticationPrincipal User student,
            @PathVariable UUID moduleId
    ) {
        return ResponseEntity.ok(
                moduleProgressService.getModuleProgress(student, moduleId)
        );
    }

    /**
     * Sequential progress across every course the student is enrolled in.
     */
    @GetMapping("/progress/modules")
    public ResponseEntity<List<ModuleProgressResponse>> getMyModuleProgress(
            @AuthenticationPrincipal User student
    ) {
        return ResponseEntity.ok(
                moduleProgressService.getMyCourseProgress(student)
        );
    }
}
