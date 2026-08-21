package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.LessonItemRequest;
import com.foliopath360.lms.dto.request.LessonRequest;
import com.foliopath360.lms.dto.request.ReorderRequest;
import com.foliopath360.lms.dto.response.LessonItemResponse;
import com.foliopath360.lms.dto.response.LessonResponse;

import java.util.List;
import java.util.UUID;

public interface LessonService {

    LessonResponse createLesson(UUID moduleId, LessonRequest request);

    LessonResponse updateLesson(UUID lessonId, LessonRequest request);

    LessonResponse getLessonById(UUID lessonId);

    List<LessonResponse> getLessonsByModuleId(UUID moduleId);

    void deleteLesson(UUID lessonId);

    // ---------------- Lesson items (topics / text / code / docs) ----------------

    LessonItemResponse addItem(UUID lessonId, LessonItemRequest request);

    LessonItemResponse updateItem(UUID lessonId, UUID itemId, LessonItemRequest request);

    void deleteItem(UUID lessonId, UUID itemId);

    // ---------------- Reordering ----------------

    List<LessonResponse> reorderLessons(UUID moduleId, ReorderRequest request);

    List<LessonItemResponse> reorderItems(UUID lessonId, ReorderRequest request);
}
