package com.foliopath360.lms.dto.response;

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
public class InvoiceTaxResponse {

    private UUID id;

    private String taxCode;

    private String taxName;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;
}
