package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.InterviewKitQuestionRequest;
import com.foliopath360.lms.dto.request.InterviewKitRequest;
import com.foliopath360.lms.dto.response.InterviewKitEnrollmentResponse;
import com.foliopath360.lms.dto.response.InterviewKitQuestionResponse;
import com.foliopath360.lms.dto.response.InterviewKitResponse;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.StudentEnrolledKitResponse;
import com.foliopath360.lms.entity.User;

import java.util.List;
import java.util.UUID;

public interface InterviewKitService {

    // Admin CRUD
    InterviewKitResponse createKit(InterviewKitRequest request);
    InterviewKitResponse updateKit(UUID kitId, InterviewKitRequest request);
    void deleteKit(UUID kitId);
    InterviewKitResponse publishKit(UUID kitId);
    InterviewKitResponse unpublishKit(UUID kitId);
    List<InterviewKitResponse> getAllKits();
    InterviewKitResponse getKitById(UUID kitId);

    // Admin question management
    InterviewKitQuestionResponse addQuestion(UUID kitId, InterviewKitQuestionRequest request);
    InterviewKitQuestionResponse updateQuestion(UUID kitId, UUID questionId, InterviewKitQuestionRequest request);
    void deleteQuestion(UUID kitId, UUID questionId);
    List<InterviewKitQuestionResponse> getQuestions(UUID kitId);

    // Admin enrollment
    InterviewKitEnrollmentResponse adminEnrollStudent(UUID studentId, UUID kitId);
    List<InterviewKitEnrollmentResponse> getStudentKitEnrollments(UUID studentId);

    // Student
    List<StudentEnrolledKitResponse> getMyEnrolledKits(User student);
    InterviewKitEnrollmentResponse getMyKitEnrollment(User student, UUID kitId);
    InterviewKitEnrollmentResponse enrollInKit(User student, UUID kitId);
    InterviewKitEnrollmentResponse dropKitEnrollment(User student, UUID kitId);
    List<InterviewKitResponse> getPublishedKits();
    MessageResponse enrollStudentInKit(User student, UUID kitId);
}
