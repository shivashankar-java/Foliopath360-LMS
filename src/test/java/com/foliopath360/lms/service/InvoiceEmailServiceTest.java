package com.foliopath360.lms.service;

import com.foliopath360.lms.config.InvoiceProperties;
import com.foliopath360.lms.entity.Invoice;
import com.foliopath360.lms.entity.InvoiceEmailStatus;
import com.foliopath360.lms.entity.InvoiceEvent;
import com.foliopath360.lms.entity.Order;
import com.foliopath360.lms.repository.InvoiceEventRepository;
import com.foliopath360.lms.repository.InvoiceRepository;
import com.foliopath360.lms.service.impl.DefaultInvoiceEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceEmailServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceEventRepository invoiceEventRepository;
    @Mock private InvoicePdfStorage pdfStorage;
    @Mock private EmailService emailService;

    private DefaultInvoiceEmailService service;
    private Invoice invoice;

    @BeforeEach
    void setUp() {
        InvoiceProperties properties = new InvoiceProperties();
        properties.setInvoiceLinkBaseUrl("http://localhost:5173/student/invoices");
        service = new DefaultInvoiceEmailService(
                invoiceRepository, invoiceEventRepository, pdfStorage,
                emailService, properties);

        invoice = Invoice.builder()
                .id(UUID.randomUUID())
                .invoiceNumber("INV-2026-000001")
                .order(Order.builder().orderNumber("FP360-1").build())
                .studentName("Aarav Sharma")
                .courseName("Professional Java Development")
                .studentEmail("aarav@example.com")
                .totalAmount(new BigDecimal("11800.00"))
                .paymentDate(LocalDateTime.of(2026, 11, 24, 10, 25))
                .pdfStorageKey("invoices/2026/INV-2026-000001/a.pdf")
                .emailAttempts(0)
                .build();

        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        when(pdfStorage.exists(invoice.getPdfStorageKey())).thenReturn(true);
        when(pdfStorage.read(invoice.getPdfStorageKey()))
                .thenReturn("%PDF-1.4".getBytes(StandardCharsets.US_ASCII));
    }

    @Test
    void sendsInvoicePdfAsAttachmentAndMarksEmailSent() {
        boolean sent = service.send(invoice.getId(), null, "staff@foliopath360.com");

        assertThat(sent).isTrue();
        verify(emailService).sendInvoiceEmail(
                org.mockito.ArgumentMatchers.eq("aarav@example.com"),
                org.mockito.ArgumentMatchers.contains("INV-2026-000001"),
                org.mockito.ArgumentMatchers.contains("Thank you for enrolling"),
                any(byte[].class),
                org.mockito.ArgumentMatchers.eq("INV-2026-000001.pdf")
        );

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        assertThat(captor.getValue().getEmailStatus()).isEqualTo(InvoiceEmailStatus.SENT);
        assertThat(captor.getValue().getEmailAttempts()).isEqualTo(1);
        assertThat(captor.getValue().getEmailSentAt()).isNotNull();
        verify(invoiceEventRepository).save(any(InvoiceEvent.class));
    }

    @Test
    void recordsFailureWithoutThrowingSoInvoiceRemainsIntact() {
        doThrow(new RuntimeException("SMTP down"))
                .when(emailService).sendInvoiceEmail(
                        anyString(), anyString(), anyString(),
                        any(byte[].class), anyString());

        boolean sent = service.send(invoice.getId(), null, "staff@foliopath360.com");

        assertThat(sent).isFalse();
        assertThat(invoice.getEmailStatus()).isEqualTo(InvoiceEmailStatus.FAILED);
        assertThat(invoice.getEmailAttempts()).isEqualTo(1);
        assertThat(invoice.getLastEmailError()).contains("SMTP down");
        verify(invoiceEventRepository).save(any(InvoiceEvent.class));
    }
}
