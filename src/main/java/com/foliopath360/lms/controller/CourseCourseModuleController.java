package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.CourseModuleLinkRequest;
import com.foliopath360.lms.dto.request.ReorderRequest;
import com.foliopath360.lms.dto.response.CourseModuleLinkResponse;
import com.foliopath360.lms.service.CourseCourseModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/{courseId}/course-modules")
@RequiredArgsConstructor
public class CourseCourseModuleController {

    private final CourseCourseModuleService courseCourseModuleService;

    @PostMapping
    public ResponseEntity<CourseModuleLinkResponse> addCourseAsModule(
            @PathVariable UUID courseId,
            @Valid @RequestBody CourseModuleLinkRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(courseCourseModuleService.addCourseAsModule(courseId, request));
    }

    @GetMapping
    public ResponseEntity<List<CourseModuleLinkResponse>> getCourseModules(
            @PathVariable UUID courseId
    ) {
        return ResponseEntity.ok(
                courseCourseModuleService.getCourseModules(courseId));
    }

    @GetMapping("/{courseModuleId}")
    public ResponseEntity<CourseModuleLinkResponse> getCourseModuleById(
            @PathVariable UUID courseId,
            @PathVariable UUID courseModuleId
    ) {
        return ResponseEntity.ok(
                courseCourseModuleService.getCourseModuleById(courseId, courseModuleId));
    }

    @DeleteMapping("/{courseModuleId}")
    public ResponseEntity<Void> removeCourseModule(
            @PathVariable UUID courseId,
            @PathVariable UUID courseModuleId
    ) {
        courseCourseModuleService.removeCourseModule(courseId, courseModuleId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    public ResponseEntity<List<CourseModuleLinkResponse>> reorderCourseModules(
            @PathVariable UUID courseId,
            @Valid @RequestBody ReorderRequest request
    ) {
        return ResponseEntity.ok(
                courseCourseModuleService.reorderCourseModules(courseId, request));
    }
}