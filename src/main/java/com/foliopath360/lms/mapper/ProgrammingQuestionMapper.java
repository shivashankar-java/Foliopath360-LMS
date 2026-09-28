package com.foliopath360.lms.mapper;

import com.foliopath360.lms.dto.request.ProgrammingQuestionRequest;
import com.foliopath360.lms.dto.request.ProgrammingTestCaseRequest;
import com.foliopath360.lms.dto.response.ProgrammingQuestionResponse;
import com.foliopath360.lms.dto.response.ProgrammingQuestionSubmissionResponse;
import com.foliopath360.lms.dto.response.ProgrammingTestCaseResponse;
import com.foliopath360.lms.entity.ProgrammingQuestion;
import com.foliopath360.lms.entity.ProgrammingQuestionSubmission;
import com.foliopath360.lms.entity.ProgrammingTestCase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProgrammingQuestionMapper {

    // Test cases are wired manually in the service so the
    // question back-reference is never left null. allowedLanguages
    // is stored as a comma-joined string and set manually.
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "course", ignore = true)
    @Mapping(target = "questionCode", ignore = true)
    @Mapping(target = "allowedLanguages", ignore = true)
    @Mapping(target = "testCases", ignore = true)
    ProgrammingQuestion toEntity(ProgrammingQuestionRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "question", ignore = true)
    ProgrammingTestCase toTestCaseEntity(ProgrammingTestCaseRequest request);

    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "allowedLanguages", ignore = true)
    ProgrammingQuestionResponse toResponse(ProgrammingQuestion question);

    ProgrammingTestCaseResponse toTestCaseResponse(ProgrammingTestCase testCase);

    @Mapping(target = "id", source = "submission.id")
    @Mapping(target = "questionId", source = "submission.question.id")
    @Mapping(target = "questionTitle", source = "submission.question.title")
    @Mapping(target = "status", expression = "java(submission.getStatus() == null ? null : submission.getStatus().name())")
    ProgrammingQuestionSubmissionResponse toSubmissionResponse(ProgrammingQuestionSubmission submission);
}