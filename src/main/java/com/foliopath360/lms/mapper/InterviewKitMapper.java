package com.foliopath360.lms.mapper;

import com.foliopath360.lms.dto.response.InterviewKitEnrollmentResponse;
import com.foliopath360.lms.dto.response.InterviewKitModuleResponse;
import com.foliopath360.lms.dto.response.InterviewKitQuestionResponse;
import com.foliopath360.lms.dto.response.InterviewKitResponse;
import com.foliopath360.lms.dto.response.StudentEnrolledKitResponse;
import com.foliopath360.lms.entity.InterviewKit;
import com.foliopath360.lms.entity.InterviewKitEnrollment;
import com.foliopath360.lms.entity.InterviewKitModule;
import com.foliopath360.lms.entity.InterviewKitQuestion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InterviewKitMapper {

    @Mapping(target = "questionCount", expression = "java((long) kit.getQuestions().size())")
    @Mapping(target = "enrollmentCount", ignore = true)
    InterviewKitResponse toResponse(InterviewKit kit);

    @Mapping(target = "kitId", source = "kit.id")
    @Mapping(target = "moduleId", expression = "java(question.getModule() != null ? question.getModule().getId().toString() : null)")
    InterviewKitQuestionResponse toQuestionResponse(InterviewKitQuestion question);

    @Mapping(target = "kitId", source = "kit.id")
    @Mapping(target = "questionCount", expression = "java((long) module.getQuestions().size())")
    @Mapping(target = "questions", ignore = true)
    InterviewKitModuleResponse toModuleResponse(InterviewKitModule module);

    @Mapping(target = "enrollmentId", source = "id")
    @Mapping(target = "kitId", source = "kit.id")
    @Mapping(target = "kitName", source = "kit.name")
    @Mapping(target = "enrollmentStatus", source = "status")
    InterviewKitEnrollmentResponse toEnrollmentResponse(InterviewKitEnrollment enrollment);

    @Mapping(target = "kitId", source = "kit.id")
    @Mapping(target = "kitCode", source = "kit.kitCode")
    @Mapping(target = "name", source = "kit.name")
    @Mapping(target = "slug", source = "kit.slug")
    @Mapping(target = "description", source = "kit.description")
    @Mapping(target = "thumbnailUrl", source = "kit.thumbnailUrl")
    @Mapping(target = "level", source = "kit.level")
    @Mapping(target = "price", source = "kit.price")
    @Mapping(target = "enrollmentStatus", source = "status")
    StudentEnrolledKitResponse toStudentEnrolledKitResponse(InterviewKitEnrollment enrollment);
}
