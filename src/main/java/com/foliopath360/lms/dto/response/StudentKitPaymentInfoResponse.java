package com.foliopath360.lms.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentKitPaymentInfoResponse {

    private UUID kitId;
    private String kitName;
    private BigDecimal amountPaid;
    private BigDecimal kitFee;
    private BigDecimal discountAmount;
    private String paymentMethod;
    private String status;
}
