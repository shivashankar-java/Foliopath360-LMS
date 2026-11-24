package com.foliopath360.lms.service;

import com.foliopath360.lms.config.InvoiceProperties;
import com.foliopath360.lms.entity.Invoice;
import com.foliopath360.lms.entity.InvoiceItem;
import com.foliopath360.lms.entity.InvoiceItemType;
import com.foliopath360.lms.entity.InvoiceTax;
import com.foliopath360.lms.entity.Order;
import com.foliopath360.lms.service.impl.OpenPdfInvoicePdfService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OpenPdfInvoicePdfServiceTest {

    @Test
    void generatesA4PdfWithBrandingAndRupeeFont() {
        InvoiceProperties properties = new InvoiceProperties();
        InvoicePdfService service = new OpenPdfInvoicePdfService(properties);

        byte[] pdf = service.generate(invoice());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII))
                .isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(3000);
    }

    private Invoice invoice() {
        Order order = Order.builder()
                .id(UUID.randomUUID())
                .orderNumber("FP360-TEST-1")
                .build();

        Invoice invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .invoiceNumber("INV-2026-000001")
                .order(order)
                .courseName("Professional Java Development")
                .invoiceDate(LocalDateTime.of(2026, 11, 24, 10, 30))
                .paymentDate(LocalDateTime.of(2026, 11, 24, 10, 25))
                .subtotal(new BigDecimal("11800.00"))
                .discountAmount(BigDecimal.ZERO)
                .couponDiscountAmount(BigDecimal.ZERO)
                .scholarshipAmount(BigDecimal.ZERO)
                .taxableAmount(new BigDecimal("10000.00"))
                .cgstAmount(new BigDecimal("900.00"))
                .sgstAmount(new BigDecimal("900.00"))
                .igstAmount(BigDecimal.ZERO)
                .totalTax(new BigDecimal("1800.00"))
                .totalAmount(new BigDecimal("11800.00"))
                .currency("INR")
                .paymentMethod("RAZORPAY")
                .paymentStatus("SUCCESS")
                .paymentReference("pay_TEST123")
                .razorpayPaymentId("pay_TEST123")
                .razorpayOrderId("order_TEST123")
                .studentName("Aarav Sharma")
                .studentCode("FP360-STU-1")
                .studentEmail("aarav@example.com")
                .studentPhone("+919999999999")
                .billingAddress("12 MG Road, Bengaluru, Karnataka 560001, India")
                .build();

        invoice.getItems().add(InvoiceItem.builder()
                .invoice(invoice)
                .itemType(InvoiceItemType.COURSE)
                .lineNumber(1)
                .description("Professional Java Development")
                .quantity(1)
                .unitPrice(new BigDecimal("11800.00"))
                .discountAmount(BigDecimal.ZERO)
                .taxableAmount(new BigDecimal("10000.00"))
                .taxRate(new BigDecimal("18"))
                .taxAmount(new BigDecimal("1800.00"))
                .lineTotal(new BigDecimal("11800.00"))
                .build());

        invoice.getTaxes().add(InvoiceTax.builder()
                .invoice(invoice)
                .taxCode("CGST")
                .taxName("CGST")
                .taxRate(new BigDecimal("9"))
                .taxAmount(new BigDecimal("900.00"))
                .build());
        invoice.getTaxes().add(InvoiceTax.builder()
                .invoice(invoice)
                .taxCode("SGST")
                .taxName("SGST")
                .taxRate(new BigDecimal("9"))
                .taxAmount(new BigDecimal("900.00"))
                .build());

        return invoice;
    }
}
