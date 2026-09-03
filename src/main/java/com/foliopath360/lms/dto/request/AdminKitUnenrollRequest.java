package com.foliopath360.lms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AdminKitUnenrollRequest {

    @NotNull
    private UUID studentId;

    @NotNull
    private UUID kitId;

    private BigDecimal refundAmount;
}
