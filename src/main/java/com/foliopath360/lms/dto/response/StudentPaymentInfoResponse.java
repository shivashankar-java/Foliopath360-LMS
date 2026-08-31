package com.foliopath360.lms.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPaymentInfoResponse {

    private UUID courseId;
    private String courseTitle;
    private BigDecimal amountPaid;
    private BigDecimal courseFee;
    private BigDecimal discountAmount;
    private String paymentMethod;
    private String status;
}
