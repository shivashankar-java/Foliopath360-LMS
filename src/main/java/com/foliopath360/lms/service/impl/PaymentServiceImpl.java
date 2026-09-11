package com.foliopath360.lms.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.foliopath360.lms.client.RazorpayGateway;
import com.foliopath360.lms.config.RazorpayProperties;
import com.foliopath360.lms.dto.request.CreateRazorpayOrderRequest;
import com.foliopath360.lms.dto.request.VerifyPaymentRequest;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.PaymentResultResponse;
import com.foliopath360.lms.dto.response.RazorpayOrderResponse;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.exception.InvalidPaymentSignatureException;
import com.foliopath360.lms.exception.RazorpayApiException;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.repository.OrderRepository;
import com.foliopath360.lms.repository.PaymentRepository;
import com.foliopath360.lms.service.EnrollmentService;
import com.foliopath360.lms.service.InterviewKitService;
import com.foliopath360.lms.service.PaymentService;
import com.foliopath360.lms.util.RazorpaySignatureUtil;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    // Plain Jackson instance for parsing webhook payloads.
    // (Boot 4 auto-configures Jackson 3, so we do not inject this as a bean.)
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final EnrollmentService enrollmentService;
    private final InterviewKitService interviewKitService;
    private final RazorpayGateway razorpayGateway;
    private final RazorpayProperties razorpayProperties;

    public PaymentServiceImpl(
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            EnrollmentService enrollmentService,
            InterviewKitService interviewKitService,
            RazorpayGateway razorpayGateway,
            RazorpayProperties razorpayProperties
    ) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.enrollmentService = enrollmentService;
        this.interviewKitService = interviewKitService;
        this.razorpayGateway = razorpayGateway;
        this.razorpayProperties = razorpayProperties;
    }

    // ------------------------------------------------------------------
    // 1. Create the Razorpay order for an internal order
    // ------------------------------------------------------------------

    @Override
    public RazorpayOrderResponse createRazorpayOrder(
            User student, CreateRazorpayOrderRequest request) {

        Order order = resolvePayableOrder(student, request.getOrderId());

        if (order.getStatus() == OrderStatus.PAID) {
            throw new IllegalStateException("This order has already been paid");
        }

        Payment payment = paymentRepository
                .findFirstByOrderIdOrderByCreatedDtDesc(order.getId())
                .orElse(null);

        // Reuse the existing Razorpay order on retry (page refresh, network
        // failure, etc.) so we don't create a new gateway order every time.
        boolean reusable = payment != null
                && payment.getRazorpayOrderId() != null
                && (payment.getStatus() == PaymentStatus.CREATED
                    || payment.getStatus() == PaymentStatus.PENDING);

        if (!reusable) {

            String razorpayOrderId = razorpayGateway.createOrder(
                    toPaise(order.getFinalAmount()),
                    order.getCurrency(),
                    order.getOrderNumber()
            );

            if (payment == null) {
                payment = Payment.builder()
                        .order(order)
                        .user(student)
                        .amount(order.getFinalAmount())
                        .currency(order.getCurrency())
                        .status(PaymentStatus.CREATED)
                        .build();
            } else {
                payment.setStatus(PaymentStatus.CREATED);
                payment.setFailureReason(null);
            }

            payment.setRazorpayOrderId(razorpayOrderId);
            payment = paymentRepository.save(payment);
        }

        order.setStatus(OrderStatus.PAYMENT_PENDING);

        return RazorpayOrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .amountInPaise(toPaise(order.getFinalAmount()))
                .amount(order.getFinalAmount())
                .currency(order.getCurrency())
                .razorpayKeyId(razorpayProperties.getKeyId())
                .build();
    }

    // ------------------------------------------------------------------
    // 2. Verify the payment returned by Razorpay Checkout
    // ------------------------------------------------------------------

    @Override
    public PaymentResultResponse verifyPayment(
            User student, VerifyPaymentRequest request) {

        Payment payment = paymentRepository
                .findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payment", "razorpay_order_id",
                        request.getRazorpayOrderId()));

        if (!payment.getUser().getId().equals(student.getId())) {
            throw new IllegalArgumentException(
                    "This payment does not belong to the current user");
        }

        Order order = payment.getOrder();

        // Idempotent: the webhook may have completed this first
        if (payment.getStatus() == PaymentStatus.SUCCESS
                && order.getStatus() == OrderStatus.PAID) {
            return buildAlreadyPaidResult(payment, order);
        }

        boolean valid = RazorpaySignatureUtil.isValidSignature(
                razorpayProperties.getKeySecret(),
                request.getRazorpayOrderId() + "|"
                        + request.getRazorpayPaymentId(),
                request.getRazorpaySignature());

        if (!valid) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Signature verification failed");
            paymentRepository.save(payment);

            throw new InvalidPaymentSignatureException(
                    "Payment signature verification failed");
        }

        return markPaidAndEnroll(
                payment, order,
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature());
    }

    // ------------------------------------------------------------------
    // 3. Webhook from Razorpay (browser may never come back)
    // ------------------------------------------------------------------

    @Override
    public MessageResponse handleWebhook(String rawBody, String signature) {

        if (!razorpayProperties.isWebhookConfigured()) {
            throw new IllegalStateException(
                    "Razorpay webhook secret is not configured");
        }

        boolean valid = RazorpaySignatureUtil.isValidSignature(
                razorpayProperties.getWebhookSecret(), rawBody, signature);

        if (!valid) {
            throw new InvalidPaymentSignatureException("Invalid webhook signature");
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(rawBody);
        } catch (JsonProcessingException ex) {
            throw new RazorpayApiException("Malformed webhook payload", ex);
        }

        String event = root.path("event").asText("");

        // payment.* events carry payload.payment.entity; order.paid carries
        // payload.order.entity - both contain the identifiers we need.
        String razorpayOrderId = root.path("payload")
                .path("payment").path("entity")
                .path("order_id").asText(null);

        if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
            razorpayOrderId = root.path("payload")
                    .path("order").path("entity")
                    .path("id").asText(null);
        }

        if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
            log.warn("Razorpay webhook '{}' without an order reference - ignored", event);
            return MessageResponse.builder()
                    .message("Webhook received but ignored: no order reference")
                    .build();
        }

        Payment payment = paymentRepository
                .findByRazorpayOrderId(razorpayOrderId)
                .orElse(null);

        if (payment == null) {
            log.warn("Razorpay webhook '{}' for unknown razorpayOrderId {} - ignored",
                    event, razorpayOrderId);
            return MessageResponse.builder()
                    .message("Webhook received but no matching payment found")
                    .build();
        }

        Order order = payment.getOrder();

        switch (event) {
            case "payment.captured", "order.paid" -> {
                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                    JsonNode entity = root.path("payload")
                            .path("payment").path("entity");
                    markPaidAndEnroll(
                            payment, order,
                            entity.path("id").asText(null),
                            null); // webhooks are authenticated by their own HMAC
                }
            }
            case "payment.failed" -> {
                if (payment.getStatus() != PaymentStatus.SUCCESS) {
                    JsonNode entity = root.path("payload")
                            .path("payment").path("entity");
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason(entity.path("error_description").asText(null));
                    paymentRepository.save(payment);
                }
            }
            default -> log.debug("Ignoring Razorpay webhook event '{}'", event);
        }

        return MessageResponse.builder()
                .message("Webhook processed")
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Explicit orderId wins; otherwise pick the student's most recent order
     * that is still awaiting payment.
     */
    private Order resolvePayableOrder(User student, UUID orderId) {

        if (orderId != null) {
            return orderRepository.findByIdAndUserId(orderId, student.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Order", "id", orderId));
        }

        return orderRepository.findFirstByUserIdAndStatusInOrderByCreatedDtDesc(
                        student.getId(),
                        List.of(OrderStatus.CREATED, OrderStatus.PAYMENT_PENDING))
                .orElseThrow(() -> new IllegalStateException(
                        "No pending order found. Please checkout your cart first."));
    }

    /**
     * Single source of truth for "money received": marks the payment and the
     * order as paid, then activates enrollments for every course in the order.
     */
    private PaymentResultResponse markPaidAndEnroll(
            Payment payment, Order order,
            String razorpayPaymentId, String razorpaySignature) {

        payment.setStatus(PaymentStatus.SUCCESS);
        if (razorpayPaymentId != null && !razorpayPaymentId.isBlank()) {
            payment.setRazorpayPaymentId(razorpayPaymentId);
        }
        if (razorpaySignature != null && !razorpaySignature.isBlank()) {
            payment.setRazorpaySignature(razorpaySignature);
        }
        payment.setPaidAt(LocalDateTime.now());
        payment.setFailureReason(null);
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        List<UUID> enrolledCourseIds = new ArrayList<>();
        List<UUID> enrolledKitIds = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            if (item.getCourse() != null) {
                enrollmentService.enrollAfterPayment(order.getUser(), item.getCourse());
                enrolledCourseIds.add(item.getCourse().getId());
            } else if (item.getKit() != null) {
                interviewKitService.activatePaidKitEnrollment(order.getUser(), item.getKit().getId());
                enrolledKitIds.add(item.getKit().getId());
            }
        }

        log.info("Order {} marked PAID; {} course(s) and {} kit(s) activated for user {}",
                order.getOrderNumber(), enrolledCourseIds.size(), enrolledKitIds.size(),
                order.getUser().getId());

        return PaymentResultResponse.builder()
                .message("Payment successful. Your purchases are now unlocked.")
                .paymentStatus(PaymentStatus.SUCCESS.name())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getStatus().name())
                .enrolledCourseIds(enrolledCourseIds)
                .enrolledKitIds(enrolledKitIds)
                .build();
    }

    private PaymentResultResponse buildAlreadyPaidResult(Payment payment, Order order) {

        List<UUID> enrolledCourseIds = new ArrayList<>();
        List<UUID> enrolledKitIds = new ArrayList<>();

        for (OrderItem item : order.getItems()) {
            if (item.getCourse() != null) {
                enrolledCourseIds.add(item.getCourse().getId());
            } else if (item.getKit() != null) {
                enrolledKitIds.add(item.getKit().getId());
            }
        }

        return PaymentResultResponse.builder()
                .message("This order was already paid.")
                .paymentStatus(PaymentStatus.SUCCESS.name())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderStatus(order.getStatus().name())
                .enrolledCourseIds(enrolledCourseIds)
                .enrolledKitIds(enrolledKitIds)
                .build();
    }

    /**
     * Rs.9,999.00 -> 999900 paise (Razorpay expects integer paise).
     */
    private long toPaise(BigDecimal amount) {
        return amount.movePointRight(2).longValueExact();
    }
}
