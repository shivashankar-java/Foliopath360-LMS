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
public class InvoiceItemResponse {

    private UUID id;

    private String itemType;

    private int lineNumber;

    private String description;

    private int quantity;

    private BigDecimal unitPrice;

    private BigDecimal discountAmount;

    private BigDecimal taxableAmount;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;

    private BigDecimal lineTotal;
}
