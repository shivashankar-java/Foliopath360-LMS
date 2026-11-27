package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.CreateRazorpayOrderRequest;
import com.foliopath360.lms.dto.request.VerifyPaymentRequest;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.PaymentResultResponse;
import com.foliopath360.lms.dto.response.RazorpayOrderResponse;
import com.foliopath360.lms.entity.User;

import java.util.UUID;

public interface PaymentService {

    RazorpayOrderResponse createRazorpayOrder(
            User student, CreateRazorpayOrderRequest request);

    PaymentResultResponse verifyPayment(User student, VerifyPaymentRequest request);

    PaymentResultResponse getPaymentStatus(User student, UUID orderId);

    MessageResponse handleWebhook(String rawBody, String signature);
}
