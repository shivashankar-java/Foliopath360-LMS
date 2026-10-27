package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.MockTestAttemptRequest;
import com.foliopath360.lms.dto.response.MockTestAttemptResponse;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.exception.ModuleLockedException;
import com.foliopath360.lms.mapper.MockTestMapper;
import com.foliopath360.lms.repository.*;
import com.foliopath360.lms.service.impl.MockTestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * A mock test attempt is the module-completion signal, so grading an attempt
 * is what drives the sequential unlock. These tests pin the wiring between
 * {@link MockTestServiceImpl} and {@link ModuleProgressService}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MockTestServiceImplProgressTest {

    @Mock private MockTestRepository mockTestRepository;
    @Mock private MockTestQuestionRepository mockTestQuestionRepository;
    @Mock private MockTestAttemptRepository mockTestAttemptRepository;
    @Mock private CourseModuleRepository courseModuleRepository;
    @Mock private ModuleProgressService moduleProgressService;
    @Mock private MockTestMapper mockTestMapper;

    private MockTestService service;
    private User student;
    private CourseModule module;
    private MockTest mockTest;

    @BeforeEach
    void setUp() {

        service = new MockTestServiceImpl(
                mockTestRepository,
                mockTestQuestionRepository,
                mockTestAttemptRepository,
                courseModuleRepository,
                moduleProgressService,
                mockTestMapper);

        student = User.builder()
                .id(UUID.randomUUID())
                .roles(new HashSet<>(Set.of(
                        Role.builder().id(1L).roleName("STUDENT").build())))
                .build();

        module = CourseModule.builder()
                .id(UUID.randomUUID())
                .course(Course.builder().id(UUID.randomUUID()).title("Java").build())
                .moduleCode("MOD-1")
                .title("Java Basics")
                .displayOrder(1)
                .status(ModuleStatus.ACTIVE)
                .build();

        mockTest = MockTest.builder()
                .id(UUID.randomUUID())
                .module(module)
                .testCode("MTS-1")
                .title("Java Basics Mock Test")
                .passPercentage(50)
                .displayOrder(1)
                .build();

        mockTest.getQuestions().addAll(List.of(
                question("Q1", "A"), question("Q2", "B")));

        lenient().when(mockTestRepository.findById(mockTest.getId()))
                .thenReturn(Optional.of(mockTest));
        lenient().when(courseModuleRepository.findById(module.getId()))
                .thenReturn(Optional.of(module));
        lenient().when(mockTestAttemptRepository
                        .existsByUserIdAndMockTestId(any(), any()))
                .thenReturn(false);
        lenient().when(mockTestAttemptRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(mockTestMapper.toOptionResponse(any()))
                .thenAnswer(invocation -> null);
    }

    @Test
    @DisplayName("Submitting an attempt for a locked module is rejected")
    void lockedModuleAttemptIsRejected() {

        org.mockito.Mockito.doThrow(new ModuleLockedException(
                        module.getId(), module.getTitle(), "Module is locked."))
                .when(moduleProgressService)
                .requireModuleAccess(eq(student), eq(module));

        assertThatThrownBy(() -> service.submitAttempt(
                student, mockTest.getId(), answers("A", "B")))
                .isInstanceOf(ModuleLockedException.class);

        // no attempt is ever recorded for a module the student has not reached
        verify(mockTestAttemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("A passing attempt completes the module and triggers the next unlock")
    void passingAttemptTriggersModuleUnlock() {

        MockTestAttemptResponse response =
                service.submitAttempt(student, mockTest.getId(), answers("A", "B"));

        assertThat(response.getPassed()).isTrue();
        assertThat(response.getScore()).isEqualTo(2);
        assertThat(response.getPercentage()).isEqualTo(100);

        // this is what marks the module COMPLETED and opens the next one
        verify(moduleProgressService).onMockTestAttempt(student, module);
    }

    @Test
    @DisplayName("A failing attempt is graded but never advances the sequence")
    void failingAttemptDoesNotTriggerModuleUnlock() {

        // 1 of 2 correct = 50%, which meets the 50% pass mark, so use a
        // clearly wrong pair to drop below it
        MockTestAttemptResponse response =
                service.submitAttempt(student, mockTest.getId(), answers("C", "D"));

        assertThat(response.getPassed()).isFalse();
        assertThat(response.getPercentage()).isEqualTo(0);

        // still notified so the service can re-derive, but a failed attempt
        // leaves the module OPEN and the next one LOCKED
        verify(moduleProgressService).onMockTestAttempt(student, module);
    }

    @Test
    @DisplayName("A second attempt on the same test is refused")
    void secondAttemptIsRefused() {

        when(mockTestAttemptRepository.existsByUserIdAndMockTestId(
                student.getId(), mockTest.getId())).thenReturn(true);

        assertThatThrownBy(() -> service.submitAttempt(
                student, mockTest.getId(), answers("A", "B")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already attempted");

        verify(mockTestAttemptRepository, never()).save(any());
    }

    @Test
    @DisplayName("Locked modules expose no mock tests to an enrolled student")
    void lockedModuleExposesNoMockTests() {

        lenient().when(courseModuleRepository.findById(module.getId()))
                .thenReturn(Optional.of(module));
        when(moduleProgressService.canAccessModule(student, module))
                .thenReturn(false);

        assertThat(service.getMockTestsByModule(module.getId(), student)).isEmpty();
    }

    @Test
    @DisplayName("An unlocked module still lists its mock tests")
    void unlockedModuleListsMockTests() {

        when(mockTestRepository.findByModuleIdOrderByDisplayOrderAsc(module.getId()))
                .thenReturn(List.of(mockTest));
        when(moduleProgressService.canAccessModule(student, module))
                .thenReturn(true);
        when(mockTestMapper.toResponse(mockTest))
                .thenAnswer(invocation -> new com.foliopath360.lms.dto.response
                        .MockTestResponse());

        assertThat(service.getMockTestsByModule(module.getId(), student)).hasSize(1);
    }

    // ------------------------------------------------------------------

    private MockTestQuestion question(String code, String correctOption) {
        return MockTestQuestion.builder()
                .id(UUID.randomUUID())
                .mockTest(mockTestRef())
                .questionCode(code)
                .questionType(QuestionType.TEXT)
                .questionText("Question " + code)
                .correctOption(correctOption)
                .displayOrder(1)
                .build();
    }

    private MockTest mockTestRef() {
        return mockTest;
    }

    private MockTestAttemptRequest answers(String... options) {
        Map<String, String> answers = new LinkedHashMap<>();
        for (int i = 0; i < mockTest.getQuestions().size(); i++) {
            answers.put(
                    mockTest.getQuestions().get(i).getId().toString(),
                    options[i]);
        }
        return MockTestAttemptRequest.builder().answers(answers).build();
    }
}
