package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.request.MockTestAttemptRequest;
import com.foliopath360.lms.dto.request.MockTestOptionRequest;
import com.foliopath360.lms.dto.request.MockTestQuestionRequest;
import com.foliopath360.lms.dto.request.MockTestRequest;
import com.foliopath360.lms.dto.response.MockTestAttemptResponse;
import com.foliopath360.lms.dto.response.MockTestResponse;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.mapper.MockTestMapper;
import com.foliopath360.lms.repository.CourseModuleRepository;
import com.foliopath360.lms.repository.MockTestAttemptRepository;
import com.foliopath360.lms.repository.MockTestQuestionRepository;
import com.foliopath360.lms.repository.MockTestRepository;
import com.foliopath360.lms.service.MockTestService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class MockTestServiceImpl implements MockTestService {

    private static final Set<String> ALLOWED_QUESTION_TYPES =
            Set.of("TEXT", "CODE");
    private static final Set<String> ALLOWED_OPTION_LABELS =
            Set.of("A", "B", "C", "D");

    private final MockTestRepository mockTestRepository;
    private final MockTestQuestionRepository mockTestQuestionRepository;
    private final MockTestAttemptRepository mockTestAttemptRepository;
    private final CourseModuleRepository courseModuleRepository;
    private final MockTestMapper mockTestMapper;

    @Override
    public MockTestResponse createMockTest(UUID moduleId, MockTestRequest request) {

        CourseModule module = courseModuleRepository.findById(moduleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("CourseModule", "id", moduleId));

        String testCode = resolveUniqueTestCode(request.getTestCode());

        MockTest mockTest = mockTestMapper.toEntity(request);
        mockTest.setModule(module);
        mockTest.setTestCode(testCode);

        applyQuestions(mockTest, request.getQuestions());

        MockTest saved = mockTestRepository.save(mockTest);

        return mockTestMapper.toResponse(saved);
    }

    @Override
    public MockTestResponse updateMockTest(UUID mockTestId, MockTestRequest request) {

        MockTest mockTest = findMockTest(mockTestId);

        mockTest.setTitle(request.getTitle());
        mockTest.setDescription(request.getDescription());
        mockTest.setDurationMinutes(request.getDurationMinutes());
        mockTest.setPassPercentage(request.getPassPercentage());
        mockTest.setDisplayOrder(request.getDisplayOrder());

        // Replace questions wholesale (fresh ids / codes are supplied by the client)
        mockTest.getQuestions().clear();
        applyQuestions(mockTest, request.getQuestions());

        MockTest saved = mockTestRepository.save(mockTest);

        return mockTestMapper.toResponse(saved);
    }

    @Override
    public void deleteMockTest(UUID mockTestId) {

        MockTest mockTest = findMockTest(mockTestId);

        mockTestRepository.delete(mockTest);
    }

    @Override
    @Transactional
    public MockTestResponse getMockTestById(UUID mockTestId, User requester) {

        MockTest mockTest = findMockTest(mockTestId);

        MockTestResponse response = mockTestMapper.toResponse(mockTest);

        if (!canViewAnswers(requester)) {
            hideCorrectOptions(response);
        }
        return response;
    }

    @Override
    @Transactional
    public List<MockTestResponse> getMockTestsByModule(UUID moduleId, User requester) {

        boolean includeAnswers = canViewAnswers(requester);

        return mockTestRepository.findByModuleIdOrderByDisplayOrderAsc(moduleId)
                .stream()
                .map(mockTestMapper::toResponse)
                .peek(response -> {
                    if (!includeAnswers) {
                        hideCorrectOptions(response);
                    }
                })
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Student attempt (grading happens server-side only)
    // ------------------------------------------------------------------

    @Override
    public MockTestAttemptResponse submitAttempt(
            User student, UUID mockTestId, MockTestAttemptRequest request) {

        MockTest mockTest = findMockTest(mockTestId);

        // Each student can attempt a mock test only once
        if (mockTestAttemptRepository.existsByUserIdAndMockTestId(
                student.getId(), mockTestId)) {
            throw new IllegalStateException(
                    "You have already attempted this mock test. "
                            + "Only one attempt is allowed per student.");
        }

        List<MockTestQuestion> questions = mockTest.getQuestions();
        int total = questions.size();

        Map<String, String> answers = request.getAnswers() == null
                ? Collections.emptyMap()
                : request.getAnswers();

        int correct = 0;
        for (MockTestQuestion question : questions) {
            String submitted = answers.get(question.getId().toString());
            if (submitted != null
                    && submitted.trim().equalsIgnoreCase(question.getCorrectOption())) {
                correct++;
            }
        }

        int percentage = total == 0 ? 0 :
                (int) Math.round((correct * 100.0) / total);
        boolean passed = percentage >= mockTest.getPassPercentage();

        LocalDateTime now = LocalDateTime.now();

        MockTestAttempt attempt = MockTestAttempt.builder()
                .user(student)
                .mockTest(mockTest)
                .score(correct)
                .totalQuestions(total)
                .percentage(percentage)
                .passed(passed)
                .submittedAt(now)
                .build();

        MockTestAttempt saved = mockTestAttemptRepository.save(attempt);

        return toAttemptResponse(saved, mockTest);
    }

    @Override
    @Transactional
    public Optional<MockTestAttemptResponse> getMyAttempt(
            User student, UUID mockTestId) {

        return mockTestAttemptRepository
                .findFirstByUserIdAndMockTestIdOrderBySubmittedAtDesc(
                        student.getId(), mockTestId)
                .map(attempt -> {
                    MockTest mockTest = findMockTest(mockTestId);
                    return toAttemptResponse(attempt, mockTest);
                });
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void applyQuestions(MockTest mockTest, List<MockTestQuestionRequest> requests) {

        for (MockTestQuestionRequest questionRequest : requests) {

            validateQuestion(questionRequest);

            MockTestQuestion question = mockTestMapper.toQuestionEntity(questionRequest);
            question.setMockTest(mockTest);
            question.setQuestionType(
                    QuestionType.valueOf(questionRequest.getQuestionType()));
            question.setQuestionCode(
                    resolveUniqueQuestionCode(questionRequest.getQuestionCode()));
            question.setCorrectOption(
                    questionRequest.getCorrectOption().trim().toUpperCase());

            for (MockTestOptionRequest optionRequest : questionRequest.getOptions()) {
                MockTestOption option = mockTestMapper.toOptionEntity(optionRequest);
                option.setQuestion(question);
                option.setLabel(optionRequest.getLabel().trim().toUpperCase());
                option.setText(optionRequest.getText().trim());
                question.getOptions().add(option);
            }

            mockTest.getQuestions().add(question);
        }
    }

    private void validateQuestion(MockTestQuestionRequest question) {

        if (!ALLOWED_QUESTION_TYPES.contains(question.getQuestionType())) {
            throw new IllegalArgumentException(
                    "Invalid question type: " + question.getQuestionType()
                            + " (allowed: TEXT, CODE)");
        }

        List<MockTestOptionRequest> options = question.getOptions();
        if (options.size() > 4) {
            throw new IllegalArgumentException(
                    "A question can have at most 4 options");
        }

        Set<String> labels = new HashSet<>();
        for (MockTestOptionRequest option : options) {
            String label = option.getLabel() == null ? "" :
                    option.getLabel().trim().toUpperCase();
            if (!ALLOWED_OPTION_LABELS.contains(label)) {
                throw new IllegalArgumentException(
                        "Option label must be one of A, B, C, D: " + label);
            }
            if (!labels.add(label)) {
                throw new IllegalArgumentException(
                        "Duplicate option label: " + label);
            }
        }

        String correct = question.getCorrectOption() == null ? "" :
                question.getCorrectOption().trim().toUpperCase();
        if (!labels.contains(correct)) {
            throw new IllegalArgumentException(
                    "correctOption '" + correct + "' does not match any option label");
        }
    }

    private String resolveUniqueTestCode(String requested) {

        if (requested != null && !requested.isBlank()) {
            if (mockTestRepository.existsByTestCode(requested)) {
                throw new IllegalArgumentException(
                        "Mock test code already exists: " + requested);
            }
            return requested;
        }
        return generateCode("MTS", code ->
                mockTestRepository.existsByTestCode(code));
    }

    private String resolveUniqueQuestionCode(String requested) {

        if (requested != null && !requested.isBlank()) {
            if (mockTestQuestionRepository.existsByQuestionCode(requested)) {
                throw new IllegalArgumentException(
                        "Question code already exists: " + requested);
            }
            return requested;
        }
        return generateCode("QST", code ->
                mockTestQuestionRepository.existsByQuestionCode(code));
    }

    private String generateCode(String prefix, java.util.function.Predicate<String> exists) {

        String code;
        do {
            code = prefix + "-" + Long.toString(System.currentTimeMillis(), 36)
                    .toUpperCase() + "-" + (int) (Math.random() * 900 + 100);
        } while (exists.test(code));
        return code;
    }

    private boolean canViewAnswers(User user) {

        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .map(Role::getRoleName)
                .anyMatch(name -> name.equals("SUPER_ADMIN") || name.equals("STAFF"));
    }

    private void hideCorrectOptions(MockTestResponse response) {

        if (response.getQuestions() == null) {
            return;
        }
        response.getQuestions()
                .forEach(question -> question.setCorrectOption(null));
    }

    private MockTest findMockTest(UUID mockTestId) {

        return mockTestRepository.findById(mockTestId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("MockTest", "id", mockTestId));
    }

    private MockTestAttemptResponse toAttemptResponse(
            MockTestAttempt attempt, MockTest mockTest) {

        return MockTestAttemptResponse.builder()
                .attemptId(attempt.getId())
                .mockTestId(mockTest.getId())
                .userId(attempt.getUser().getId())
                .score(attempt.getScore())
                .total(attempt.getTotalQuestions())
                .percentage(attempt.getPercentage())
                .passed(attempt.getPassed())
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }
}
