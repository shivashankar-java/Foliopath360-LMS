package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.request.CodeExecutionRequest;
import com.foliopath360.lms.dto.request.ProgrammingQuestionRequest;
import com.foliopath360.lms.dto.request.ProgrammingQuestionRunRequest;
import com.foliopath360.lms.dto.request.ProgrammingQuestionSubmitRequest;
import com.foliopath360.lms.dto.request.ProgrammingTestCaseRequest;
import com.foliopath360.lms.dto.response.CodeExecutionResponse;
import com.foliopath360.lms.dto.response.ProgrammingQuestionResponse;
import com.foliopath360.lms.dto.response.ProgrammingQuestionSubmissionResponse;
import com.foliopath360.lms.dto.response.ProgrammingQuestionSubmitResponse;
import com.foliopath360.lms.dto.response.ProgrammingSubmissionCaseResult;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.execution.ExecutorFactory;
import com.foliopath360.lms.mapper.ProgrammingQuestionMapper;
import com.foliopath360.lms.repository.CourseRepository;
import com.foliopath360.lms.repository.ProgrammingQuestionRepository;
import com.foliopath360.lms.repository.ProgrammingQuestionSubmissionRepository;
import com.foliopath360.lms.service.CodeExecutionService;
import com.foliopath360.lms.service.ProgrammingQuestionService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class ProgrammingQuestionServiceImpl implements ProgrammingQuestionService {

    private static final ProgrammingQuestionDifficulty DEFAULT_DIFFICULTY =
            ProgrammingQuestionDifficulty.MEDIUM;

    private final ProgrammingQuestionRepository programmingQuestionRepository;
    private final ProgrammingQuestionSubmissionRepository programmingQuestionSubmissionRepository;
    private final CourseRepository courseRepository;
    private final ProgrammingQuestionMapper programmingQuestionMapper;
    private final CodeExecutionService codeExecutionService;
    private final ExecutorFactory executorFactory;

    @Override
    public ProgrammingQuestionResponse createProgrammingQuestion(
            UUID courseId, ProgrammingQuestionRequest request) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Course", "id", courseId));

        String questionCode = resolveUniqueQuestionCode(request.getQuestionCode());

        ProgrammingQuestion question = programmingQuestionMapper.toEntity(request);
        question.setCourse(course);
        question.setQuestionCode(questionCode);
        question.setDifficulty(resolveDifficulty(request.getDifficulty()));
        question.setAllowedLanguages(String.join(",", resolveLanguages(request.getAllowedLanguages())));

        applyTestCases(question, request.getTestCases());

        ProgrammingQuestion saved = programmingQuestionRepository.save(question);

        ProgrammingQuestionResponse response = toResponse(saved);
        response.setAllowedLanguages(splitLanguages(saved.getAllowedLanguages()));
        return response;
    }

    @Override
    public ProgrammingQuestionResponse updateProgrammingQuestion(
            UUID questionId, ProgrammingQuestionRequest request) {

        ProgrammingQuestion question = findProgrammingQuestion(questionId);

        question.setTitle(request.getTitle());
        question.setProblemStatement(request.getProblemStatement());
        question.setDifficulty(resolveDifficulty(request.getDifficulty()));
        question.setAllowedLanguages(String.join(",", resolveLanguages(request.getAllowedLanguages())));
        question.setInputFormat(request.getInputFormat());
        question.setOutputFormat(request.getOutputFormat());
        question.setConstraints(request.getConstraints());
        question.setSampleInput(request.getSampleInput());
        question.setSampleOutput(request.getSampleOutput());
        question.setDisplayOrder(resolveDisplayOrder(request.getDisplayOrder()));

        // Replace test cases wholesale
        question.getTestCases().clear();
        applyTestCases(question, request.getTestCases());

        ProgrammingQuestion saved = programmingQuestionRepository.save(question);

        ProgrammingQuestionResponse response = toResponse(saved);
        response.setAllowedLanguages(splitLanguages(saved.getAllowedLanguages()));
        return response;
    }

    @Override
    public void deleteProgrammingQuestion(UUID questionId) {

        ProgrammingQuestion question = findProgrammingQuestion(questionId);
        programmingQuestionRepository.delete(question);
    }

    @Override
    @Transactional
    public ProgrammingQuestionResponse getProgrammingQuestion(UUID questionId, User requester) {

        ProgrammingQuestion question = findProgrammingQuestion(questionId);

        ProgrammingQuestionResponse response = toResponse(question);
        response.setAllowedLanguages(splitLanguages(question.getAllowedLanguages()));

        if (!canViewFullContent(requester)) {
            sanitizeForStudent(response);
        }
        return response;
    }

    @Override
    @Transactional
    public List<ProgrammingQuestionResponse> getProgrammingQuestionsByCourse(
            UUID courseId, User requester) {

        boolean includeHidden = canViewFullContent(requester);

        // Load the requesting student's latest submission per question in one
        // query so the solved/attempted status comes from stored submission
        // data, never from client-side state.
        Map<UUID, ProgrammingQuestionSubmission> latestSubmissionByQuestion =
                resolveLatestSubmissions(courseId, requester);

        return programmingQuestionRepository
                .findByCourseIdOrderByDisplayOrderAsc(courseId)
                .stream()
                .map(question -> {
                    ProgrammingQuestionResponse response = toResponse(question);
                    response.setAllowedLanguages(splitLanguages(question.getAllowedLanguages()));
                    if (!includeHidden) {
                        sanitizeForStudent(response);
                        ProgrammingQuestionSubmission latest =
                                latestSubmissionByQuestion.get(question.getId());
                        response.setStudentStatus(resolveStudentStatus(latest));
                    } else {
                        response.setStudentStatus(null);
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public CodeExecutionResponse run(UUID questionId, ProgrammingQuestionRunRequest request) {

        ProgrammingQuestion question = findProgrammingQuestion(questionId);
        String language = normalizeLanguage(request.getLanguage());
        validateLanguage(question, language);

        return codeExecutionService.execute(CodeExecutionRequest.builder()
                .language(language)
                .code(request.getCode())
                .input(request.getInput() == null ? "" : request.getInput())
                .build());
    }

    @Override
    public ProgrammingQuestionSubmitResponse submit(
            UUID questionId, ProgrammingQuestionSubmitRequest request, User requester) {

        ProgrammingQuestion question = findProgrammingQuestion(questionId);
        String language = normalizeLanguage(request.getLanguage());
        validateLanguage(question, language);

        List<ProgrammingTestCase> testCases = question.getTestCases();
        int total = testCases.size();

        List<ProgrammingSubmissionCaseResult> caseResults = new ArrayList<>();
        long totalExecutionTimeMs = 0;
        int passed = 0;
        boolean anyCompilationError = false;
        boolean anyTimeLimit = false;
        boolean anyRuntimeError = false;

        for (int i = 0; i < total; i++) {
            ProgrammingTestCase testCase = testCases.get(i);

            CodeExecutionResponse result = codeExecutionService.execute(CodeExecutionRequest.builder()
                    .language(language)
                    .code(request.getCode())
                    .input(testCase.getInput() == null ? "" : testCase.getInput())
                    .build());

            boolean isPassed = result.isSuccess()
                    && outputsMatch(result.getOutput(), testCase.getExpectedOutput());

            boolean isHidden = !Boolean.TRUE.equals(testCase.getIsPublic());
            if (isPassed) passed++;

            long execTime = Math.max(0, result.getExecutionTime());
            totalExecutionTimeMs += execTime;

            if (!result.isSuccess()) {
                if (isCompilationFailure(result.getError())) anyCompilationError = true;
                else if (isTimeoutFailure(result.getError())) anyTimeLimit = true;
                else anyRuntimeError = true;
            }

            String message;
            if (result.isSuccess() && !isPassed) {
                message = isHidden ? "Failed (hidden test case)" : "Wrong answer";
            } else if (isCompilationFailure(result.getError())) {
                message = "Compilation error";
            } else if (isTimeoutFailure(result.getError())) {
                message = "Time limit exceeded";
            } else if (!result.isSuccess()) {
                message = "Runtime error";
            } else {
                message = "Passed";
            }

            caseResults.add(ProgrammingSubmissionCaseResult.builder()
                    .caseNumber(i + 1)
                    .passed(isPassed)
                    .hidden(isHidden)
                    .executionTimeMs(execTime)
                    .message(message)
                    .build());
        }

        ProgrammingSubmissionStatus status = resolveSubmissionStatus(
                total, passed, anyCompilationError, anyTimeLimit, anyRuntimeError);
        boolean accepted = status == ProgrammingSubmissionStatus.ACCEPTED;
        Long memoryKb = null; // local executors do not measure child-process memory

        ProgrammingQuestionSubmission submission = requester == null
                ? null
                : programmingQuestionSubmissionRepository.save(
                        ProgrammingQuestionSubmission.builder()
                                .user(requester)
                                .question(question)
                                .language(language)
                                .code(request.getCode())
                                .status(status)
                                .accepted(accepted)
                                .totalTestCases(total)
                                .passedTestCases(passed)
                                .executionTimeMs(totalExecutionTimeMs)
                                .memoryKb(memoryKb)
                                .submittedAt(LocalDateTime.now())
                                .build());

        return ProgrammingQuestionSubmitResponse.builder()
                .questionId(question.getId())
                .submissionId(submission == null ? null : submission.getId())
                .title(question.getTitle())
                .language(language)
                .accepted(accepted)
                .totalTestCases(total)
                .passedTestCases(passed)
                .failedTestCases(total - passed)
                .totalExecutionTimeMs(totalExecutionTimeMs)
                .memoryKb(memoryKb)
                .verdict(status.name())
                .caseResults(caseResults)
                .build();
    }

    @Override
    public List<ProgrammingQuestionSubmissionResponse> getProgrammingQuestionSubmissions(
            UUID questionId, User requester) {

        if (requester == null) {
            return List.of();
        }
        findProgrammingQuestion(questionId);
        return programmingQuestionSubmissionRepository
                .findByQuestionIdAndUserIdOrderBySubmittedAtDesc(questionId, requester.getId())
                .stream()
                .map(programmingQuestionMapper::toSubmissionResponse)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private ProgrammingQuestionResponse toResponse(ProgrammingQuestion question) {
        return programmingQuestionMapper.toResponse(question);
    }

    private void applyTestCases(ProgrammingQuestion question, List<ProgrammingTestCaseRequest> requests) {

        for (ProgrammingTestCaseRequest testCaseRequest : requests) {
            ProgrammingTestCase testCase =
                    programmingQuestionMapper.toTestCaseEntity(testCaseRequest);
            testCase.setQuestion(question);
            testCase.setIsPublic(Boolean.TRUE.equals(testCaseRequest.getIsPublic()));
            testCase.setDisplayOrder(resolveDisplayOrder(testCaseRequest.getDisplayOrder()));
            question.getTestCases().add(testCase);
        }
    }

    private ProgrammingQuestion findProgrammingQuestion(UUID questionId) {
        return programmingQuestionRepository.findById(questionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "ProgrammingQuestion", "id", questionId));
    }

    private String resolveUniqueQuestionCode(String requested) {
        if (requested != null && !requested.isBlank()) {
            if (programmingQuestionRepository.existsByQuestionCode(requested)) {
                throw new IllegalArgumentException(
                        "Programming question code already exists: " + requested);
            }
            return requested;
        }
        return generateCode("PRQ", code ->
                programmingQuestionRepository.existsByQuestionCode(code));
    }

    private String generateCode(String prefix, java.util.function.Predicate<String> exists) {
        String code;
        do {
            code = prefix + "-" + Long.toString(System.currentTimeMillis(), 36)
                    .toUpperCase() + "-" + (int) (Math.random() * 900 + 100);
        } while (exists.test(code));
        return code;
    }

    private ProgrammingQuestionDifficulty resolveDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank()) {
            return DEFAULT_DIFFICULTY;
        }
        try {
            return ProgrammingQuestionDifficulty.valueOf(difficulty.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Invalid difficulty: " + difficulty
                            + " (allowed: EASY, MEDIUM, HARD)");
        }
    }

    private Set<String> resolveLanguages(List<String> allowedLanguages) {
        Set<String> supported = new LinkedHashSet<>(
                executorFactory.supportedLanguages());
        if (allowedLanguages == null || allowedLanguages.isEmpty()) {
            return supported;
        }
        Set<String> requested = allowedLanguages.stream()
                .map(this::normalizeLanguage)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (String lang : requested) {
            if (!supported.contains(lang)) {
                throw new IllegalArgumentException(
                        "Unsupported language: " + lang
                                + " (supported: "
                                + String.join(", ", supported) + ")");
            }
        }
        return requested;
    }

    private List<String> splitLanguages(String stored) {
        if (stored == null || stored.isBlank()) {
            return List.of();
        }
        return Arrays.stream(stored.split(","))
                .filter(s -> !s.isBlank())
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            throw new IllegalArgumentException("Programming language must not be empty");
        }
        return language.trim().toUpperCase(Locale.ROOT);
    }

    private void validateLanguage(ProgrammingQuestion question, String language) {
        Set<String> allowed = splitLanguages(question.getAllowedLanguages())
                .stream()
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        if (!allowed.contains(language)) {
            throw new IllegalArgumentException(
                    "Language '" + language + "' is not allowed for this question"
                            + " (allowed: " + String.join(", ", allowed) + ")");
        }
        if (executorFactory.getExecutor(language) == null) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
    }

    private Integer resolveDisplayOrder(Integer displayOrder) {
        return displayOrder == null ? 1 : displayOrder;
    }

    private boolean canViewFullContent(User user) {
        if (user == null || user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .map(Role::getRoleName)
                .anyMatch(name -> name.equals("SUPER_ADMIN") || name.equals("STAFF"));
    }

    /**
     * Hidden test cases must never be exposed to students.
     * Public test cases (input + expected output) stay visible so students
     * can sanity-check their solutions before submitting.
     */
    private void sanitizeForStudent(ProgrammingQuestionResponse response) {
        if (response.getTestCases() == null) {
            return;
        }
        response.setTestCases(
                response.getTestCases().stream()
                        .filter(tc -> Boolean.TRUE.equals(tc.getIsPublic()))
                        .collect(Collectors.toList()));
    }

    private boolean outputsMatch(String actual, String expected) {
        String normalizedActual = normalizeOutput(actual);
        String normalizedExpected = normalizeOutput(expected);
        return normalizedActual.equals(normalizedExpected);
    }

    // ------------------------------------------------------------------
    // Submission status helpers
    // ------------------------------------------------------------------

    /**
     * Only students get a persisted-storage-backed status. Staff requests
     * (and anonymous/none) simply get an empty map.
     */
    private Map<UUID, ProgrammingQuestionSubmission> resolveLatestSubmissions(
            UUID courseId, User requester) {

        if (requester == null || canViewFullContent(requester)) {
            return Map.of();
        }
        return programmingQuestionSubmissionRepository
                .findByUserIdAndQuestion_CourseIdOrderBySubmittedAtDesc(
                        requester.getId(), courseId)
                .stream()
                .collect(Collectors.toMap(
                        submission -> submission.getQuestion().getId(),
                        Function.identity(),
                        (first, second) -> first));
    }

    private String resolveStudentStatus(ProgrammingQuestionSubmission latest) {
        if (latest == null) {
            return "NOT_ATTEMPTED";
        }
        return Boolean.TRUE.equals(latest.getAccepted())
                ? "ACCEPTED"
                : "ATTEMPTED";
    }

    private ProgrammingSubmissionStatus resolveSubmissionStatus(
            int total, int passed,
            boolean anyCompilationError,
            boolean anyTimeLimit,
            boolean anyRuntimeError) {

        if (anyCompilationError) {
            return ProgrammingSubmissionStatus.COMPILATION_ERROR;
        }
        if (anyTimeLimit) {
            return ProgrammingSubmissionStatus.TIME_LIMIT_EXCEEDED;
        }
        if (anyRuntimeError) {
            return ProgrammingSubmissionStatus.RUNTIME_ERROR;
        }
        return (total > 0 && passed == total)
                ? ProgrammingSubmissionStatus.ACCEPTED
                : ProgrammingSubmissionStatus.WRONG_ANSWER;
    }

    private boolean isCompilationFailure(String error) {
        return error != null
                && error.toLowerCase(Locale.ROOT).contains("compilation");
    }

    private boolean isTimeoutFailure(String error) {
        return error != null
                && error.toLowerCase(Locale.ROOT).contains("timed out");
    }

    private String normalizeOutput(String value) {
        if (value == null) {
            return "";
        }
        // Trim leading/trailing whitespace and normalize internal line endings
        return value
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }
}