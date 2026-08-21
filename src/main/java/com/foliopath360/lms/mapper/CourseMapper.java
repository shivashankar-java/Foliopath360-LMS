package com.foliopath360.lms.mapper;

import com.foliopath360.lms.dto.request.CourseRequest;
import com.foliopath360.lms.dto.request.LessonItemRequest;
import com.foliopath360.lms.dto.response.CourseResponse;
import com.foliopath360.lms.dto.response.LessonItemResponse;
import com.foliopath360.lms.dto.response.LessonResponse;
import com.foliopath360.lms.dto.response.ModuleResponse;
import com.foliopath360.lms.entity.Course;
import com.foliopath360.lms.entity.CourseModule;
import com.foliopath360.lms.entity.Lesson;
import com.foliopath360.lms.entity.LessonItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CourseMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "modules", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    Course toEntity(CourseRequest request);

    // modules auto-mapped via toModuleResponse
    CourseResponse toResponse(Course course);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "lessons", ignore = true)
    @Mapping(target = "status", ignore = true)
    CourseModule toModuleEntity(com.foliopath360.lms.dto.request.ModuleRequest request);

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "lessons", ignore = true)
    ModuleResponse toModuleResponse(CourseModule module);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "module", ignore = true)
    @Mapping(target = "status", ignore = true)
    Lesson toLessonEntity(com.foliopath360.lms.dto.request.LessonRequest request);

    // items auto-mapped via toItemResponse
    LessonResponse toLessonResponse(Lesson lesson);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lesson", ignore = true)
    LessonItem toItemEntity(LessonItemRequest request);

    LessonItemResponse toItemResponse(LessonItem item);
}
