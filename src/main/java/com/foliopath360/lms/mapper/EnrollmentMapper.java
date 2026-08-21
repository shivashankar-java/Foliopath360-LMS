package com.foliopath360.lms.mapper;

import com.foliopath360.lms.dto.response.EnrollmentResponse;
import com.foliopath360.lms.dto.response.StudentEnrolledCourseResponse;
import com.foliopath360.lms.entity.Enrollment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EnrollmentMapper {

    @Mapping(target = "enrollmentId", source = "id")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseCode", source = "course.courseCode")
    @Mapping(target = "courseTitle", source = "course.title")
    @Mapping(target = "enrollmentStatus", source = "status")
    EnrollmentResponse toResponse(Enrollment enrollment);

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseCode", source = "course.courseCode")
    @Mapping(target = "title", source = "course.title")
    @Mapping(target = "slug", source = "course.slug")
    @Mapping(target = "shortDescription", source = "course.shortDescription")
    @Mapping(target = "thumbnailUrl", source = "course.thumbnailUrl")
    @Mapping(target = "level", source = "course.level")
    @Mapping(target = "enrollmentStatus", source = "status")
    StudentEnrolledCourseResponse toEnrolledCourseResponse(Enrollment enrollment);
}
