package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.ModuleRequest;
import com.foliopath360.lms.dto.request.ReorderRequest;
import com.foliopath360.lms.dto.response.ModuleResponse;
import com.foliopath360.lms.service.CourseModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/{courseId}/modules")
@RequiredArgsConstructor
public class CourseModuleController {

    private final CourseModuleService courseModuleService;

    @PostMapping
    public ResponseEntity<ModuleResponse> createModule(
            @PathVariable UUID courseId,
            @Valid @RequestBody ModuleRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(courseModuleService.createModule(courseId, request));
    }

    @PutMapping("/{moduleId}")
    public ResponseEntity<ModuleResponse> updateModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @Valid @RequestBody ModuleRequest request
    ) {
        return ResponseEntity.ok(
                courseModuleService.updateModule(moduleId, request)
        );
    }

    @DeleteMapping("/{moduleId}")
    public ResponseEntity<Void> deleteModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId
    ) {
        courseModuleService.deleteModule(moduleId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    public ResponseEntity<List<ModuleResponse>> reorderModules(
            @PathVariable UUID courseId,
            @Valid @RequestBody ReorderRequest request
    ) {
        return ResponseEntity.ok(
                courseModuleService.reorderModules(courseId, request)
        );
    }

    @GetMapping("/{moduleId}")
    public ResponseEntity<ModuleResponse> getModuleById(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId
    ) {
        return ResponseEntity.ok(
                courseModuleService.getModuleById(moduleId)
        );
    }

    @GetMapping
    public ResponseEntity<List<ModuleResponse>> getModulesByCourseId(
            @PathVariable UUID courseId
    ) {
        return ResponseEntity.ok(
                courseModuleService.getModulesByCourseId(courseId)
        );
    }
}
