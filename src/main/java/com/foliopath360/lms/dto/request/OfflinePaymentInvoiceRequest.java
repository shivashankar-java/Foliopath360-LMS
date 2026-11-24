package com.foliopath360.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfflinePaymentInvoiceRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotBlank(message = "Payment method is required (CASH, UPI, BANK_TRANSFER, etc.)")
    private String paymentMethod;

    private String paymentReference;

    private String paidAt;

    @PositiveOrZero(message = "Amount cannot be negative")
    private BigDecimal amount;

    @PositiveOrZero(message = "Coupon discount cannot be negative")
    private BigDecimal couponDiscount;

    @PositiveOrZero(message = "Scholarship amount cannot be negative")
    private BigDecimal scholarshipAmount;

    private String billingAddress;

    private String gstin;
}
