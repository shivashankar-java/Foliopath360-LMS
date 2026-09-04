package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.CourseModuleLinkRequest;
import com.foliopath360.lms.dto.request.ReorderRequest;
import com.foliopath360.lms.dto.response.CourseModuleLinkResponse;

import java.util.List;
import java.util.UUID;

public interface CourseCourseModuleService {

    /**
     * Add an existing course as a module inside the given parent course.
     *
     * @throws IllegalArgumentException if the course is the same as parent (self-reference),
     *         or already linked (duplicate), or would create a circular dependency.
     */
    CourseModuleLinkResponse addCourseAsModule(UUID parentCourseId, CourseModuleLinkRequest request);

    List<CourseModuleLinkResponse> getCourseModules(UUID parentCourseId);

    CourseModuleLinkResponse getCourseModuleById(UUID parentCourseId, UUID courseModuleId);

    void removeCourseModule(UUID parentCourseId, UUID courseModuleId);

    List<CourseModuleLinkResponse> reorderCourseModules(UUID parentCourseId, ReorderRequest request);
}