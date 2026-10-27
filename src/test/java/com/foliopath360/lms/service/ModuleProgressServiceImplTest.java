package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.response.ModuleProgressItemResponse;
import com.foliopath360.lms.dto.response.ModuleProgressResponse;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.exception.ModuleLockedException;
import com.foliopath360.lms.repository.*;
import com.foliopath360.lms.service.impl.ModuleProgressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sequential module unlocking driven by mock test completion.
 *
 * <p>Pure Mockito unit tests - no database - so the derivation rules can be
 * asserted exhaustively:
 *
 * <pre>
 * Enrollment -&gt; Module 1 OPEN -&gt; pass Mock Test 1 -&gt; Module 1 COMPLETED
 *            -&gt; Module 2 OPEN  -&gt; pass Mock Test 2 -&gt; Module 2 COMPLETED
 *            -&gt; ... -&gt; final test passed -&gt; 100%
 * </pre>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ModuleProgressServiceImplTest {

    private static final UUID COURSE_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private CourseModuleRepository courseModuleRepository;
    @Mock private MockTestRepository mockTestRepository;
    @Mock private MockTestAttemptRepository mockTestAttemptRepository;
    @Mock private EnrollmentRepository enrollmentRepository;
    @Mock private LessonProgressRepository lessonProgressRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private StudentProfileRepository studentProfileRepository;

    private ModuleProgressService service;
    private User student;
    private Course course;
    private List<CourseModule> modules;

    @BeforeEach
    void setUp() {

        service = new ModuleProgressServiceImpl(
                courseModuleRepository,
                mockTestRepository,
                mockTestAttemptRepository,
                enrollmentRepository,
                lessonProgressRepository,
                lessonRepository,
                studentProfileRepository);

        student = User.builder()
                .id(USER_ID)
                .roles(new HashSet<>(Set.of(
                        Role.builder().id(1L).roleName("STUDENT").build())))
                .build();

        course = Course.builder().id(COURSE_ID).title("Java Mastery").build();

        modules = List.of(
                module(1, "Java Basics"),
                module(2, "OOP Concepts"),
                module(3, "Collections"),
                module(4, "Exception Handling"),
                module(5, "Advanced Java"));

        // every module has its own gating mock test by default
        lenient().when(mockTestRepository.findByCourseIdOrderByModuleOrder(COURSE_ID))
                .thenReturn(modules.stream().map(this::mockTestFor).collect(
                        Collectors.toList()));

        lenient().when(mockTestAttemptRepository
                        .findPassedAttemptsByUserIdAndCourseId(any(), any()))
                .thenReturn(List.of());

        lenient().when(lessonProgressRepository
                .findByUserIdAndLessonModuleCourseId(any(), any()))
                .thenReturn(List.of());

        lenient().when(enrollmentRepository
                        .findByUserIdAndCourseId(USER_ID, COURSE_ID))
                .thenReturn(Optional.of(enrollment(EnrollmentStatus.ENROLLED)));

        lenient().when(enrollmentRepository
                        .existsByUserIdAndCourseIdAndStatusNot(
                                eq(USER_ID), eq(COURSE_ID),
                                eq(EnrollmentStatus.DROPPED)))
                .thenReturn(true);

        lenient().when(studentProfileRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(
                        StudentProfile.builder().studentCode("S000003").build()));

        lenient().when(courseModuleRepository
                        .findByCourseIdAndStatusOrderByDisplayOrderAsc(
                                COURSE_ID, ModuleStatus.ACTIVE))
                .thenReturn(modules);
    }

    // ------------------------------------------------------------------
    // 1. Initial enrollment
    // ------------------------------------------------------------------

    @Test
    @DisplayName("On enrollment only the first module is OPEN and the rest are LOCKED")
    void onlyFirstModuleIsOpenOnEnrollment() {

        ModuleProgressResponse progress = service.getCourseProgress(student, COURSE_ID);

        assertThat(statuses(progress)).containsExactly(
                "OPEN", "LOCKED", "LOCKED", "LOCKED", "LOCKED");

        assertThat(progress.getTotalModules()).isEqualTo(5);
        assertThat(progress.getCompletedModules()).isZero();
        assertThat(progress.getProgressPercentage()).isZero();
        assertThat(progress.getCurrentModule()).isEqualTo(1);
        assertThat(progress.getCompleted()).isFalse();
        assertThat(progress.getStudentId()).isEqualTo("S000003");
    }

    // ------------------------------------------------------------------
    // 2. Mock test as module completion
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Passing module 1's mock test completes it and unlocks module 2")
    void passingMockTestCompletesModuleAndUnlocksNext() {

        givenPassedAttempts(passed(modules.get(0)));

        ModuleProgressResponse progress = service.getCourseProgress(student, COURSE_ID);

        assertThat(statuses(progress)).containsExactly(
                "COMPLETED", "OPEN", "LOCKED", "LOCKED", "LOCKED");

        assertThat(progress.getCompletedModules()).isEqualTo(1);
        assertThat(progress.getProgressPercentage()).isEqualTo(20);
        assertThat(progress.getCurrentModule()).isEqualTo(2);

        ModuleProgressItemResponse first = progress.getModules().get(0);
        assertThat(first.getMockTestCompleted()).isTrue();
        assertThat(first.getModuleName()).isEqualTo("Java Basics");
        assertThat(first.getCompletedAt()).isNotNull();
        assertThat(first.getUnlocked()).isTrue();
    }

    @Test
    @DisplayName("A failed attempt neither completes the module nor advances progress")
    void failedAttemptDoesNotCompleteModuleOrAdvanceProgress() {

        MockTestAttempt failed = MockTestAttempt.builder()
                .user(student)
                .mockTest(mockTestFor(modules.get(0)))
                .passed(false)
                .percentage(20)
                .submittedAt(LocalDateTime.now())
                .build();

        // only PASSED attempts are ever fetched back for derivation
        when(mockTestAttemptRepository.findPassedAttemptsByUserIdAndCourseId(
                USER_ID, COURSE_ID)).thenReturn(List.of());

        ModuleProgressResponse progress = service.getCourseProgress(student, COURSE_ID);

        assertThat(statuses(progress)).containsExactly(
                "OPEN", "LOCKED", "LOCKED", "LOCKED", "LOCKED");
        assertThat(progress.getProgressPercentage()).isZero();
        assertThat(progress.getCompletedModules()).isZero();
        assertThat(failed.getPassed()).isFalse();
    }

    // ------------------------------------------------------------------
    // 3. Progress formula
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Progress % = completed modules / total modules * 100 (2 of 5 = 40)")
    void progressFollowsCompletedModulesOverTotalModules() {

        givenPassedAttempts(passed(modules.get(0)), passed(modules.get(1)));

        ModuleProgressResponse progress = service.getCourseProgress(student, COURSE_ID);

        assertThat(progress.getTotalModules()).isEqualTo(5);
        assertThat(progress.getCompletedModules()).isEqualTo(2);
        assertThat(progress.getProgressPercentage()).isEqualTo(40);
        assertThat(progress.getCurrentModule()).isEqualTo(3);

        assertThat(statuses(progress)).containsExactly(
                "COMPLETED", "COMPLETED", "OPEN", "LOCKED", "LOCKED");
    }

    @Test
    @DisplayName("Viewing lessons never moves progress - only passed mock tests do")
    void viewingLessonsDoesNotAffectProgress() {

        givenPassedAttempts(passed(modules.get(0)));

        // the student has read every lesson of module 1 but not passed the test
        Lesson lesson = Lesson.builder()
                .id(UUID.randomUUID())
                .module(modules.get(0))
                .lessonCode("LES-1")
                .title("Intro")
                .displayOrder(1)
                .build();

        when(lessonProgressRepository.findByUserIdAndLessonModuleCourseId(
                USER_ID, COURSE_ID)).thenReturn(List.of(
                LessonProgress.builder()
                        .user(student)
                        .lesson(lesson)
                        .completedAt(LocalDateTime.now())
                        .build()));

        ModuleProgressResponse progress = service.getCourseProgress(student, COURSE_ID);

        assertThat(progress.getCompletedModules()).isEqualTo(1);
        assertThat(progress.getProgressPercentage()).isEqualTo(20);
        // module 2 stays locked: reading module 1's lesson did nothing
        assertThat(statuses(progress)).containsExactly(
                "COMPLETED", "OPEN", "LOCKED", "LOCKED", "LOCKED");
    }

    // ------------------------------------------------------------------
    // 4. End state
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Passing the final mock test yields 100% with every module COMPLETED")
    void finalMockTestGivesHundredPercent() {

        givenPassedAttempts(modules.stream().map(this::passed).toArray(
                MockTestAttempt[]::new));

        ModuleProgressResponse progress = service.getCourseProgress(student, COURSE_ID);

        assertThat(statuses(progress)).containsExactly(
                "COMPLETED", "COMPLETED", "COMPLETED", "COMPLETED", "COMPLETED");
        assertThat(progress.getCompletedModules()).isEqualTo(5);
        assertThat(progress.getProgressPercentage()).isEqualTo(100);
        assertThat(progress.getCompleted()).isTrue();
        // nothing left to work on
        assertThat(progress.getCurrentModule()).isNull();
    }

    // ------------------------------------------------------------------
    // 5. Locked modules cannot be reached from the frontend
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A locked module is rejected with ModuleLockedException (HTTP 403)")
    void lockedModuleIsRejected() {

        givenPassedAttempts(passed(modules.get(0)));

        // only module 1 has been passed, so modules 3..5 are still locked
        assertThatThrownBy(() ->
                service.requireModuleAccess(student, modules.get(3)))
                .isInstanceOf(ModuleLockedException.class)
                .hasMessageContaining("Exception Handling")
                .hasMessageContaining("locked");
    }

    @Test
    @DisplayName("The OPEN and COMPLETED modules are reachable")
    void unlockedModulesAreReachable() {

        givenPassedAttempts(passed(modules.get(0)));

        service.requireModuleAccess(student, modules.get(0)); // COMPLETED
        service.requireModuleAccess(student, modules.get(1)); // OPEN

        assertThat(service.canAccessModule(student, modules.get(0))).isTrue();
        assertThat(service.canAccessModule(student, modules.get(1))).isTrue();
        assertThat(service.canAccessModule(student, modules.get(2))).isFalse();
        assertThat(service.canAccessModule(student, modules.get(4))).isFalse();
    }

    @Test
    @DisplayName("A student not enrolled in the course cannot open any module")
    void notEnrolledStudentCannotOpenModules() {

        when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(
                USER_ID, COURSE_ID, EnrollmentStatus.DROPPED)).thenReturn(false);

        assertThatThrownBy(() ->
                service.requireModuleAccess(student, modules.get(0)))
                .isInstanceOf(ModuleLockedException.class)
                .hasMessageContaining("not enrolled");

        assertThat(service.canAccessModule(student, modules.get(0))).isFalse();
        assertThat(service.isModuleUnlocked(student, modules.get(0))).isFalse();
    }

    @Test
    @DisplayName("A dropped enrollment re-locks every module of the course")
    void droppedEnrollmentLocksEverything() {

        when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID))
                .thenReturn(Optional.of(enrollment(EnrollmentStatus.DROPPED)));
        when(enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(
                USER_ID, COURSE_ID, EnrollmentStatus.DROPPED)).thenReturn(false);

        givenPassedAttempts(passed(modules.get(0)));

        assertThatThrownBy(() ->
                service.requireModuleAccess(student, modules.get(0)))
                .isInstanceOf(ModuleLockedException.class)
                .hasMessageContaining("dropped");

        assertThat(service.canAccessModule(student, modules.get(0))).isFalse();
    }

    @Test
    @DisplayName("Staff and admins are never blocked by the sequential gate")
    void staffBypassTheSequentialGate() {

        givenPassedAttempts();

        User admin = User.builder().id(UUID.randomUUID()).roles(new HashSet<>(
                Set.of(Role.builder().id(2L).roleName("SUPER_ADMIN").build())))
                .build();

        service.requireModuleAccess(admin, modules.get(4));
        assertThat(service.canAccessModule(admin, modules.get(4))).isTrue();
    }

    // ------------------------------------------------------------------
    // 6. Persisted enrollment progress
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Enrollment progress is persisted from completed mock tests only")
    void refreshPersistsMockTestDrivenPercentage() {

        givenPassedAttempts(passed(modules.get(0)), passed(modules.get(1)));

        service.refreshEnrollmentProgress(USER_ID, COURSE_ID);

        ArgumentCaptor<Enrollment> saved = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(saved.capture());

        assertThat(saved.getValue().getProgressPercentage()).isEqualTo(40);
        assertThat(saved.getValue().getStatus())
                .isEqualTo(EnrollmentStatus.IN_PROGRESS);
        assertThat(saved.getValue().getCompletedAt()).isNull();
    }

    @Test
    @DisplayName("100% marks the enrollment COMPLETED and stamps completedAt")
    void hundredPercentCompletesEnrollment() {

        givenPassedAttempts(modules.stream().map(this::passed).toArray(
                MockTestAttempt[]::new));

        service.refreshEnrollmentProgress(USER_ID, COURSE_ID);

        ArgumentCaptor<Enrollment> saved = ArgumentCaptor.forClass(Enrollment.class);
        verify(enrollmentRepository).save(saved.capture());

        assertThat(saved.getValue().getProgressPercentage()).isEqualTo(100);
        assertThat(saved.getValue().getStatus())
                .isEqualTo(EnrollmentStatus.COMPLETED);
        assertThat(saved.getValue().getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("A dropped enrollment is never mutated by a refresh")
    void droppedEnrollmentIsLeftAlone() {

        givenPassedAttempts(passed(modules.get(0)));

        when(enrollmentRepository.findByUserIdAndCourseId(USER_ID, COURSE_ID))
                .thenReturn(Optional.of(enrollment(EnrollmentStatus.DROPPED)));

        service.refreshEnrollmentProgress(USER_ID, COURSE_ID);

        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("Grading a mock test refreshes the enrollment and unlocks the next module")
    void onMockTestAttemptUnlocksNextModule() {

        givenPassedAttempts(passed(modules.get(0)));

        ModuleProgressResponse progress =
                service.onMockTestAttempt(student, modules.get(0));

        verify(enrollmentRepository, atLeastOnce()).save(any(Enrollment.class));
        assertThat(statuses(progress)).containsExactly(
                "COMPLETED", "OPEN", "LOCKED", "LOCKED", "LOCKED");
        assertThat(progress.getProgressPercentage()).isEqualTo(20);
    }

    // ------------------------------------------------------------------
    // 7. Liveness
    // ------------------------------------------------------------------

    @Test
    @DisplayName("A module with no mock test is auto-completed so the sequence cannot dead-end")
    void moduleWithoutMockTestAutoCompletes() {

        // module 2 has no mock test attached at all
        when(mockTestRepository.findByCourseIdOrderByModuleOrder(COURSE_ID))
                .thenReturn(List.of(mockTestFor(modules.get(0)),
                        mockTestFor(modules.get(2)),
                        mockTestFor(modules.get(3)),
                        mockTestFor(modules.get(4))));

        givenPassedAttempts(passed(modules.get(0)));

        ModuleProgressResponse progress = service.getCourseProgress(student, COURSE_ID);

        assertThat(statuses(progress)).containsExactly(
                "COMPLETED", "COMPLETED", "OPEN", "LOCKED", "LOCKED");

        ModuleProgressItemResponse autoCompleted = progress.getModules().get(1);
        assertThat(autoCompleted.getHasMockTest()).isFalse();
        assertThat(autoCompleted.getMockTestId()).isNull();
        assertThat(progress.getCurrentModule()).isEqualTo(3);
    }

    @Test
    @DisplayName("A course with no modules reports 0% rather than dividing by zero")
    void emptyCourseIsZeroPercent() {

        when(courseModuleRepository.findByCourseIdAndStatusOrderByDisplayOrderAsc(
                COURSE_ID, ModuleStatus.ACTIVE)).thenReturn(List.of());

        ModuleProgressResponse progress = service.getCourseProgress(student, COURSE_ID);

        assertThat(progress.getTotalModules()).isZero();
        assertThat(progress.getProgressPercentage()).isZero();
        assertThat(progress.getCompleted()).isFalse();
        assertThat(progress.getModules()).isEmpty();
    }

    @Test
    @DisplayName("Module numbering is 1-based and follows display order")
    void modulesAreNumberedFromOne() {

        givenPassedAttempts(passed(modules.get(0)), passed(modules.get(1)));

        ModuleProgressResponse progress = service.getCourseProgress(student, COURSE_ID);

        assertThat(progress.getModules())
                .extracting(ModuleProgressItemResponse::getModuleNumber)
                .containsExactly(1, 2, 3, 4, 5);
        assertThat(progress.getModules())
                .extracting(ModuleProgressItemResponse::getModuleName)
                .containsExactly("Java Basics", "OOP Concepts", "Collections",
                        "Exception Handling", "Advanced Java");
        assertThat(progress.getModules())
                .extracting(ModuleProgressItemResponse::getMockTestCompleted)
                .containsExactly(true, true, false, false, false);
        assertThat(progress.getModules())
                .extracting(ModuleProgressItemResponse::getUnlocked)
                .containsExactly(true, true, true, false, false);
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private void givenPassedAttempts(MockTestAttempt... attempts) {
        when(mockTestAttemptRepository.findPassedAttemptsByUserIdAndCourseId(
                USER_ID, COURSE_ID)).thenReturn(List.of(attempts));
    }

    private List<String> statuses(ModuleProgressResponse progress) {
        return progress.getModules().stream()
                .map(ModuleProgressItemResponse::getStatus)
                .collect(Collectors.toList());
    }

    private CourseModule module(int number, String title) {

        CourseModule module = CourseModule.builder()
                .id(UUID.randomUUID())
                .course(course)
                .moduleCode("MOD-" + number)
                .title(title)
                .displayOrder(number)
                .status(ModuleStatus.ACTIVE)
                .build();
        module.getLessons().clear();
        return module;
    }

    private MockTest mockTestFor(CourseModule module) {
        return MockTest.builder()
                .id(UUID.randomUUID())
                .module(module)
                .testCode("MTS-" + module.getDisplayOrder())
                .title(module.getTitle() + " Mock Test")
                .displayOrder(1)
                .build();
    }

    private MockTestAttempt passed(CourseModule module) {
        return MockTestAttempt.builder()
                .user(student)
                .mockTest(mockTestFor(module))
                .passed(true)
                .percentage(90)
                .submittedAt(LocalDateTime.now())
                .build();
    }

    private Enrollment enrollment(EnrollmentStatus status) {
        return Enrollment.builder()
                .id(UUID.randomUUID())
                .user(student)
                .course(course)
                .status(status)
                .progressPercentage(0)
                .enrolledAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Course modules are only counted when ACTIVE")
    void inactiveModulesAreExcludedFromTheSequence() {
        // getCourseProgress queries the ACTIVE-scoped sequence directly, so an
        // INACTIVE module never enters the progression or the total
        when(courseModuleRepository.findByCourseIdAndStatusOrderByDisplayOrderAsc(
                eq(COURSE_ID), eq(ModuleStatus.ACTIVE)))
                .thenReturn(modules.subList(0, 3));

        givenPassedAttempts(passed(modules.get(0)));

        ModuleProgressResponse progress = service.getCourseProgress(student, COURSE_ID);

        assertThat(progress.getTotalModules()).isEqualTo(3);
        assertThat(progress.getProgressPercentage()).isEqualTo(33);
        assertThat(statuses(progress)).containsExactly("COMPLETED", "OPEN", "LOCKED");
    }
}
