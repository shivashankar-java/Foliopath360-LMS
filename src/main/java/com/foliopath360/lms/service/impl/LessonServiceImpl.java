package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.request.LessonRequest;
import com.foliopath360.lms.dto.response.LessonResponse;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.mapper.CourseMapper;
import com.foliopath360.lms.repository.CourseModuleRepository;
import com.foliopath360.lms.repository.LessonRepository;
import com.foliopath360.lms.service.LessonService;
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

        lesson.setLessonType(
                LessonType.valueOf(request.getLessonType())
        );
        lesson.setContentType(
                ContentType.valueOf(request.getContentType())
        );

        lesson.setStatus(LessonStatus.DRAFT);

        Lesson saved = lessonRepository.save(lesson);

        return courseMapper.toLessonResponse(saved);
    }

    @Override
    public LessonResponse updateLesson(UUID lessonId, LessonRequest request) {

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lesson", "id", lessonId));

        lesson.setLessonCode(request.getLessonCode());
        lesson.setTitle(request.getTitle());
        lesson.setDescription(request.getDescription());
        lesson.setContent(request.getContent());
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
}
