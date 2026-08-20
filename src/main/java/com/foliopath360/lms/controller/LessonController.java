package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.LessonRequest;
import com.foliopath360.lms.dto.response.LessonResponse;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/modules/{moduleId}/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LessonResponse> createLesson(
            @PathVariable UUID moduleId,
            @Valid @RequestBody LessonRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(lessonService.createLesson(moduleId, request));
    }

    @PutMapping("/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LessonResponse> updateLesson(
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody LessonRequest request
    ) {
        return ResponseEntity.ok(
                lessonService.updateLesson(lessonId, request)
        );
    }

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

    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> deleteLesson(
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId
    ) {
        lessonService.deleteLesson(lessonId);
        return ResponseEntity.ok(
                MessageResponse.builder()
                        .message("Lesson deleted successfully")
                        .build()
        );
    }
}
