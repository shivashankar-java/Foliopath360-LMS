package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.response.ModuleProgressResponse;
import com.foliopath360.lms.entity.CourseModule;
import com.foliopath360.lms.entity.User;

import java.util.UUID;

/**
 * Sequential module unlocking driven entirely by mock test completion.
 *
 * <p>The lifecycle enforced here is:
 *
 * <pre>
 * Enrollment -&gt; Module 1 OPEN -&gt; Mock Test 1 passed -&gt; Module 1 COMPLETED
 *            -&gt; Module 2 OPEN  -&gt; Mock Test 2 passed -&gt; Module 2 COMPLETED
 *            -&gt; ... -&gt; final mock test passed -&gt; 100% progress
 * </pre>
 *
 * <p>Module status is derived from {@code mock_test_attempts} plus the
 * module's position in the course; it is never stored, so it cannot drift
 * out of sync with the attempts that drive it.
 */
public interface ModuleProgressService {

    /**
     * Full sequential progress (status of every module plus the overall
     * percentage) for the student's active enrollment in a course.
     */
    ModuleProgressResponse getCourseProgress(User student, UUID courseId);

    /**
     * Same payload as {@link #getCourseProgress} but resolved from a single
     * module - the containing course drives the sequence.
     */
    ModuleProgressResponse getModuleProgress(User student, UUID moduleId);

    /**
     * Progress for every course the student is actively enrolled in.
     */
    java.util.List<ModuleProgressResponse> getMyCourseProgress(User student);

    /**
     * Called after a mock test attempt is graded. When the attempt passed,
     * the module is treated as completed and the next module unlocks.
     * Returns the refreshed progress payload, or {@code null} when the
     * student has no active enrollment (nothing to persist).
     */
    ModuleProgressResponse onMockTestAttempt(User student, CourseModule module);

    /**
     * Whether the requester may open the module's lessons / mock tests.
     *
     * <p>Staff and admins always can. A student can only reach a module that
     * is {@code OPEN} or {@code COMPLETED} in their own enrollment, so a
     * locked module cannot be read by crafting a direct URL.
     */
    boolean canAccessModule(User requester, CourseModule module);

    /**
     * Same as {@link #canAccessModule} but rejects locked modules with a
     * {@link com.foliopath360.lms.exception.ModuleLockedException} (HTTP 403)
     * so callers get an explicit reason instead of empty content.
     */
    void requireModuleAccess(User requester, CourseModule module);

    /**
     * True when the student is actively enrolled in the module's own course
     * <em>and</em> the module is not {@code LOCKED} for them.
     *
     * <p>Distinct from {@link #canAccessModule}: this returns {@code false}
     * for a not-yet-enrolled student rather than rejecting them, so callers
     * that support alternative access routes (e.g. a parent course that
     * embeds this course as a module) can fall through to their own logic.
     */
    boolean isModuleUnlocked(User student, CourseModule module);

    /**
     * Recomputes and persists {@code enrollment.progressPercentage} from
     * completed mock tests. Called on enrollment, lesson completion and
     * mock test grading so the stored value always matches the derivation.
     */
    void refreshEnrollmentProgress(UUID userId, UUID courseId);
}
