package com.foliopath360.lms.exception;

/**
 * Thrown when a student without any course / interview kit enrollment tries to
 * reach the standalone programming question bank.
 */
public class ProgrammingQuestionAccessDeniedException extends RuntimeException {

    public ProgrammingQuestionAccessDeniedException(String message) {
        super(message);
    }
}
