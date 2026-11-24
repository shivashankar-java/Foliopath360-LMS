package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.config.InvoiceProperties;
import com.foliopath360.lms.entity.Invoice;
import com.foliopath360.lms.entity.InvoiceEmailStatus;
import com.foliopath360.lms.entity.InvoiceEvent;
import com.foliopath360.lms.entity.InvoiceEventType;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.repository.InvoiceEventRepository;
import com.foliopath360.lms.repository.InvoiceRepository;
import com.foliopath360.lms.service.EmailService;
import com.foliopath360.lms.service.InvoiceEmailService;
import com.foliopath360.lms.service.InvoicePdfStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class DefaultInvoiceEmailService implements InvoiceEmailService {

    private static final Logger log = LoggerFactory.getLogger(DefaultInvoiceEmailService.class);
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final InvoiceRepository invoiceRepository;
    private final InvoiceEventRepository invoiceEventRepository;
    private final InvoicePdfStorage pdfStorage;
    private final EmailService emailService;
    private final InvoiceProperties properties;

    public DefaultInvoiceEmailService(
            InvoiceRepository invoiceRepository,
            InvoiceEventRepository invoiceEventRepository,
            InvoicePdfStorage pdfStorage,
            EmailService emailService,
            InvoiceProperties properties
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceEventRepository = invoiceEventRepository;
        this.pdfStorage = pdfStorage;
        this.emailService = emailService;
        this.properties = properties;
    }

    @Override
    @Transactional
    public boolean send(UUID invoiceId, String recipientEmail, String actorEmail) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        if (invoice.getPdfStorageKey() == null || !pdfStorage.exists(invoice.getPdfStorageKey())) {
            log.error("Invoice {} has no stored PDF; email not sent", invoice.getInvoiceNumber());
            markFailure(invoice, "Invoice PDF is not available", actorEmail);
            return false;
        }

        String recipient = recipientEmail == null || recipientEmail.isBlank()
                ? invoice.getStudentEmail()
                : recipientEmail;

        String subject = "Your Foliopath 360 Course Payment Invoice – "
                + invoice.getInvoiceNumber();
        String html = buildEmailBody(invoice);
        String filename = invoice.getInvoiceNumber() + ".pdf";

        boolean retry = invoice.getEmailAttempts() != null && invoice.getEmailAttempts() > 0;
        try {
            emailService.sendInvoiceEmail(
                    recipient,
                    subject,
                    html,
                    pdfStorage.read(invoice.getPdfStorageKey()),
                    filename
            );

            invoice.setEmailStatus(InvoiceEmailStatus.SENT);
            invoice.setEmailAttempts((invoice.getEmailAttempts() == null ? 0
                    : invoice.getEmailAttempts()) + 1);
            invoice.setEmailSentAt(LocalDateTime.now());
            invoice.setLastEmailAttemptAt(LocalDateTime.now());
            invoice.setLastEmailError(null);
            invoiceRepository.save(invoice);

            invoiceEventRepository.save(InvoiceEvent.builder()
                    .invoice(invoice)
                    .eventType(retry ? InvoiceEventType.EMAIL_RETRIED
                            : InvoiceEventType.EMAIL_SENT)
                    .message("Invoice emailed to " + recipient)
                    .actorEmail(actorEmail)
                    .build());

            log.info("Invoice {} emailed to {}", invoice.getInvoiceNumber(), recipient);
            return true;
        } catch (Exception ex) {
            String error = ex.getMessage() == null
                    ? ex.getClass().getSimpleName() : ex.getMessage();
            markFailure(invoice, error, actorEmail);
            log.warn("Invoice email failed for {} ({}): {}",
                    invoice.getInvoiceNumber(), recipient, error);
            return false;
        }
    }

    private void markFailure(Invoice invoice, String error, String actorEmail) {
        invoice.setEmailStatus(InvoiceEmailStatus.FAILED);
        invoice.setEmailAttempts((invoice.getEmailAttempts() == null ? 0
                : invoice.getEmailAttempts()) + 1);
        invoice.setLastEmailAttemptAt(LocalDateTime.now());
        invoice.setLastEmailError(truncate(error));
        invoiceRepository.save(invoice);

        invoiceEventRepository.save(InvoiceEvent.builder()
                .invoice(invoice)
                .eventType(InvoiceEventType.EMAIL_FAILED)
                .message(truncate(error))
                .actorEmail(actorEmail)
                .build());
    }

    private String buildEmailBody(Invoice invoice) {
        String safeName = HtmlUtils.htmlEscape(nullToEmpty(invoice.getStudentName()));
        String safeCourse = HtmlUtils.htmlEscape(nullToEmpty(invoice.getCourseName()));
        String safeNumber = HtmlUtils.htmlEscape(invoice.getInvoiceNumber());
        String safeAmount = "₹ " + invoice.getTotalAmount().toPlainString();
        String safeDate = DATE_FORMATTER.format(invoice.getPaymentDate());
        String safeSupport = HtmlUtils.htmlEscape(properties.getCompany().getSupportEmail());
        String invoiceLink = properties.getInvoiceLinkBaseUrl().endsWith("/")
                ? properties.getInvoiceLinkBaseUrl() + invoice.getId()
                : properties.getInvoiceLinkBaseUrl() + "/" + invoice.getId();

        return """
                <div style="font-family:Arial,sans-serif;color:#18342d;background:#f6f8f7;padding:24px;">
                  <div style="max-width:600px;margin:auto;background:#ffffff;border:1px solid #d7e0dc;border-radius:10px;overflow:hidden;">
                    <div style="background:#08352b;color:#ffffff;padding:22px 28px;">
                      <div style="font-size:20px;font-weight:bold;letter-spacing:1px;">FOLIOPATH 360</div>
                      <div style="font-size:12px;color:#e8b23e;">Learn. Grow. Go Beyond.</div>
                    </div>
                    <div style="padding:28px;">
                      <h2 style="margin-top:0;color:#08352b;">Payment received, %s!</h2>
                      <p>Thank you for enrolling with Foliopath 360! Your payment has been
                         successfully received. Please find your invoice attached for your records.</p>
                      <table style="width:100%%;border-collapse:collapse;margin:18px 0;">
                        <tr><td style="padding:8px 0;color:#5a6964;">Invoice Number</td>
                            <td style="padding:8px 0;text-align:right;font-weight:bold;">%s</td></tr>
                        <tr><td style="padding:8px 0;color:#5a6964;">Course</td>
                            <td style="padding:8px 0;text-align:right;font-weight:bold;">%s</td></tr>
                        <tr><td style="padding:8px 0;color:#5a6964;">Amount Paid</td>
                            <td style="padding:8px 0;text-align:right;font-weight:bold;">%s</td></tr>
                        <tr><td style="padding:8px 0;color:#5a6964;">Payment Date</td>
                            <td style="padding:8px 0;text-align:right;font-weight:bold;">%s</td></tr>
                      </table>
                      <p style="margin:24px 0;">
                        <a href="%s" style="background:#107e5f;color:#ffffff;padding:12px 22px;
                           border-radius:6px;text-decoration:none;display:inline-block;">
                           View Invoice in Dashboard</a>
                      </p>
                      <p style="color:#5a6964;font-size:12px;">
                        The download link requires you to sign in to your Foliopath 360 account.
                        For support, contact %s.
                      </p>
                    </div>
                    <div style="background:#f6f8f7;padding:16px 28px;color:#5a6964;font-size:11px;">
                      This is an automatically generated email. Please do not reply.
                    </div>
                  </div>
                </div>
                """.formatted(safeName, safeNumber, safeCourse, safeAmount, safeDate,
                    HtmlUtils.htmlEscape(invoiceLink), safeSupport);
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 900 ? value.substring(0, 897) + "..." : value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
