package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.config.InvoiceProperties;
import com.foliopath360.lms.config.InvoiceTaxProperties;
import com.foliopath360.lms.dto.request.InvoiceFilterRequest;
import com.foliopath360.lms.dto.request.InvoiceResendEmailRequest;
import com.foliopath360.lms.dto.request.OfflinePaymentInvoiceRequest;
import com.foliopath360.lms.dto.response.InvoiceResponse;
import com.foliopath360.lms.entity.Course;
import com.foliopath360.lms.entity.Invoice;
import com.foliopath360.lms.entity.InvoiceEvent;
import com.foliopath360.lms.entity.InvoiceEventType;
import com.foliopath360.lms.entity.InvoiceItem;
import com.foliopath360.lms.entity.InvoiceItemType;
import com.foliopath360.lms.entity.InvoiceSequence;
import com.foliopath360.lms.entity.InvoiceTax;
import com.foliopath360.lms.entity.Order;
import com.foliopath360.lms.entity.OrderItem;
import com.foliopath360.lms.entity.Payment;
import com.foliopath360.lms.entity.PaymentStatus;
import com.foliopath360.lms.entity.OrderStatus;
import com.foliopath360.lms.entity.StudentProfile;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.exception.InvoiceGenerationException;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.mapper.InvoiceMapper;
import com.foliopath360.lms.repository.InvoiceEventRepository;
import com.foliopath360.lms.repository.InvoiceRepository;
import com.foliopath360.lms.repository.InvoiceSequenceRepository;
import com.foliopath360.lms.repository.OrderRepository;
import com.foliopath360.lms.repository.PaymentRepository;
import com.foliopath360.lms.repository.StudentProfileRepository;
import com.foliopath360.lms.repository.InvoiceSpecification;
import com.foliopath360.lms.service.EnrollmentService;
import com.foliopath360.lms.service.InterviewKitService;
import com.foliopath360.lms.service.InvoiceEmailService;
import com.foliopath360.lms.service.InvoicePdfService;
import com.foliopath360.lms.service.InvoicePdfStorage;
import com.foliopath360.lms.service.InvoiceService;
import com.foliopath360.lms.service.InvoiceTaxCalculation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class InvoiceServiceImpl implements InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceServiceImpl.class);
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );

    private final InvoiceRepository invoiceRepository;
    private final InvoiceEventRepository invoiceEventRepository;
    private final InvoiceSequenceRepository invoiceSequenceRepository;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final EnrollmentService enrollmentService;
    private final InterviewKitService interviewKitService;
    private final InvoicePdfService pdfService;
    private final InvoicePdfStorage pdfStorage;
    private final InvoiceEmailService emailService;
    private final InvoiceMapper mapper;
    private final InvoiceProperties properties;
    private final InvoiceTaxProperties taxProperties;

    public InvoiceServiceImpl(
            InvoiceRepository invoiceRepository,
            InvoiceEventRepository invoiceEventRepository,
            InvoiceSequenceRepository invoiceSequenceRepository,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            StudentProfileRepository studentProfileRepository,
            EnrollmentService enrollmentService,
            InterviewKitService interviewKitService,
            InvoicePdfService pdfService,
            InvoicePdfStorage pdfStorage,
            InvoiceEmailService emailService,
            InvoiceMapper mapper,
            InvoiceProperties properties,
            InvoiceTaxProperties taxProperties
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceEventRepository = invoiceEventRepository;
        this.invoiceSequenceRepository = invoiceSequenceRepository;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.enrollmentService = enrollmentService;
        this.interviewKitService = interviewKitService;
        this.pdfService = pdfService;
        this.pdfStorage = pdfStorage;
        this.emailService = emailService;
        this.mapper = mapper;
        this.properties = properties;
        this.taxProperties = taxProperties;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public InvoiceResponse ensureInvoiceForPayment(Payment payment) {
        if (payment == null || payment.getId() == null) {
            throw new InvoiceGenerationException("Payment record is required");
        }
        Payment managed = paymentRepository.findById(payment.getId()).orElse(payment);
        return generateInvoice(managed, BigDecimal.ZERO, BigDecimal.ZERO, null, null);
    }

    @Override
    @Transactional
    public InvoiceResponse generateForOfflinePayment(
            OfflinePaymentInvoiceRequest request, User actor) {

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Order", "id", request.getOrderId()));

        Invoice existing = invoiceRepository.findByOrderId(order.getId()).orElse(null);
        if (existing != null) {
            return mapper.toResponse(existing);
        }

        BigDecimal couponDiscount = nvl(request.getCouponDiscount());
        BigDecimal scholarshipAmount = nvl(request.getScholarshipAmount());
        BigDecimal expectedAmount = money(order.getFinalAmount())
                .subtract(couponDiscount)
                .subtract(scholarshipAmount)
                .max(BigDecimal.ZERO);
        BigDecimal amount = request.getAmount() == null
                ? expectedAmount : money(request.getAmount());
        if (amount.compareTo(expectedAmount) != 0) {
            throw new IllegalArgumentException(
                    "Offline payment amount must match the order final amount "
                            + "after coupon and scholarship adjustments");
        }
        if (order.getStatus() == OrderStatus.PAID) {
            Payment successPayment = paymentRepository.findFirstByOrderIdAndStatusOrderByCreatedDtDesc(
                            order.getId(), PaymentStatus.SUCCESS)
                    .orElseThrow(() -> new InvoiceGenerationException(
                            "Order is paid but no successful payment record was found"));
            return generateInvoice(successPayment, couponDiscount,
                    scholarshipAmount, request.getBillingAddress(),
                    request.getGstin());
        }

        LocalDateTime paidAt = parseDateTime(request.getPaidAt());

        Payment payment = Payment.builder()
                .order(order)
                .user(order.getUser())
                .amount(amount)
                .currency(order.getCurrency())
                .status(PaymentStatus.SUCCESS)
                .paymentMethod(request.getPaymentMethod().trim().toUpperCase(Locale.ROOT))
                .paymentReference(request.getPaymentReference())
                .paidAt(paidAt)
                .build();
        payment = paymentRepository.saveAndFlush(payment);

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(paidAt);
        orderRepository.save(order);

        for (OrderItem item : order.getItems()) {
            if (item.getCourse() != null) {
                enrollmentService.enrollAfterPayment(order.getUser(), item.getCourse());
            } else if (item.getKit() != null) {
                interviewKitService.activatePaidKitEnrollment(
                        order.getUser(), item.getKit().getId());
            }
        }

        log.info("Offline payment {} recorded for order {}",
                payment.getId(), order.getOrderNumber());

        return generateInvoice(payment, couponDiscount,
                scholarshipAmount, request.getBillingAddress(),
                request.getGstin());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> list(InvoiceFilterRequest filter, Pageable pageable) {
        return invoiceRepository.findAll(
                        InvoiceSpecification.fromFilter(filter), pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> myInvoices(User student, Pageable pageable) {
        return invoiceRepository.findByStudentId(student.getId(), pageable)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(UUID invoiceId, User requester) {
        Invoice invoice = findInvoice(invoiceId);
        authorize(invoice, requester);
        return mapper.toResponse(invoice);
    }

    @Override
    @Transactional
    public byte[] download(UUID invoiceId, User requester) {
        Invoice invoice = findInvoice(invoiceId);
        authorize(invoice, requester);

        if (invoice.getPdfStorageKey() == null
                || !pdfStorage.exists(invoice.getPdfStorageKey())) {
            try {
                storePdf(invoice, false);
            } catch (InvoiceGenerationException ex) {
                log.error("Unable to regenerate PDF for invoice {}",
                        invoice.getInvoiceNumber(), ex);
                throw ex;
            }
        }

        invoiceEventRepository.save(InvoiceEvent.builder()
                .invoice(invoice)
                .eventType(InvoiceEventType.PDF_DOWNLOADED)
                .message("Invoice PDF downloaded")
                .actorEmail(requester.getEmail())
                .build());

        return pdfStorage.read(invoice.getPdfStorageKey());
    }

    @Override
    @Transactional
    public InvoiceResponse resendEmail(UUID invoiceId,
                                       InvoiceResendEmailRequest request,
                                       User requester) {
        Invoice invoice = findInvoice(invoiceId);
        authorize(invoice, requester);

        String recipient = request == null ? null : request.getEmail();
        if (recipient != null && !recipient.isBlank()
                && isStudent(requester)
                && !recipient.equalsIgnoreCase(invoice.getStudentEmail())) {
            throw new AccessDeniedException(
                    "Students can only resend invoices to their registered email");
        }

        boolean sent = emailService.send(invoice.getId(), recipient, requester.getEmail());
        if (!sent) {
            throw new InvoiceGenerationException(
                    "Invoice email could not be sent. Please retry later.");
        }
        return mapper.toResponse(findInvoice(invoiceId));
    }

    private InvoiceResponse generateInvoice(
            Payment payment,
            BigDecimal couponDiscount,
            BigDecimal scholarshipAmount,
            String billingAddress,
            String gstin
    ) {
        if (payment == null || payment.getId() == null) {
            throw new InvoiceGenerationException("Payment record is required");
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS
                || payment.getPaidAt() == null) {
            throw new InvoiceGenerationException(
                    "An invoice can only be generated for a verified successful payment");
        }

        Invoice duplicate = invoiceRepository.findByPaymentId(payment.getId()).orElse(null);
        if (duplicate != null) {
            log.info("Invoice {} already exists for payment {} - skipping generation",
                    duplicate.getInvoiceNumber(), payment.getId());
            return mapper.toResponse(duplicate);
        }

        Order order = payment.getOrder();
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            throw new InvoiceGenerationException(
                    "Paid order has no items to invoice");
        }

        User student = payment.getUser();
        StudentProfile profile = studentProfileRepository.findByUserId(student.getId())
                .orElse(null);

        BigDecimal subtotal = order.getItems().stream()
                .map(item -> money(item.getPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = money(order.getDiscountAmount());
        BigDecimal coupon = nvl(couponDiscount);
        BigDecimal scholarship = nvl(scholarshipAmount);
        BigDecimal totalDiscount = discount.add(coupon).add(scholarship);

        Course firstCourse = order.getItems().stream()
                .filter(item -> item.getCourse() != null)
                .map(OrderItem::getCourse)
                .findFirst()
                .orElse(null);

        BigDecimal rate = taxProperties.rateFor(
                firstCourse == null ? null : firstCourse.getId());
        boolean interState = taxProperties.isInterState(
                profile == null ? null : profile.getState());

        InvoiceTaxCalculation calculation = InvoiceTaxCalculation.calculate(
                subtotal, totalDiscount, rate, interState, taxProperties.isInclusive());

        BigDecimal paidAmount = money(payment.getAmount());
        if (calculation.totalAmount().compareTo(paidAmount) != 0) {
            throw new InvoiceGenerationException(
                    "Invoice total " + calculation.totalAmount()
                            + " does not match verified payment amount " + paidAmount);
        }

        Invoice invoice = Invoice.builder()
                .invoiceNumber(nextInvoiceNumber())
                .student(student)
                .order(order)
                .payment(payment)
                .course(firstCourse)
                .courseName(buildCourseName(order))
                .invoiceDate(LocalDateTime.now())
                .paymentDate(payment.getPaidAt())
                .subtotal(subtotal)
                .discountAmount(discount)
                .couponDiscountAmount(coupon)
                .scholarshipAmount(scholarship)
                .taxableAmount(calculation.taxableAmount())
                .cgstAmount(calculation.cgstAmount())
                .sgstAmount(calculation.sgstAmount())
                .igstAmount(calculation.igstAmount())
                .totalTax(calculation.totalTax())
                .totalAmount(paidAmount)
                .currency(payment.getCurrency() == null ? "INR" : payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getStatus().name())
                .paymentReference(payment.getRazorpayPaymentId() != null
                        ? payment.getRazorpayPaymentId() : payment.getPaymentReference())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayPaymentId(payment.getRazorpayPaymentId())
                .studentName(fullName(student))
                .studentCode(profile == null ? null : profile.getStudentCode())
                .studentEmail(student.getEmail())
                .studentPhone(student.getMobileNumber())
                .studentGstin(gstin)
                .billingAddress(billingAddress != null
                        ? billingAddress : buildBillingAddress(profile))
                .build();

        addItems(invoice, order, calculation, totalDiscount);
        addTaxes(invoice, calculation);

        invoice = invoiceRepository.saveAndFlush(invoice);
        invoiceEventRepository.save(InvoiceEvent.builder()
                .invoice(invoice)
                .eventType(InvoiceEventType.CREATED)
                .message("Invoice created after verified payment " + payment.getId())
                .actorEmail(student.getEmail())
                .build());

        try {
            storePdf(invoice, false);
        } catch (InvoiceGenerationException ex) {
            log.error("Invoice {} saved but PDF generation failed",
                    invoice.getInvoiceNumber(), ex);
        }

        try {
            emailService.send(invoice.getId(), student.getEmail(), student.getEmail());
        } catch (Exception ex) {
            log.error("Invoice {} saved but email dispatch failed",
                    invoice.getInvoiceNumber(), ex);
        }

        log.info("Invoice {} generated for payment {} and student {}",
                invoice.getInvoiceNumber(), payment.getId(), student.getId());
        return mapper.toResponse(invoice);
    }

    private void addItems(Invoice invoice, Order order,
                          InvoiceTaxCalculation calculation,
                          BigDecimal totalDiscount) {
        BigDecimal subtotal = calculation.subtotal();
        int line = 1;

        BigDecimal lineDiscountRemaining = nvl(totalDiscount);
        BigDecimal taxableRemaining = calculation.taxableAmount();
        BigDecimal taxRemaining = calculation.totalTax();

        List<OrderItem> orderItems = order.getItems();
        for (int i = 0; i < orderItems.size(); i++) {
            OrderItem source = orderItems.get(i);
            BigDecimal price = money(source.getPrice());
            boolean last = i == orderItems.size() - 1;

            BigDecimal itemDiscount = last
                    ? lineDiscountRemaining
                    : allocate(lineDiscountRemaining, price, subtotal);
            lineDiscountRemaining = lineDiscountRemaining.subtract(itemDiscount);

            BigDecimal itemTaxable = last
                    ? taxableRemaining
                    : allocate(taxableRemaining, price, subtotal);
            taxableRemaining = taxableRemaining.subtract(itemTaxable);

            BigDecimal itemTax = last
                    ? taxRemaining
                    : allocate(taxRemaining, price, subtotal);
            taxRemaining = taxRemaining.subtract(itemTax);

            InvoiceItemType type = source.getCourse() != null
                    ? InvoiceItemType.COURSE : InvoiceItemType.KIT;
            String description = source.getCourse() != null
                    ? (source.getCourseTitle() != null
                        ? source.getCourseTitle() : source.getCourse().getTitle())
                    : (source.getKitName() != null
                        ? source.getKitName() : "Interview Kit");

            invoice.getItems().add(InvoiceItem.builder()
                    .invoice(invoice)
                    .itemType(type)
                    .lineNumber(line++)
                    .description(description)
                    .quantity(1)
                    .unitPrice(price)
                    .discountAmount(itemDiscount)
                    .taxableAmount(itemTaxable)
                    .taxRate(calculation.taxRate())
                    .taxAmount(itemTax)
                    .lineTotal(itemTaxable.add(itemTax))
                    .build());
        }
    }

    private void addTaxes(Invoice invoice, InvoiceTaxCalculation calculation) {
        if (calculation.cgstAmount().compareTo(BigDecimal.ZERO) != 0) {
            invoice.getTaxes().add(InvoiceTax.builder()
                    .invoice(invoice)
                    .taxCode("CGST")
                    .taxName("CGST")
                    .taxRate(calculation.cgstRate())
                    .taxAmount(calculation.cgstAmount())
                    .build());
        }
        if (calculation.sgstAmount().compareTo(BigDecimal.ZERO) != 0) {
            invoice.getTaxes().add(InvoiceTax.builder()
                    .invoice(invoice)
                    .taxCode("SGST")
                    .taxName("SGST")
                    .taxRate(calculation.sgstRate())
                    .taxAmount(calculation.sgstAmount())
                    .build());
        }
        if (calculation.igstAmount().compareTo(BigDecimal.ZERO) != 0) {
            invoice.getTaxes().add(InvoiceTax.builder()
                    .invoice(invoice)
                    .taxCode("IGST")
                    .taxName("IGST")
                    .taxRate(calculation.igstRate())
                    .taxAmount(calculation.igstAmount())
                    .build());
        }
    }

    private void storePdf(Invoice invoice, boolean regenerate) {
        try {
            byte[] pdf = pdfService.generate(invoice);
            String key = pdfStorage.store(invoice.getInvoiceNumber(), pdf);
            invoice.setPdfStorageKey(key);
            invoiceRepository.save(invoice);
            invoiceEventRepository.save(InvoiceEvent.builder()
                    .invoice(invoice)
                    .eventType(regenerate
                            ? InvoiceEventType.PDF_REGENERATED
                            : InvoiceEventType.PDF_GENERATED)
                    .message("PDF stored at " + key)
                    .build());
        } catch (InvoiceGenerationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvoiceGenerationException("Unable to store invoice PDF", ex);
        }
    }

    private String nextInvoiceNumber() {
        InvoiceSequence sequence = invoiceSequenceRepository.saveAndFlush(
                InvoiceSequence.builder().createdAt(LocalDateTime.now()).build());
        if (sequence.getId() == null) {
            throw new InvoiceGenerationException("Unable to allocate invoice number");
        }
        String prefix = properties.getNumberPrefix() == null
                ? "INV" : properties.getNumberPrefix().trim().toUpperCase(Locale.ROOT);
        return prefix + "-" + LocalDate.now().getYear()
                + "-" + String.format("%06d", sequence.getId());
    }

    private Invoice findInvoice(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));
    }

    private void authorize(Invoice invoice, User requester) {
        if (isStudent(requester)
                && !invoice.getStudent().getId().equals(requester.getId())) {
            throw new AccessDeniedException(
                    "You are not allowed to access this invoice");
        }
    }

    private boolean isStudent(User user) {
        if (user.getRoles() == null) {
            return false;
        }
        return user.getRoles().stream()
                .anyMatch(role -> "STUDENT".equalsIgnoreCase(role.getRoleName()));
    }

    private String buildCourseName(Order order) {
        return order.getItems().stream()
                .map(item -> item.getCourseTitle() != null
                        ? item.getCourseTitle()
                        : (item.getKitName() != null ? item.getKitName() : "Learning item"))
                .distinct()
                .reduce((a, b) -> a + ", " + b)
                .orElse("Foliopath 360 Learning");
    }

    private String buildBillingAddress(StudentProfile profile) {
        if (profile == null) {
            return null;
        }
        return List.of(profile.getAddress(), profile.getCity(), profile.getState(),
                        profile.getCountry())
                .stream()
                .filter(value -> value != null && !value.isBlank())
                .reduce((a, b) -> a + ", " + b)
                .orElse(null);
    }

    private String fullName(User user) {
        return (user.getFirstName() == null ? "" : user.getFirstName())
                + (user.getLastName() == null || user.getLastName().isBlank()
                    ? "" : " " + user.getLastName());
    }

    private BigDecimal allocate(BigDecimal total, BigDecimal weight, BigDecimal totalWeight) {
        if (totalWeight == null || totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return total.multiply(weight)
                .divide(totalWeight, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : money(value);
    }

    private BigDecimal money(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }
        String trimmed = value.trim();
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                if (formatter == DateTimeFormatter.ISO_LOCAL_DATE_TIME) {
                    return LocalDateTime.parse(trimmed, formatter);
                }
                LocalDate date = LocalDate.parse(trimmed, formatter);
                return date.atStartOfDay();
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }
        throw new IllegalArgumentException(
                "paidAt must be DD-MM-YYYY, YYYY-MM-DD or an ISO local date-time");
    }
}
