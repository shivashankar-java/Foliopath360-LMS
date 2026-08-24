package com.foliopath360.lms.dto.response;

import lombok.*;

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

    private UUID orderId;

    private String orderNumber;

    private String orderStatus;

    private List<UUID> enrolledCourseIds;
}
