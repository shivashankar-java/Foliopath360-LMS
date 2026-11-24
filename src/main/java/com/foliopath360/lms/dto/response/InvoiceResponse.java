package com.foliopath360.lms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceResponse {

    private UUID id;

    private String invoiceNumber;

    private UUID studentId;

    private String studentCode;

    private String studentName;

    private String studentEmail;

    private String studentPhone;

    private String studentGstin;

    private String billingAddress;

    private UUID orderId;

    private String orderNumber;

    private UUID paymentId;

    private UUID courseId;

    private String courseName;

    private LocalDateTime invoiceDate;

    private LocalDateTime paymentDate;

    private BigDecimal subtotal;

    private BigDecimal discountAmount;

    private BigDecimal couponDiscountAmount;

    private BigDecimal scholarshipAmount;

    private BigDecimal taxableAmount;

    private BigDecimal cgstAmount;

    private BigDecimal sgstAmount;

    private BigDecimal igstAmount;

    private BigDecimal totalTax;

    private BigDecimal totalAmount;

    private String currency;

    private String paymentMethod;

    private String paymentStatus;

    private String paymentReference;

    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String emailStatus;

    private Integer emailAttempts;

    private LocalDateTime emailSentAt;

    private String downloadUrl;

    private List<InvoiceItemResponse> items;

    private List<InvoiceTaxResponse> taxes;
}
