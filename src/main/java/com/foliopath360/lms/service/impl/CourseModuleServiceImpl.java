package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.request.ModuleRequest;
import com.foliopath360.lms.dto.response.ModuleResponse;
import com.foliopath360.lms.entity.Course;
import com.foliopath360.lms.entity.CourseModule;
import com.foliopath360.lms.entity.ModuleStatus;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.mapper.CourseMapper;
import com.foliopath360.lms.repository.CourseModuleRepository;
import com.foliopath360.lms.repository.CourseRepository;
import com.foliopath360.lms.service.CourseModuleService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourseModuleServiceImpl implements CourseModuleService {

    private final CourseModuleRepository courseModuleRepository;
    private final CourseRepository courseRepository;
    private final CourseMapper courseMapper;

    public CourseModuleServiceImpl(
            CourseModuleRepository courseModuleRepository,
            CourseRepository courseRepository,
            CourseMapper courseMapper
    ) {
        this.courseModuleRepository = courseModuleRepository;
        this.courseRepository = courseRepository;
        this.courseMapper = courseMapper;
    }

    @Override
    public ModuleResponse createModule(UUID courseId, ModuleRequest request) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", courseId));

        if (courseModuleRepository.existsByModuleCode(request.getModuleCode())) {
            throw new IllegalArgumentException(
                    "Module code already exists: " + request.getModuleCode()
            );
        }

        CourseModule module = courseMapper.toModuleEntity(request);
        module.setCourse(course);
        module.setStatus(ModuleStatus.ACTIVE);

        CourseModule saved = courseModuleRepository.save(module);

        return courseMapper.toModuleResponse(saved);
    }

    @Override
    public ModuleResponse updateModule(UUID moduleId, ModuleRequest request) {

        CourseModule module = courseModuleRepository.findById(moduleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("CourseModule", "id", moduleId));

        module.setModuleCode(request.getModuleCode());
        module.setTitle(request.getTitle());
        module.setDescription(request.getDescription());
        module.setDisplayOrder(request.getDisplayOrder());

        CourseModule saved = courseModuleRepository.save(module);

        return courseMapper.toModuleResponse(saved);
    }

    @Override
    @Transactional
    public ModuleResponse getModuleById(UUID moduleId) {

        CourseModule module = courseModuleRepository.findById(moduleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("CourseModule", "id", moduleId));

        return courseMapper.toModuleResponse(module);
    }

    @Override
    @Transactional
    public List<ModuleResponse> getModulesByCourseId(UUID courseId) {

        return courseModuleRepository
                .findByCourseIdOrderByDisplayOrderAsc(courseId)
                .stream()
                .map(courseMapper::toModuleResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteModule(UUID moduleId) {

        CourseModule module = courseModuleRepository.findById(moduleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("CourseModule", "id", moduleId));

        courseModuleRepository.delete(module);
    }
}
