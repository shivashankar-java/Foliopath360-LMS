package com.foliopath360.lms.service;

import com.foliopath360.lms.config.InvoiceProperties;
import com.foliopath360.lms.config.InvoiceTaxProperties;
import com.foliopath360.lms.dto.request.OfflinePaymentInvoiceRequest;
import com.foliopath360.lms.dto.response.InvoiceResponse;
import com.foliopath360.lms.entity.Course;
import com.foliopath360.lms.entity.Invoice;
import com.foliopath360.lms.entity.InvoiceSequence;
import com.foliopath360.lms.entity.Order;
import com.foliopath360.lms.entity.OrderItem;
import com.foliopath360.lms.entity.OrderStatus;
import com.foliopath360.lms.entity.Payment;
import com.foliopath360.lms.entity.PaymentStatus;
import com.foliopath360.lms.entity.Role;
import com.foliopath360.lms.entity.StudentProfile;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.exception.InvoiceGenerationException;
import com.foliopath360.lms.mapper.InvoiceMapper;
import com.foliopath360.lms.repository.InvoiceEventRepository;
import com.foliopath360.lms.repository.InvoiceRepository;
import com.foliopath360.lms.repository.InvoiceSequenceRepository;
import com.foliopath360.lms.repository.OrderRepository;
import com.foliopath360.lms.repository.PaymentRepository;
import com.foliopath360.lms.repository.StudentProfileRepository;
import com.foliopath360.lms.service.impl.InvoiceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceEventRepository invoiceEventRepository;
    @Mock private InvoiceSequenceRepository invoiceSequenceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private EnrollmentService enrollmentService;
    @Mock private InterviewKitService interviewKitService;
    @Mock private InvoicePdfService pdfService;
    @Mock private InvoicePdfStorage pdfStorage;
    @Mock private InvoiceEmailService emailService;
    @Mock private InvoiceMapper mapper;

    private InvoiceServiceImpl service;
    private User student;
    private Course course;
    private Order order;
    private Payment payment;

    @BeforeEach
    void setUp() {
        InvoiceProperties properties = new InvoiceProperties();
        properties.setNumberPrefix("INV");

        InvoiceTaxProperties taxProperties = new InvoiceTaxProperties();
        taxProperties.setDefaultRate(new BigDecimal("18"));
        taxProperties.setCompanyState("Karnataka");
        taxProperties.setInclusive(true);

        service = new InvoiceServiceImpl(
                invoiceRepository, invoiceEventRepository, invoiceSequenceRepository,
                paymentRepository, orderRepository, studentProfileRepository,
                enrollmentService, interviewKitService, pdfService, pdfStorage,
                emailService, mapper, properties, taxProperties
        );

        student = user(UUID.randomUUID(), "student@foliopath360.com", "STUDENT");
        course = Course.builder()
                .id(UUID.randomUUID())
                .title("Professional Java Development")
                .build();

        OrderItem item = OrderItem.builder()
                .id(UUID.randomUUID())
                .course(course)
                .courseTitle("Professional Java Development")
                .price(new BigDecimal("11800.00"))
                .build();
        order = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber("FP360-TEST-1")
                .user(student)
                .totalAmount(new BigDecimal("11800.00"))
                .discountAmount(BigDecimal.ZERO)
                .finalAmount(new BigDecimal("11800.00"))
                .status(OrderStatus.PAID)
                .items(new ArrayList<>(List.of(item)))
                .build();
        item.setOrder(order);

        payment = Payment.builder()
                .id(UUID.randomUUID())
                .order(order)
                .user(student)
                .amount(new BigDecimal("11800.00"))
                .currency("INR")
                .status(PaymentStatus.SUCCESS)
                .paymentMethod("RAZORPAY")
                .razorpayPaymentId("pay_TEST123")
                .razorpayOrderId("order_TEST123")
                .paidAt(LocalDateTime.of(2026, 11, 24, 10, 25))
                .build();
    }

    @Test
    void generatesOneInvoiceForVerifiedPayment() {
        stubSuccessfulGeneration();

        InvoiceResponse response = service.ensureInvoiceForPayment(payment);

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).saveAndFlush(captor.capture());
        Invoice saved = captor.getValue();

        assertThat(response.getInvoiceNumber()).isEqualTo("INV-" + java.time.LocalDate.now().getYear() + "-000042");
        assertThat(saved.getPayment().getId()).isEqualTo(payment.getId());
        assertThat(saved.getTaxableAmount()).isEqualByComparingTo("10000.00");
        assertThat(saved.getCgstAmount()).isEqualByComparingTo("900.00");
        assertThat(saved.getSgstAmount()).isEqualByComparingTo("900.00");
        assertThat(saved.getIgstAmount()).isEqualByComparingTo("0.00");
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("11800.00");
        assertThat(saved.getItems()).hasSize(1);
        assertThat(saved.getTaxes()).hasSize(2);
        assertThat(saved.getPdfStorageKey()).isEqualTo("invoices/test.pdf");
        verify(pdfService).generate(saved);
        verify(pdfStorage).store(anyString(), any(byte[].class));
        verify(emailService).send(saved.getId(), student.getEmail(), student.getEmail());
    }

    @Test
    void duplicatePaymentDoesNotCreateAnotherInvoice() {
        Invoice existing = Invoice.builder()
                .id(UUID.randomUUID())
                .invoiceNumber("INV-2026-000001")
                .payment(payment)
                .build();
        when(invoiceRepository.findByPaymentId(payment.getId()))
                .thenReturn(Optional.of(existing));
        when(mapper.toResponse(existing)).thenReturn(
                InvoiceResponse.builder().id(existing.getId())
                        .invoiceNumber(existing.getInvoiceNumber()).build());

        InvoiceResponse response = service.ensureInvoiceForPayment(payment);

        assertThat(response.getInvoiceNumber()).isEqualTo("INV-2026-000001");
        verify(invoiceRepository, never()).saveAndFlush(any(Invoice.class));
        verifyNoInteractions(pdfService, pdfStorage, emailService);
    }

    @Test
    void failedPaymentNeverGeneratesPaidInvoice() {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setPaidAt(null);

        assertThatThrownBy(() -> service.ensureInvoiceForPayment(payment))
                .isInstanceOf(InvoiceGenerationException.class)
                .hasMessageContaining("verified successful payment");

        verifyNoInteractions(pdfService, pdfStorage, emailService, invoiceRepository);
    }

    @Test
    void studentCannotAccessAnotherStudentsInvoice() {
        User otherStudent = user(UUID.randomUUID(), "other@foliopath360.com", "STUDENT");
        Invoice invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .invoiceNumber("INV-2026-000009")
                .student(student)
                .build();
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> service.getInvoice(invoice.getId(), otherStudent))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void generatesInvoiceForOfflinePayment() {
        order.setStatus(OrderStatus.CREATED);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(invoiceRepository.findByOrderId(order.getId())).thenReturn(Optional.empty());
        when(paymentRepository.saveAndFlush(any(Payment.class))).thenAnswer(invocation -> {
            Payment saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });

        OfflinePaymentInvoiceRequest request = OfflinePaymentInvoiceRequest.builder()
                .orderId(order.getId())
                .paymentMethod("CASH")
                .paymentReference("CASH-RECEIPT-42")
                .paidAt("24-11-2026")
                .amount(new BigDecimal("11800.00"))
                .billingAddress("12 MG Road, Bengaluru")
                .build();

        stubSuccessfulGeneration();

        service.generateForOfflinePayment(request, student);

        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).saveAndFlush(paymentCaptor.capture());
        assertThat(paymentCaptor.getValue().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(paymentCaptor.getValue().getPaymentMethod()).isEqualTo("CASH");
        assertThat(paymentCaptor.getValue().getPaymentReference())
                .isEqualTo("CASH-RECEIPT-42");
        verify(enrollmentService).enrollAfterPayment(student, course);
        verify(orderRepository).save(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void ensureInvoiceForPaymentRequiresExistingTransaction() throws Exception {
        Method method = InvoiceServiceImpl.class.getMethod(
                "ensureInvoiceForPayment", Payment.class);
        Transactional transactional = method.getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.MANDATORY);
    }

    private void stubSuccessfulGeneration() {
        lenient().when(invoiceRepository.findByPaymentId(payment.getId()))
                .thenReturn(Optional.empty());
        when(studentProfileRepository.findByUserId(student.getId())).thenReturn(
                Optional.of(StudentProfile.builder()
                        .user(student)
                        .studentCode("FP360-STU-1")
                        .state("Karnataka")
                        .address("12 MG Road")
                        .city("Bengaluru")
                        .country("India")
                        .build()));
        when(invoiceSequenceRepository.saveAndFlush(any(InvoiceSequence.class)))
                .thenReturn(InvoiceSequence.builder()
                        .id(42L)
                        .createdAt(LocalDateTime.now())
                        .build());
        when(invoiceRepository.saveAndFlush(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });
        when(invoiceEventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(pdfService.generate(any(Invoice.class))).thenReturn("%PDF-1.4".getBytes());
        when(pdfStorage.store(anyString(), any(byte[].class))).thenReturn("invoices/test.pdf");
        when(emailService.send(any(), any(), any())).thenReturn(true);
        when(mapper.toResponse(any(Invoice.class))).thenAnswer(invocation -> {
            Invoice source = invocation.getArgument(0);
            return InvoiceResponse.builder()
                    .id(source.getId())
                    .invoiceNumber(source.getInvoiceNumber())
                    .build();
        });
    }

    private User user(UUID id, String email, String roleName) {
        return User.builder()
                .id(id)
                .username(email)
                .email(email)
                .firstName("Aarav")
                .lastName("Sharma")
                .mobileNumber("+919999999999")
                .roles(Set.of(Role.builder().id(1L).roleName(roleName).build()))
                .build();
    }
}
