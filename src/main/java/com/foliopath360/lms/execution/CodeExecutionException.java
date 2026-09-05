package com.foliopath360.lms.execution;

/**
 * Raised when a submitted program cannot be compiled/run.
 * Only the message is ever surfaced to the client — never the stack trace.
 */
public class CodeExecutionException extends RuntimeException {

    public CodeExecutionException(String message) {
        super(message);
    }

    public CodeExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}