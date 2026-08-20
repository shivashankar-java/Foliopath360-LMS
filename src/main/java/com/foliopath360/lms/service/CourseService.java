package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.CourseRequest;
import com.foliopath360.lms.dto.response.CourseResponse;

import java.util.List;
import java.util.UUID;

public interface CourseService {

    CourseResponse createCourse(CourseRequest request);

    CourseResponse updateCourse(UUID id, CourseRequest request);

    CourseResponse getCourseById(UUID id);

    CourseResponse getCourseBySlug(String slug);

    List<CourseResponse> getAllCourses();

    List<CourseResponse> getPublishedCourses();

    void deleteCourse(UUID id);

    CourseResponse publishCourse(UUID id);
}
