package com.foliopath360.lms.exception;

public class InvoiceGenerationException extends RuntimeException {

    public InvoiceGenerationException(String message) {
        super(message);
    }

    public InvoiceGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
