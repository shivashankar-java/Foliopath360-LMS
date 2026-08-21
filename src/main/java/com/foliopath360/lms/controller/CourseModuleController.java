package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.response.ModuleResponse;
import com.foliopath360.lms.service.CourseModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/{courseId}/modules")
@RequiredArgsConstructor
public class CourseModuleController {

    private final CourseModuleService courseModuleService;

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
