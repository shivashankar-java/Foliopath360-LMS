package com.foliopath360.lms.entity;

/**
 * Per-student status of a course module.
 *
 * <p>Module status is <em>derived</em> from {@code mock_test_attempts} and the
 * module's position in the course - it is never persisted, so it can never
 * drift out of sync with the attempts that drive it:
 *
 * <ul>
 *   <li>{@link #LOCKED} - every preceding module is not yet completed, so
 *       the student may not access this module at all.</li>
 *   <li>{@link #OPEN} - every preceding module is completed and this one is
 *       not, making it the student's current module (also reported as
 *       {@code IN_PROGRESS} by older clients).</li>
 *   <li>{@link #COMPLETED} - the module's mock test has a passed attempt,
 *       which is what unlocks the following module.</li>
 * </ul>
 */
public enum ModuleProgressStatus {

    LOCKED,
    OPEN,
    COMPLETED
}
