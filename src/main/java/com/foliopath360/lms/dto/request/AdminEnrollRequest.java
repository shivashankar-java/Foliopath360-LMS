package com.foliopath360.lms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminEnrollRequest {

    @NotNull
    private UUID studentId;

    @NotNull
    private UUID courseId;

    // Discount percentage applied at the time of offline enrollment
    private BigDecimal discountPercentage;

    // Computed discount amount subtracted from the course fee
    private BigDecimal discountAmount;

    // The actual amount collected from the student after discount
    private BigDecimal amountPaid;

    // Method of payment: UPI, CARD, NET_BANKING, CASH
    private String paymentMethod;

    private String remarks;
}
