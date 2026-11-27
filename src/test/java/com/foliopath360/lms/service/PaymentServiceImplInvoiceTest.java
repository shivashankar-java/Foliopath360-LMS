package com.foliopath360.lms.service;

import com.foliopath360.lms.client.RazorpayGateway;
import com.foliopath360.lms.config.RazorpayProperties;
import com.foliopath360.lms.dto.request.VerifyPaymentRequest;
import com.foliopath360.lms.dto.response.InvoiceResponse;
import com.foliopath360.lms.entity.Course;
import com.foliopath360.lms.entity.Order;
import com.foliopath360.lms.entity.OrderItem;
import com.foliopath360.lms.entity.OrderStatus;
import com.foliopath360.lms.entity.Payment;
import com.foliopath360.lms.entity.PaymentStatus;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.exception.InvalidPaymentSignatureException;
import com.foliopath360.lms.repository.OrderRepository;
import com.foliopath360.lms.repository.PaymentRepository;
import com.foliopath360.lms.service.impl.PaymentServiceImpl;
import com.foliopath360.lms.util.RazorpaySignatureUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplInvoiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private EnrollmentService enrollmentService;
    @Mock private InterviewKitService interviewKitService;
    @Mock private RazorpayGateway razorpayGateway;
    @Mock private InvoiceService invoiceService;

    private PaymentServiceImpl service;
    private Payment payment;
    private VerifyPaymentRequest request;
    private User student;

    @BeforeEach
    void setUp() {
        RazorpayProperties razorpayProperties = new RazorpayProperties();
        razorpayProperties.setKeySecret("test_secret");

        service = new PaymentServiceImpl(
                paymentRepository, orderRepository, enrollmentService,
                interviewKitService, razorpayGateway, razorpayProperties, invoiceService
        );

        student = User.builder()
                .id(UUID.randomUUID())
                .email("student@foliopath360.com")
                .build();

        Course course = Course.builder().id(UUID.randomUUID()).title("Java").build();
        OrderItem item = OrderItem.builder()
                .id(UUID.randomUUID())
                .course(course)
                .courseTitle("Java")
                .price(new BigDecimal("1000.00"))
                .build();
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber("FP360-TEST-1")
                .user(student)
                .totalAmount(new BigDecimal("1000.00"))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("1000.00"))
                .status(OrderStatus.PAYMENT_PENDING)
                .items(new ArrayList<>(List.of(item)))
                .build();
        item.setOrder(order);

        payment = Payment.builder()
                .id(UUID.randomUUID())
                .order(order)
                .user(student)
                .razorpayOrderId("order_TEST1")
                .amount(new BigDecimal("1000.00"))
                .currency("INR")
                .status(PaymentStatus.CREATED)
                .build();

        String payload = "order_TEST1|pay_TEST1";
        request = VerifyPaymentRequest.builder()
                .razorpayOrderId("order_TEST1")
                .razorpayPaymentId("pay_TEST1")
                .razorpaySignature(
                        RazorpaySignatureUtil.hmacSha256Hex("test_secret", payload))
                .build();
    }

    @Test
    void verifiedPaymentTriggersInvoiceGeneration() {
        when(paymentRepository.findByRazorpayOrderId("order_TEST1"))
                .thenReturn(Optional.of(payment));

        var result = service.verifyPayment(student, request);

        assertThat(result.getPaymentStatus()).isEqualTo("SUCCESS");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(payment.getOrder().getStatus()).isEqualTo(OrderStatus.PAID);
        verify(enrollmentService).enrollAfterPayment(any(), any());
        verify(invoiceService).ensureInvoiceForPayment(payment);
    }

    @Test
    void duplicateVerificationDoesNotCreateDuplicateInvoiceWork() {
        when(paymentRepository.findByRazorpayOrderId("order_TEST1"))
                .thenReturn(Optional.of(payment));
        when(invoiceService.ensureInvoiceForPayment(payment)).thenReturn(
                InvoiceResponse.builder().invoiceNumber("INV-2026-000001").build());

        service.verifyPayment(student, request);
        service.verifyPayment(student, request);

        verify(invoiceService, times(2)).ensureInvoiceForPayment(payment);
        verify(enrollmentService, times(1)).enrollAfterPayment(any(), any());
    }

    @Test
    void invoiceGenerationFailureDoesNotFailPaymentVerification() {
        when(paymentRepository.findByRazorpayOrderId("order_TEST1"))
                .thenReturn(Optional.of(payment));
        doThrow(new RuntimeException("invoice unavailable"))
                .when(invoiceService).ensureInvoiceForPayment(payment);

        var result = service.verifyPayment(student, request);

        assertThat(result.getPaymentStatus()).isEqualTo("SUCCESS");
        verify(invoiceService).ensureInvoiceForPayment(payment);
    }

    @Test
    void cancelledOrderDoesNotEnrollOrGenerateInvoice() {
        payment.getOrder().setStatus(OrderStatus.CANCELLED);
        when(paymentRepository.findByRazorpayOrderId("order_TEST1"))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.verifyPayment(student, request))
                .isInstanceOf(IllegalStateException.class);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(enrollmentService, never()).enrollAfterPayment(any(), any());
        verify(invoiceService, never()).ensureInvoiceForPayment(any());
    }

    @Test
    void invalidSignatureDoesNotGenerateInvoice() {
        request.setRazorpaySignature("invalid");
        when(paymentRepository.findByRazorpayOrderId("order_TEST1"))
                .thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> service.verifyPayment(student, request))
                .isInstanceOf(InvalidPaymentSignatureException.class);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(invoiceService, never()).ensureInvoiceForPayment(any());
    }
}
