package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.CourseRequest;
import com.foliopath360.lms.dto.response.CourseResponse;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> createCourse(
            @Valid @RequestBody CourseRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(courseService.createCourse(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable UUID id,
            @Valid @RequestBody CourseRequest request
    ) {
        return ResponseEntity.ok(courseService.updateCourse(id, request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getCourseById(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(courseService.getCourseById(id));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<CourseResponse> getCourseBySlug(
            @PathVariable String slug
    ) {
        return ResponseEntity.ok(courseService.getCourseBySlug(slug));
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/published")
    public ResponseEntity<List<CourseResponse>> getPublishedCourses() {
        return ResponseEntity.ok(courseService.getPublishedCourses());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteCourse(
            @PathVariable UUID id
    ) {
        courseService.deleteCourse(id);
        return ResponseEntity.ok(
                MessageResponse.builder()
                        .message("Course deleted successfully")
                        .build()
        );
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CourseResponse> publishCourse(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(courseService.publishCourse(id));
    }
}
