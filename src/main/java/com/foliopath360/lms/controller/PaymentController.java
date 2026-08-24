package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.CreateRazorpayOrderRequest;
import com.foliopath360.lms.dto.request.VerifyPaymentRequest;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.PaymentResultResponse;
import com.foliopath360.lms.dto.response.RazorpayOrderResponse;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Creates (or reuses) the Razorpay order for an internal order.
     * The response contains everything the frontend needs to open
     * Razorpay Checkout. If orderId is omitted, the most recent
     * pending order of the student is used.
     *
     * Public webhook note: /api/payments/webhook is permitAll in
     * SecurityConfig - it is authenticated by the Razorpay HMAC signature.
     */
    @PostMapping("/create-order")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<RazorpayOrderResponse> createRazorpayOrder(
            @AuthenticationPrincipal User student,
            @Valid @RequestBody(required = false) CreateRazorpayOrderRequest request
    ) {
        return ResponseEntity.ok(paymentService.createRazorpayOrder(
                student,
                request == null ? CreateRazorpayOrderRequest.builder().build() : request
        ));
    }

    /**
     * Verifies razorpay_order_id + razorpay_payment_id + razorpay_signature.
     * On success: Payment SUCCESS -> Order PAID -> Enrollment ACTIVE.
     */
    @PostMapping("/verify")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<PaymentResultResponse> verifyPayment(
            @AuthenticationPrincipal User student,
            @Valid @RequestBody VerifyPaymentRequest request
    ) {
        return ResponseEntity.ok(paymentService.verifyPayment(student, request));
    }

    /**
     * Server-to-server notifications from Razorpay. No JWT here -
     * authenticity comes from the X-Razorpay-Signature HMAC header.
     */
    @PostMapping("/webhook")
    public ResponseEntity<MessageResponse> handleWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        return ResponseEntity.ok(paymentService.handleWebhook(rawBody, signature));
    }
}
