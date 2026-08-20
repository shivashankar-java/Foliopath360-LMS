package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.ModuleRequest;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.ModuleResponse;
import com.foliopath360.lms.service.CourseModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/{courseId}/modules")
@RequiredArgsConstructor
public class CourseModuleController {

    private final CourseModuleService courseModuleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModuleResponse> createModule(
            @PathVariable UUID courseId,
            @Valid @RequestBody ModuleRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(courseModuleService.createModule(courseId, request));
    }

    @PutMapping("/{moduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModuleResponse> updateModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId,
            @Valid @RequestBody ModuleRequest request
    ) {
        return ResponseEntity.ok(
                courseModuleService.updateModule(moduleId, request)
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

    @DeleteMapping("/{moduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteModule(
            @PathVariable UUID courseId,
            @PathVariable UUID moduleId
    ) {
        courseModuleService.deleteModule(moduleId);
        return ResponseEntity.ok(
                MessageResponse.builder()
                        .message("Module deleted successfully")
                        .build()
        );
    }
}
