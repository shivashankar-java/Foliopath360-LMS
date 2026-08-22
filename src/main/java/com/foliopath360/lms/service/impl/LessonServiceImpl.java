package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.request.LessonItemRequest;
import com.foliopath360.lms.dto.request.LessonRequest;
import com.foliopath360.lms.dto.request.ReorderRequest;
import com.foliopath360.lms.dto.response.LessonItemResponse;
import com.foliopath360.lms.dto.response.LessonResponse;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.mapper.CourseMapper;
import com.foliopath360.lms.repository.CourseModuleRepository;
import com.foliopath360.lms.repository.LessonRepository;
import com.foliopath360.lms.service.LessonService;
import com.foliopath360.lms.util.ContentSanitizer;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final CourseMapper courseMapper;

    public LessonServiceImpl(
            LessonRepository lessonRepository,
            CourseModuleRepository courseModuleRepository,
            CourseMapper courseMapper
    ) {
        this.lessonRepository = lessonRepository;
        this.courseModuleRepository = courseModuleRepository;
        this.courseMapper = courseMapper;
    }

    @Override
    public LessonResponse createLesson(UUID moduleId, LessonRequest request) {

        CourseModule module = courseModuleRepository.findById(moduleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("CourseModule", "id", moduleId));

        if (lessonRepository.existsByLessonCode(request.getLessonCode())) {
            throw new IllegalArgumentException(
                    "Lesson code already exists: " + request.getLessonCode()
            );
        }

        Lesson lesson = courseMapper.toLessonEntity(request);
        lesson.setModule(module);

        // Rich text (bold / lists etc.) is stored as sanitized HTML
        lesson.setContent(ContentSanitizer.sanitize(request.getContent()));

        lesson.setLessonType(
                LessonType.valueOf(request.getLessonType())
        );
        lesson.setContentType(
                ContentType.valueOf(request.getContentType())
        );

        lesson.setStatus(LessonStatus.DRAFT);

        // Set back-references so cascade can persist the items
        if (lesson.getItems() != null) {
            lesson.getItems().forEach(item -> {
                item.setLesson(lesson);
                item.setContent(ContentSanitizer.sanitize(item.getContent()));
            });
        }

        Lesson saved = lessonRepository.save(lesson);

        return courseMapper.toLessonResponse(saved);
    }

    @Override
    public LessonResponse updateLesson(UUID lessonId, LessonRequest request) {

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lesson", "id", lessonId));

        if (!lesson.getLessonCode().equals(request.getLessonCode())
                && lessonRepository.existsByLessonCode(request.getLessonCode())) {
            throw new IllegalArgumentException(
                    "Lesson code already exists: " + request.getLessonCode()
            );
        }

        lesson.setLessonCode(request.getLessonCode());
        lesson.setTitle(request.getTitle());
        lesson.setDescription(request.getDescription());
        lesson.setContent(ContentSanitizer.sanitize(request.getContent()));
        lesson.setCodeContent(request.getCodeContent());
        lesson.setCodeLanguage(request.getCodeLanguage());
        lesson.setDocumentUrl(request.getDocumentUrl());
        lesson.setDisplayOrder(request.getDisplayOrder());
        lesson.setEstimatedMinutes(request.getEstimatedMinutes());

        if (request.getLessonType() != null) {
            lesson.setLessonType(
                    LessonType.valueOf(request.getLessonType())
            );
        }

        if (request.getContentType() != null) {
            lesson.setContentType(
                    ContentType.valueOf(request.getContentType())
            );
        }

        // Replace items when provided
        if (request.getItems() != null) {
            lesson.getItems().clear();

            for (LessonItemRequest itemRequest : request.getItems()) {
                LessonItem item = courseMapper.toItemEntity(itemRequest);
                item.setLesson(lesson);
                item.setContent(ContentSanitizer.sanitize(itemRequest.getContent()));
                lesson.getItems().add(item);
            }
        }

        Lesson saved = lessonRepository.save(lesson);

        return courseMapper.toLessonResponse(saved);
    }

    @Override
    @Transactional
    public LessonResponse getLessonById(UUID lessonId) {

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lesson", "id", lessonId));

        return courseMapper.toLessonResponse(lesson);
    }

    @Override
    @Transactional
    public List<LessonResponse> getLessonsByModuleId(UUID moduleId) {

        return lessonRepository
                .findByModuleIdOrderByDisplayOrderAsc(moduleId)
                .stream()
                .map(courseMapper::toLessonResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteLesson(UUID lessonId) {

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lesson", "id", lessonId));

        lessonRepository.delete(lesson);
    }

    // ------------------------------------------------------------------
    // Lesson items (topics / text / code / docs)
    // ------------------------------------------------------------------

    @Override
    public LessonItemResponse addItem(UUID lessonId, LessonItemRequest request) {

        Lesson lesson = findLesson(lessonId);

        LessonItem item = courseMapper.toItemEntity(request);
        item.setLesson(lesson);
        item.setContent(ContentSanitizer.sanitize(request.getContent()));
        lesson.getItems().add(item);

        lessonRepository.save(lesson);

        return courseMapper.toItemResponse(item);
    }

    @Override
    public LessonItemResponse updateItem(
            UUID lessonId, UUID itemId, LessonItemRequest request
    ) {

        Lesson lesson = findLesson(lessonId);

        LessonItem item = findItem(lesson, itemId);

        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setContent(ContentSanitizer.sanitize(request.getContent()));
        item.setCodeContent(request.getCodeContent());
        item.setCodeLanguage(request.getCodeLanguage());
        item.setDisplayOrder(request.getDisplayOrder());

        return courseMapper.toItemResponse(item);
    }

    @Override
    public void deleteItem(UUID lessonId, UUID itemId) {

        Lesson lesson = findLesson(lessonId);

        LessonItem item = findItem(lesson, itemId);

        lesson.getItems().remove(item);
    }

    // ------------------------------------------------------------------
    // Reordering
    // ------------------------------------------------------------------

    @Override
    public List<LessonResponse> reorderLessons(UUID moduleId, ReorderRequest request) {

        courseModuleRepository.findById(moduleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("CourseModule", "id", moduleId));

        for (ReorderRequest.Entry entry : request.getEntries()) {

            Lesson lesson = lessonRepository.findById(entry.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Lesson", "id", entry.getId()));

            if (!lesson.getModule().getId().equals(moduleId)) {
                throw new IllegalArgumentException(
                        "Lesson " + entry.getId() + " does not belong to this module"
                );
            }

            lesson.setDisplayOrder(entry.getDisplayOrder());
        }

        return getLessonsByModuleId(moduleId);
    }

    @Override
    public List<LessonItemResponse> reorderItems(UUID lessonId, ReorderRequest request) {

        Lesson lesson = findLesson(lessonId);

        for (ReorderRequest.Entry entry : request.getEntries()) {

            LessonItem item = findItem(lesson, entry.getId());
            item.setDisplayOrder(entry.getDisplayOrder());
        }

        return lesson.getItems().stream()
                .map(courseMapper::toItemResponse)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Lesson findLesson(UUID lessonId) {

        return lessonRepository.findById(lessonId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lesson", "id", lessonId));
    }

    private LessonItem findItem(Lesson lesson, UUID itemId) {

        return lesson.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException("LessonItem", "id", itemId));
    }
}
