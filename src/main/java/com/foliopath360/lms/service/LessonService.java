package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.LessonItemRequest;
import com.foliopath360.lms.dto.request.LessonRequest;
import com.foliopath360.lms.dto.request.ReorderRequest;
import com.foliopath360.lms.dto.response.LessonItemResponse;
import com.foliopath360.lms.dto.response.LessonResponse;
import com.foliopath360.lms.entity.User;

import java.util.List;
import java.util.UUID;

public interface LessonService {

    LessonResponse createLesson(UUID moduleId, LessonRequest request);

    LessonResponse updateLesson(UUID lessonId, LessonRequest request);

    LessonResponse getLessonById(UUID lessonId);

    List<LessonResponse> getLessonsByModuleId(UUID moduleId);

    /**
     * Enrollment-aware read: lesson content is only returned when the
     * requester is SUPER_ADMIN / STAFF or an enrolled student.
     * Otherwise metadata is returned with locked = true.
     */
    LessonResponse getLessonByIdForRequester(UUID lessonId, User requester);

    /**
     * Enrollment-aware list read (same rules as getLessonByIdForRequester).
     */
    List<LessonResponse> getLessonsByModuleIdForRequester(
            UUID moduleId, User requester);

    void deleteLesson(UUID lessonId);

    // ---------------- Lesson items (topics / text / code / docs) ----------------

    LessonItemResponse addItem(UUID lessonId, LessonItemRequest request);

    LessonItemResponse updateItem(UUID lessonId, UUID itemId, LessonItemRequest request);

    void deleteItem(UUID lessonId, UUID itemId);

    // ---------------- Reordering ----------------

    List<LessonResponse> reorderLessons(UUID moduleId, ReorderRequest request);

    List<LessonItemResponse> reorderItems(UUID lessonId, ReorderRequest request);
}
