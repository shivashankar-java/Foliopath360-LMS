package com.foliopath360.lms.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a call to the Razorpay API fails (network issue, rejection,
 * unexpected response).
 */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class RazorpayApiException extends RuntimeException {

    public RazorpayApiException(String message) {
        super(message);
    }

    public RazorpayApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
