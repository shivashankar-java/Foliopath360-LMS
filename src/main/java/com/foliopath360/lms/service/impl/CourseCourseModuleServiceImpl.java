package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.request.CourseModuleLinkRequest;
import com.foliopath360.lms.dto.request.ReorderRequest;
import com.foliopath360.lms.dto.response.CourseModuleLinkResponse;
import com.foliopath360.lms.entity.Course;
import com.foliopath360.lms.entity.CourseCourseModule;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.repository.CourseCourseModuleRepository;
import com.foliopath360.lms.repository.CourseRepository;
import com.foliopath360.lms.service.CourseCourseModuleService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CourseCourseModuleServiceImpl implements CourseCourseModuleService {

    private final CourseCourseModuleRepository courseCourseModuleRepository;
    private final CourseRepository courseRepository;

    public CourseCourseModuleServiceImpl(
            CourseCourseModuleRepository courseCourseModuleRepository,
            CourseRepository courseRepository
    ) {
        this.courseCourseModuleRepository = courseCourseModuleRepository;
        this.courseRepository = courseRepository;
    }

    @Override
    public CourseModuleLinkResponse addCourseAsModule(UUID parentCourseId, CourseModuleLinkRequest request) {

        if (parentCourseId.equals(request.getCourseId())) {
            throw new IllegalArgumentException(
                    "A course cannot be added as a module to itself."
            );
        }

        Course parentCourse = courseRepository.findById(parentCourseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", parentCourseId));

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));

        if (courseCourseModuleRepository.existsByParentCourseIdAndCourseId(parentCourseId, request.getCourseId())) {
            throw new IllegalArgumentException(
                    "This course is already added as a module."
            );
        }

        if (wouldCreateCircularDependency(parentCourseId, request.getCourseId())) {
            throw new IllegalArgumentException(
                    "This course cannot be added because it would create a circular course dependency."
            );
        }

        int maxOrder = courseCourseModuleRepository.countByParentCourseId(parentCourseId);
        int displayOrder = request.getDisplayOrder() != null
                ? request.getDisplayOrder()
                : maxOrder + 1;

        CourseCourseModule link = CourseCourseModule.builder()
                .parentCourse(parentCourse)
                .course(course)
                .displayOrder(displayOrder)
                .build();

        CourseCourseModule saved = courseCourseModuleRepository.save(link);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public List<CourseModuleLinkResponse> getCourseModules(UUID parentCourseId) {
        courseRepository.findById(parentCourseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", parentCourseId));

        return courseCourseModuleRepository
                .findByParentCourseIdOrderByDisplayOrderAsc(parentCourseId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CourseModuleLinkResponse getCourseModuleById(UUID parentCourseId, UUID courseModuleId) {
        CourseCourseModule link = courseCourseModuleRepository.findByIdAndParentCourseId(courseModuleId, parentCourseId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseCourseModule", "id", courseModuleId));
        return toResponse(link);
    }

    @Override
    public void removeCourseModule(UUID parentCourseId, UUID courseModuleId) {
        CourseCourseModule link = courseCourseModuleRepository.findByIdAndParentCourseId(courseModuleId, parentCourseId)
                .orElseThrow(() -> new ResourceNotFoundException("CourseCourseModule", "id", courseModuleId));
        courseCourseModuleRepository.delete(link);
    }

    @Override
    public List<CourseModuleLinkResponse> reorderCourseModules(UUID parentCourseId, ReorderRequest request) {

        courseRepository.findById(parentCourseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", parentCourseId));

        for (ReorderRequest.Entry entry : request.getEntries()) {

            CourseCourseModule link = courseCourseModuleRepository.findById(entry.getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "CourseCourseModule", "id", entry.getId()));

            if (!link.getParentCourse().getId().equals(parentCourseId)) {
                throw new IllegalArgumentException(
                        "Module " + entry.getId() + " does not belong to this course"
                );
            }

            link.setDisplayOrder(entry.getDisplayOrder());
            courseCourseModuleRepository.save(link);
        }

        return getCourseModules(parentCourseId);
    }

    /**
     * Checks whether linking courseId to parentCourseId would create a circular dependency.
     *
     * We are about to add: parentCourse -> contains -> course.
     * A cycle would form if course already (transitively) contains parentCourse.
     * BFS outward from courseId following the "contains" relation; if we ever
     * reach parentCourseId, adding the link would be circular.
     */
    private boolean wouldCreateCircularDependency(UUID parentCourseId, UUID courseId) {

        java.util.Queue<UUID> queue = new java.util.LinkedList<>();
        java.util.Set<UUID> visited = new java.util.HashSet<>();
        queue.add(courseId);
        visited.add(courseId);

        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            // What does `current` already contain as course-modules?
            List<CourseCourseModule> contained =
                    courseCourseModuleRepository.findByParentCourseIdOrderByDisplayOrderAsc(current);
            for (CourseCourseModule link : contained) {
                UUID containedCourseId = link.getCourse().getId();
                if (containedCourseId.equals(parentCourseId)) {
                    return true;
                }
                if (visited.add(containedCourseId)) {
                    queue.add(containedCourseId);
                }
            }
        }
        return false;
    }

    private CourseModuleLinkResponse toResponse(CourseCourseModule link) {
        Course course = link.getCourse();
        return CourseModuleLinkResponse.builder()
                .id(link.getId())
                .courseId(course.getId())
                .courseCode(course.getCourseCode())
                .title(course.getTitle())
                .shortDescription(course.getShortDescription())
                .status(course.getStatus() != null ? course.getStatus().name() : null)
                .displayOrder(link.getDisplayOrder())
                .build();
    }
}