package com.foliopath360.lms.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResultResponse {

    private String message;

    private String paymentStatus;

    private String razorpayPaymentId;

    private BigDecimal amount;

    private String paymentMethod;

    private UUID orderId;

    private String orderNumber;

    private String orderStatus;

    private List<UUID> enrolledCourseIds;

    private List<UUID> enrolledKitIds;
}
