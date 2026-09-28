package com.foliopath360.lms.entity;

/**
 * Final verdict of a single programming-question submission.
 *
 * <p>Only {@link #ACCEPTED} means every required (public + hidden) test case
 * passed. Any other value keeps the question in the "attempted" state in the
 * student's course list.</p>
 */
public enum ProgrammingSubmissionStatus {
    ACCEPTED,
    WRONG_ANSWER,
    COMPILATION_ERROR,
    RUNTIME_ERROR,
    TIME_LIMIT_EXCEEDED,
    MEMORY_LIMIT_EXCEEDED
}
