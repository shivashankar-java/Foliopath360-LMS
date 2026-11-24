package com.foliopath360.lms.entity;

import com.foliopath360.lms.entity.base.AuditFields;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "invoices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_invoices_invoice_number", columnNames = "invoice_number"),
                @UniqueConstraint(name = "uk_invoices_payment_id", columnNames = "payment_id")
        },
        indexes = {
                @Index(name = "idx_invoices_student_id", columnList = "student_id"),
                @Index(name = "idx_invoices_course_id", columnList = "course_id"),
                @Index(name = "idx_invoices_invoice_date", columnList = "invoice_date"),
                @Index(name = "idx_invoices_payment_status", columnList = "payment_status")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends AuditFields {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 40)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "course_id")
    private Course course;

    @Column(name = "course_name", length = 250)
    private String courseName;

    @Column(name = "invoice_date", nullable = false)
    private LocalDateTime invoiceDate;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "subtotal", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Column(name = "coupon_discount_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal couponDiscountAmount = BigDecimal.ZERO;

    @Column(name = "scholarship_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal scholarshipAmount = BigDecimal.ZERO;

    @Column(name = "taxable_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxableAmount;

    @Column(name = "cgst_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cgstAmount = BigDecimal.ZERO;

    @Column(name = "sgst_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal sgstAmount = BigDecimal.ZERO;

    @Column(name = "igst_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal igstAmount = BigDecimal.ZERO;

    @Column(name = "total_tax", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalTax = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "currency", nullable = false, length = 10)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus;

    @Column(name = "payment_reference", length = 120)
    private String paymentReference;

    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;

    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @Column(name = "student_name", nullable = false, length = 200)
    private String studentName;

    @Column(name = "student_code", length = 50)
    private String studentCode;

    @Column(name = "student_email", nullable = false, length = 150)
    private String studentEmail;

    @Column(name = "student_phone", length = 30)
    private String studentPhone;

    @Column(name = "student_gstin", length = 20)
    private String studentGstin;

    @Column(name = "billing_address", length = 500)
    private String billingAddress;

    @Column(name = "pdf_storage_key", length = 500)
    private String pdfStorageKey;

    @Column(name = "pdf_content_type", length = 100)
    @Builder.Default
    private String pdfContentType = "application/pdf";

    @Enumerated(EnumType.STRING)
    @Column(name = "email_status", nullable = false, length = 20)
    @Builder.Default
    private InvoiceEmailStatus emailStatus = InvoiceEmailStatus.PENDING;

    @Column(name = "email_attempts", nullable = false)
    @Builder.Default
    private Integer emailAttempts = 0;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @Column(name = "last_email_attempt_at")
    private LocalDateTime lastEmailAttemptAt;

    @Column(name = "last_email_error", length = 1000)
    private String lastEmailError;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InvoiceTax> taxes = new ArrayList<>();
}
