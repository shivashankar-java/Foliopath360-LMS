package com.foliopath360.lms.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Everything the frontend needs to open the Razorpay Checkout widget.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RazorpayOrderResponse {

    private UUID orderId;

    private String orderNumber;

    private String razorpayOrderId;

    // Amount in paise (Razorpay expects paise, e.g. Rs.9,999 -> 999900)
    private Long amountInPaise;

    // Amount in rupees for display purposes
    private BigDecimal amount;

    private String currency;

    private String razorpayKeyId;
}
