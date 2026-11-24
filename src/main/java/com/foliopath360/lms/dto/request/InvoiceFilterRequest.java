package com.foliopath360.lms.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceFilterRequest {

    private String invoiceNumber;

    private String studentName;

    private String studentId;

    private String email;

    private UUID courseId;

    private String paymentStatus;

    private String dateFrom;

    private String dateTo;
}
