package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.MockTestAttemptRequest;
import com.foliopath360.lms.dto.request.MockTestRequest;
import com.foliopath360.lms.dto.response.MockTestAttemptResponse;
import com.foliopath360.lms.dto.response.MockTestResponse;
import com.foliopath360.lms.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MockTestService {

    MockTestResponse createMockTest(UUID moduleId, MockTestRequest request);

    MockTestResponse updateMockTest(UUID mockTestId, MockTestRequest request);

    void deleteMockTest(UUID mockTestId);

    /**
     * Single test. Correct answers are only exposed to SUPER_ADMIN / STAFF.
     */
    MockTestResponse getMockTestById(UUID mockTestId, User requester);

    List<MockTestResponse> getMockTestsByModule(UUID moduleId, User requester);

    /**
     * Submit a student attempt. Each student can attempt a mock test
     * only once; grading happens server-side.
     */
    MockTestAttemptResponse submitAttempt(
            User student, UUID mockTestId, MockTestAttemptRequest request);

    /**
     * The current student's existing (single) attempt, if any.
     */
    Optional<MockTestAttemptResponse> getMyAttempt(
            User student, UUID mockTestId);
}
