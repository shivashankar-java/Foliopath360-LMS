package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.response.LessonResponse;
import com.foliopath360.lms.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/modules/{moduleId}/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @GetMapping("/{lessonId}")
    public ResponseEntity<LessonResponse> getLessonById(
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId
    ) {
        return ResponseEntity.ok(
                lessonService.getLessonById(lessonId)
        );
    }

    @GetMapping
    public ResponseEntity<List<LessonResponse>> getLessonsByModuleId(
            @PathVariable UUID moduleId
    ) {
        return ResponseEntity.ok(
                lessonService.getLessonsByModuleId(moduleId)
        );
    }
}
