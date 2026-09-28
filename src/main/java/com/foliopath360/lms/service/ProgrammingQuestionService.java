package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.ProgrammingQuestionRequest;
import com.foliopath360.lms.dto.request.ProgrammingQuestionRunRequest;
import com.foliopath360.lms.dto.request.ProgrammingQuestionSubmitRequest;
import com.foliopath360.lms.dto.response.CodeExecutionResponse;
import com.foliopath360.lms.dto.response.ProgrammingQuestionResponse;
import com.foliopath360.lms.dto.response.ProgrammingQuestionSubmissionResponse;
import com.foliopath360.lms.dto.response.ProgrammingQuestionSubmitResponse;
import com.foliopath360.lms.entity.User;

import java.util.List;
import java.util.UUID;

public interface ProgrammingQuestionService {

    ProgrammingQuestionResponse createProgrammingQuestion(UUID courseId, ProgrammingQuestionRequest request);

    ProgrammingQuestionResponse updateProgrammingQuestion(UUID questionId, ProgrammingQuestionRequest request);

    void deleteProgrammingQuestion(UUID questionId);

    ProgrammingQuestionResponse getProgrammingQuestion(UUID questionId, User requester);

    List<ProgrammingQuestionResponse> getProgrammingQuestionsByCourse(UUID courseId, User requester);

    CodeExecutionResponse run(UUID questionId, ProgrammingQuestionRunRequest request);

    ProgrammingQuestionSubmitResponse submit(
            UUID questionId, ProgrammingQuestionSubmitRequest request, User requester);

    List<ProgrammingQuestionSubmissionResponse> getProgrammingQuestionSubmissions(
            UUID questionId, User requester);
}