package com.foliopath360.lms.exception;

/**
 * Thrown when a Razorpay HMAC signature does not match (payment verification
 * or webhook validation). Treated as a client error.
 */
public class InvalidPaymentSignatureException extends RuntimeException {

    public InvalidPaymentSignatureException(String message) {
        super(message);
    }
}
