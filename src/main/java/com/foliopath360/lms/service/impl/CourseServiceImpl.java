package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.request.CourseRequest;
import com.foliopath360.lms.dto.response.CourseResponse;
import com.foliopath360.lms.entity.Course;
import com.foliopath360.lms.entity.CourseStatus;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.mapper.CourseMapper;
import com.foliopath360.lms.repository.CourseRepository;
import com.foliopath360.lms.service.CourseService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    public CourseServiceImpl(CourseRepository courseRepository, CourseMapper courseMapper) {
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
    }

    @Override
    public CourseResponse createCourse(CourseRequest request) {

        if (courseRepository.existsByCourseCode(request.getCourseCode())) {
            throw new IllegalArgumentException(
                    "Course code already exists: " + request.getCourseCode()
            );
        }

        if (courseRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException(
                    "Slug already exists: " + request.getSlug()
            );
        }

        Course course = courseMapper.toEntity(request);

        if (request.getLevel() != null) {
            course.setLevel(
                    com.foliopath360.lms.entity.CourseLevel.valueOf(request.getLevel())
            );
        }

        course.setStatus(CourseStatus.DRAFT);

        Course saved = courseRepository.save(course);

        return courseMapper.toResponse(saved);
    }

    @Override
    public CourseResponse updateCourse(UUID id, CourseRequest request) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", id));

        course.setCourseCode(request.getCourseCode());
        course.setTitle(request.getTitle());
        course.setSlug(request.getSlug());
        course.setShortDescription(request.getShortDescription());
        course.setDescription(request.getDescription());
        course.setThumbnailUrl(request.getThumbnailUrl());
        course.setLanguage(request.getLanguage());
        course.setPrice(request.getPrice());

        if (request.getLevel() != null) {
            course.setLevel(
                    com.foliopath360.lms.entity.CourseLevel.valueOf(request.getLevel())
            );
        }

        Course saved = courseRepository.save(course);

        return courseMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CourseResponse getCourseById(UUID id) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", id));

        return courseMapper.toResponse(course);
    }

    @Override
    @Transactional
    public CourseResponse getCourseBySlug(String slug) {

        Course course = courseRepository.findBySlug(slug)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "slug", slug));

        return courseMapper.toResponse(course);
    }

    @Override
    @Transactional
    public List<CourseResponse> getAllCourses() {

        return courseRepository.findAll()
                .stream()
                .map(courseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<CourseResponse> getPublishedCourses() {

        return courseRepository.findByStatus(CourseStatus.PUBLISHED)
                .stream()
                .map(courseMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteCourse(UUID id) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", id));

        courseRepository.delete(course);
    }

    @Override
    public CourseResponse publishCourse(UUID id) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", id));

        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishedAt(LocalDateTime.now());

        Course saved = courseRepository.save(course);

        return courseMapper.toResponse(saved);
    }
}
