package com.foliopath360.lms.dto.request;

import lombok.*;

import java.util.UUID;

/**
 * orderId is optional. When omitted, the backend picks the student's most
 * recent order that is still awaiting payment.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRazorpayOrderRequest {

    private UUID orderId;
}
