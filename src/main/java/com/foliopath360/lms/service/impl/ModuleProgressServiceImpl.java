package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.response.ModuleProgressItemResponse;
import com.foliopath360.lms.dto.response.ModuleProgressResponse;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.exception.ModuleLockedException;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.repository.*;
import com.foliopath360.lms.service.ModuleProgressService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Derives sequential module status from completed mock tests.
 *
 * <p>Nothing about a module's per-student state is persisted. It is always
 * recomputed from {@code mock_test_attempts} plus the module's position in
 * {@code course_modules.display_order}, which is what makes the rule
 * "progress comes only from passed mock tests" impossible to bypass or
 * desynchronise.
 *
 * <p>Progression rule:
 * <ol>
 *   <li>a module is COMPLETED when the student has a <em>passed</em> attempt
 *       on one of its mock tests;</li>
 *   <li>the first module that is not completed is OPEN;</li>
 *   <li>every module after the OPEN one is LOCKED.</li>
 * </ol>
 *
 * <p>A module with no mock test attached has nothing to pass, so it is
 * treated as completed - otherwise the sequence would dead-end and no
 * later module could ever unlock.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class ModuleProgressServiceImpl implements ModuleProgressService {

    private final CourseModuleRepository courseModuleRepository;
    private final MockTestRepository mockTestRepository;
    private final MockTestAttemptRepository mockTestAttemptRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final LessonRepository lessonRepository;
    private final StudentProfileRepository studentProfileRepository;

    // ------------------------------------------------------------------
    // Read API
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public ModuleProgressResponse getCourseProgress(User student, UUID courseId) {

        Enrollment enrollment = requireActiveEnrollment(
                student.getId(), courseId);

        return build(student, enrollment,
                courseModuleRepository.findByCourseIdAndStatusOrderByDisplayOrderAsc(
                        courseId, ModuleStatus.ACTIVE));
    }

    @Override
    @Transactional
    public ModuleProgressResponse getModuleProgress(User student, UUID moduleId) {

        CourseModule module = findModule(moduleId);

        Enrollment enrollment = requireActiveEnrollment(
                student.getId(), module.getCourse().getId());

        return build(student, enrollment,
                courseModuleRepository.findByCourseIdAndStatusOrderByDisplayOrderAsc(
                        module.getCourse().getId(), ModuleStatus.ACTIVE));
    }

    @Override
    @Transactional
    public List<ModuleProgressResponse> getMyCourseProgress(User student) {

        return enrollmentRepository.findByUserId(student.getId()).stream()
                .filter(e -> e.getStatus() != EnrollmentStatus.DROPPED)
                .map(e -> build(student, e,
                        courseModuleRepository
                                .findByCourseIdAndStatusOrderByDisplayOrderAsc(
                                        e.getCourse().getId(), ModuleStatus.ACTIVE)))
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // Write path
    // ------------------------------------------------------------------

    @Override
    public ModuleProgressResponse onMockTestAttempt(User student, CourseModule module) {

        UUID courseId = module.getCourse().getId();

        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(student.getId(), courseId)
                .orElse(null);

        // Only enrolled students progress. A non-enrolled attempt is graded
        // but must not create progress, so nothing is persisted here.
        if (enrollment == null || enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            return null;
        }

        refreshEnrollmentProgress(student.getId(), courseId);

        return build(student, enrollmentRepository
                        .findByUserIdAndCourseId(student.getId(), courseId)
                        .orElse(enrollment),
                courseModuleRepository.findByCourseIdAndStatusOrderByDisplayOrderAsc(
                        courseId, ModuleStatus.ACTIVE));
    }

    @Override
    public void refreshEnrollmentProgress(UUID userId, UUID courseId) {

        List<CourseModule> sequence = courseModuleRepository
                .findByCourseIdAndStatusOrderByDisplayOrderAsc(
                        courseId, ModuleStatus.ACTIVE);

        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(userId, courseId)
                .orElse(null);

        if (enrollment == null || enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            return;
        }

        Set<UUID> completed = completedModuleIds(userId, courseId, sequence);
        int total = sequence.size();

        int percentage = total == 0
                ? 0
                : (int) ((completed.size() * 100L) / total);

        enrollment.setProgressPercentage(percentage);

        if (percentage >= 100) {
            enrollment.setStatus(EnrollmentStatus.COMPLETED);
            if (enrollment.getCompletedAt() == null) {
                enrollment.setCompletedAt(LocalDateTime.now());
            }
        } else {
            enrollment.setStatus(completed.isEmpty()
                    ? EnrollmentStatus.ENROLLED
                    : EnrollmentStatus.IN_PROGRESS);
            enrollment.setCompletedAt(null);
        }

        enrollmentRepository.save(enrollment);
    }

    // ------------------------------------------------------------------
    // Access control
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public boolean canAccessModule(User requester, CourseModule module) {

        if (isPrivileged(requester)) {
            return true;
        }
        if (!isStudent(requester)) {
            return false;
        }
        // Enrollment is verified first: without one the module is not the
        // student's to open, whatever its position in the sequence.
        if (!enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(
                requester.getId(),
                module.getCourse().getId(),
                EnrollmentStatus.DROPPED)) {
            return false;
        }
        return statusOf(requester.getId(), module) != ModuleProgressStatus.LOCKED;
    }

    @Override
    @Transactional
    public void requireModuleAccess(User requester, CourseModule module) {

        if (isPrivileged(requester)) {
            return;
        }

        if (!isStudent(requester)) {
            throw new ModuleLockedException(module.getId(), module.getTitle(),
                    "You must be signed in as a student to access this module.");
        }

        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(
                        requester.getId(), module.getCourse().getId())
                .orElseThrow(() -> new ModuleLockedException(
                        module.getId(), module.getTitle(),
                        "You are not enrolled in this course. "
                                + "Enroll to unlock module \"" + module.getTitle()
                                + "\"."));

        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new ModuleLockedException(module.getId(), module.getTitle(),
                    "You have dropped this course. Re-enroll to access module \""
                            + module.getTitle() + "\".");
        }

        ModuleProgressStatus status = statusOf(requester.getId(), module);
        if (status == ModuleProgressStatus.LOCKED) {
            throw new ModuleLockedException(module.getId(), module.getTitle(),
                    "Module \"" + module.getTitle() + "\" is locked. "
                            + "Pass the previous module's mock test to unlock it.");
        }
    }

    /**
     * True when the student is actively enrolled in the module's own course
     * <em>and</em> the module is not locked for them.
     *
     * <p>Used by the lesson service, which still grants content access through
     * a parent course that embeds this course as a module; that inheritance is
     * checked separately and is unaffected by this gate.
     */
    @Override
    @Transactional
    public boolean isModuleUnlocked(User student, CourseModule module) {

        if (student == null || student.getId() == null) {
            return false;
        }
        return enrollmentRepository.existsByUserIdAndCourseIdAndStatusNot(
                student.getId(),
                module.getCourse().getId(),
                EnrollmentStatus.DROPPED)
                && statusOf(student.getId(), module) != ModuleProgressStatus.LOCKED;
    }

    /**
     * The module's status for the student, derived on demand.
     *
     * <p>A completed module stays COMPLETED regardless of its position; only
     * the first module that is <em>not</em> yet completed is OPEN, and every
     * module after it is LOCKED.
     */
    private ModuleProgressStatus statusOf(UUID userId, CourseModule module) {

        UUID courseId = module.getCourse().getId();

        List<CourseModule> sequence = courseModuleRepository
                .findByCourseIdAndStatusOrderByDisplayOrderAsc(
                        courseId, ModuleStatus.ACTIVE);

        Set<UUID> completed = completedModuleIds(userId, courseId, sequence);

        if (completed.contains(module.getId())) {
            return ModuleProgressStatus.COMPLETED;
        }

        return sequence.stream()
                .filter(candidate -> !completed.contains(candidate.getId()))
                .findFirst()
                .filter(first -> first.getId().equals(module.getId()))
                .isPresent()
                ? ModuleProgressStatus.OPEN
                : ModuleProgressStatus.LOCKED;
    }

    // ------------------------------------------------------------------
    // Derivation
    // ------------------------------------------------------------------

    /**
     * Modules the student has finished: those with a passed mock test attempt,
     * plus modules that have no mock test at all (nothing to pass).
     */
    private Set<UUID> completedModuleIds(
            UUID userId, UUID courseId, List<CourseModule> sequence) {

        if (sequence.isEmpty()) {
            return Set.of();
        }

        Set<UUID> withMockTest = mockTestRepository
                .findByCourseIdOrderByModuleOrder(courseId)
                .stream()
                .map(test -> test.getModule().getId())
                .collect(Collectors.toSet());

        Set<UUID> passed = mockTestAttemptRepository
                .findPassedAttemptsByUserIdAndCourseId(userId, courseId)
                .stream()
                .map(attempt -> attempt.getMockTest().getModule().getId())
                .collect(Collectors.toSet());

        return sequence.stream()
                .map(CourseModule::getId)
                .filter(id -> passed.contains(id) || !withMockTest.contains(id))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private ModuleProgressResponse build(
            User student, Enrollment enrollment, List<CourseModule> sequence) {

        UUID userId = student.getId();
        UUID courseId = enrollment.getCourse().getId();

        List<ModuleProgressItemResponse> modules = buildModules(
                userId, courseId, sequence);

        int total = modules.size();
        int completed = (int) modules.stream()
                .filter(m -> ModuleProgressStatus.COMPLETED.name()
                        .equals(m.getStatus()))
                .count();

        // first module that is not COMPLETED -> the student's current module
        Integer currentModule = null;
        for (int i = 0; i < modules.size(); i++) {
            if (!ModuleProgressStatus.COMPLETED.name()
                    .equals(modules.get(i).getStatus())) {
                currentModule = i + 1;
                break;
            }
        }

        int percentage = total == 0 ? 0 : (completed * 100) / total;

        return ModuleProgressResponse.builder()
                .studentId(studentProfileRepository.findByUserId(userId)
                        .map(StudentProfile::getStudentCode)
                        .orElse(null))
                .studentUserId(userId)
                .courseId(courseId)
                .courseName(enrollment.getCourse().getTitle())
                .enrollmentId(enrollment.getId())
                .enrollmentStatus(enrollment.getStatus().name())
                .totalModules(total)
                .completedModules(completed)
                .progressPercentage(percentage)
                .currentModule(currentModule)
                .completed(total > 0 && completed == total)
                .modules(modules)
                .build();
    }

    private List<ModuleProgressItemResponse> buildModules(
            UUID userId, UUID courseId, List<CourseModule> sequence) {

        // moduleId -> its gating mock test (lowest display order)
        Map<UUID, MockTest> gatingTest = mockTestRepository
                .findByCourseIdOrderByModuleOrder(courseId)
                .stream()
                .collect(Collectors.toMap(
                        test -> test.getModule().getId(),
                        test -> test,
                        (first, ignored) -> first,
                        LinkedHashMap::new));

        // moduleId -> when the student passed it
        Map<UUID, LocalDateTime> completedAt = new LinkedHashMap<>();
        mockTestAttemptRepository.findPassedAttemptsByUserIdAndCourseId(userId, courseId)
                .forEach(attempt -> completedAt.putIfAbsent(
                        attempt.getMockTest().getModule().getId(),
                        attempt.getSubmittedAt()));

        // lesson completion is reported per module for context only; it never
        // counts towards course progress
        Set<UUID> completedLessons = lessonProgressRepository
                .findByUserIdAndLessonModuleCourseId(userId, courseId)
                .stream()
                .map(p -> p.getLesson().getId())
                .collect(Collectors.toSet());

        List<ModuleProgressItemResponse> items = new ArrayList<>(sequence.size());
        boolean currentAssigned = false;

        for (int i = 0; i < sequence.size(); i++) {

            CourseModule module = sequence.get(i);
            MockTest test = gatingTest.get(module.getId());
            LocalDateTime passedAt = completedAt.get(module.getId());

            // no mock test attached -> nothing to pass, so auto-complete,
            // otherwise the sequence dead-ends here
            boolean moduleCompleted =
                    test == null || passedAt != null;

            ModuleProgressStatus status;
            if (moduleCompleted) {
                status = ModuleProgressStatus.COMPLETED;
            } else if (!currentAssigned) {
                status = ModuleProgressStatus.OPEN;
                currentAssigned = true;
            } else {
                status = ModuleProgressStatus.LOCKED;
            }

            List<Lesson> lessons = module.getLessons() == null
                    ? List.of() : module.getLessons();
            int lessonTotal = lessons.size();
            int lessonDone = (int) lessons.stream()
                    .filter(lesson -> completedLessons.contains(lesson.getId()))
                    .count();

            items.add(ModuleProgressItemResponse.builder()
                    .moduleId(module.getId())
                    .moduleCode(module.getModuleCode())
                    .moduleName(module.getTitle())
                    .description(module.getDescription())
                    .moduleNumber(i + 1)
                    .displayOrder(module.getDisplayOrder())
                    .status(status.name())
                    .unlocked(status != ModuleProgressStatus.LOCKED)
                    .mockTestCompleted(moduleCompleted)
                    .hasMockTest(test != null)
                    .mockTestId(test == null ? null : test.getId())
                    .mockTestTitle(test == null ? null : test.getTitle())
                    .completedAt(passedAt)
                    .totalLessons(lessonTotal)
                    .completedLessons(lessonDone)
                    .lessonProgressPercentage(
                            lessonTotal == 0 ? 0 : (lessonDone * 100) / lessonTotal)
                    .build());
        }

        return items;
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Enrollment requireActiveEnrollment(UUID userId, UUID courseId) {

        Enrollment enrollment = enrollmentRepository
                .findByUserIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Enrollment", "courseId", courseId));

        if (enrollment.getStatus() == EnrollmentStatus.DROPPED) {
            throw new IllegalArgumentException(
                    "You have dropped this course. Re-enroll to continue.");
        }
        return enrollment;
    }

    private CourseModule findModule(UUID moduleId) {
        return courseModuleRepository.findById(moduleId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("CourseModule", "id", moduleId));
    }

    private boolean isPrivileged(User user) {
        return hasRole(user, "SUPER_ADMIN") || hasRole(user, "STAFF");
    }

    private boolean isStudent(User user) {
        return hasRole(user, "STUDENT");
    }

    private boolean hasRole(User user, String roleName) {
        return user != null
                && user.getRoles() != null
                && user.getRoles().stream()
                        .map(Role::getRoleName)
                        .anyMatch(roleName::equals);
    }
}
