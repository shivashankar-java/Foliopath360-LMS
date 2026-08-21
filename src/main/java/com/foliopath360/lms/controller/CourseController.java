package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.CourseRequest;
import com.foliopath360.lms.dto.response.CourseResponse;
import com.foliopath360.lms.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(
            @Valid @RequestBody CourseRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(courseService.createCourse(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable UUID id,
            @Valid @RequestBody CourseRequest request
    ) {
        return ResponseEntity.ok(
                courseService.updateCourse(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable UUID id
    ) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<CourseResponse> publishCourse(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(courseService.publishCourse(id));
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAllCourses() {
        return ResponseEntity.ok(courseService.getAllCourses());
    }

    @GetMapping("/published")
    public ResponseEntity<List<CourseResponse>> getPublishedCourses() {
        return ResponseEntity.ok(courseService.getPublishedCourses());
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
}
