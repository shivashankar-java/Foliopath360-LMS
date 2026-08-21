package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.LessonItemRequest;
import com.foliopath360.lms.dto.request.LessonRequest;
import com.foliopath360.lms.dto.request.ReorderRequest;
import com.foliopath360.lms.dto.response.LessonItemResponse;
import com.foliopath360.lms.dto.response.LessonResponse;
import com.foliopath360.lms.service.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/modules/{moduleId}/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping
    public ResponseEntity<LessonResponse> createLesson(
            @PathVariable UUID moduleId,
            @Valid @RequestBody LessonRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(lessonService.createLesson(moduleId, request));
    }

    @PutMapping("/{lessonId}")
    public ResponseEntity<LessonResponse> updateLesson(
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody LessonRequest request
    ) {
        return ResponseEntity.ok(
                lessonService.updateLesson(lessonId, request)
        );
    }

    @DeleteMapping("/{lessonId}")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId
    ) {
        lessonService.deleteLesson(lessonId);
        return ResponseEntity.noContent().build();
    }

    // ---------------- Lesson items (topics / text / code / docs) ----------------

    @PostMapping("/{lessonId}/items")
    public ResponseEntity<LessonItemResponse> addItem(
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody LessonItemRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(lessonService.addItem(lessonId, request));
    }

    @PutMapping("/{lessonId}/items/{itemId}")
    public ResponseEntity<LessonItemResponse> updateItem(
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @PathVariable UUID itemId,
            @Valid @RequestBody LessonItemRequest request
    ) {
        return ResponseEntity.ok(
                lessonService.updateItem(lessonId, itemId, request)
        );
    }

    @DeleteMapping("/{lessonId}/items/{itemId}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @PathVariable UUID itemId
    ) {
        lessonService.deleteItem(lessonId, itemId);
        return ResponseEntity.noContent().build();
    }

    // ---------------- Reordering ----------------

    @PatchMapping("/reorder")
    public ResponseEntity<List<LessonResponse>> reorderLessons(
            @PathVariable UUID moduleId,
            @Valid @RequestBody ReorderRequest request
    ) {
        return ResponseEntity.ok(
                lessonService.reorderLessons(moduleId, request)
        );
    }

    @PatchMapping("/{lessonId}/items/reorder")
    public ResponseEntity<List<LessonItemResponse>> reorderItems(
            @PathVariable UUID moduleId,
            @PathVariable UUID lessonId,
            @Valid @RequestBody ReorderRequest request
    ) {
        return ResponseEntity.ok(
                lessonService.reorderItems(lessonId, request)
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
}
