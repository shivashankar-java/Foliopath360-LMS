package com.foliopath360.lms.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransactionResponse {

    private UUID id;
    private String txnId;
    private String studentName;
    private String courseTitle;
    private BigDecimal amount;
    private String method;
    private LocalDateTime date;
    private String status;
}
