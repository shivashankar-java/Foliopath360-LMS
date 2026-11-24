package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.InvoiceFilterRequest;
import com.foliopath360.lms.dto.request.InvoiceResendEmailRequest;
import com.foliopath360.lms.dto.request.OfflinePaymentInvoiceRequest;
import com.foliopath360.lms.dto.response.InvoiceResponse;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springdoc.core.annotations.ParameterObject;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoices", description = "Foliopath 360 payment invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @PreAuthorize("hasAnyRole('STAFF', 'SUPER_ADMIN')")
    @Operation(summary = "List invoices with search, filters, pagination and sorting")
    public ResponseEntity<Page<InvoiceResponse>> list(
            @ParameterObject InvoiceFilterRequest filter,
            @ParameterObject @PageableDefault(size = 20,
                    sort = "invoiceDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(invoiceService.list(filter, pageable));
    }

    @GetMapping("/my-invoices")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "Get invoices belonging to the logged-in student")
    public ResponseEntity<Page<InvoiceResponse>> myInvoices(
            @AuthenticationPrincipal User student,
            @ParameterObject @PageableDefault(size = 20,
                    sort = "invoiceDate", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(invoiceService.myInvoices(student, pageable));
    }

    @GetMapping("/{invoiceId}")
    @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'SUPER_ADMIN')")
    @Operation(summary = "Get invoice details (students can only access their own invoices)")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @PathVariable UUID invoiceId,
            @AuthenticationPrincipal User requester
    ) {
        return ResponseEntity.ok(invoiceService.getInvoice(invoiceId, requester));
    }

    @GetMapping("/{invoiceId}/download")
    @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'SUPER_ADMIN')")
    @Operation(summary = "Download an invoice PDF as an attachment")
    public ResponseEntity<byte[]> download(
            @PathVariable UUID invoiceId,
            @AuthenticationPrincipal User requester
    ) {
        return pdfResponse(invoiceId, requester, "attachment");
    }

    @GetMapping("/{invoiceId}/pdf")
    @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'SUPER_ADMIN')")
    @Operation(summary = "Stream an invoice PDF inline")
    public ResponseEntity<byte[]> streamPdf(
            @PathVariable UUID invoiceId,
            @AuthenticationPrincipal User requester
    ) {
        return pdfResponse(invoiceId, requester, "inline");
    }

    @PostMapping("/{invoiceId}/resend-email")
    @PreAuthorize("hasAnyRole('STUDENT', 'STAFF', 'SUPER_ADMIN')")
    @Operation(summary = "Resend the invoice PDF by email")
    public ResponseEntity<InvoiceResponse> resendEmail(
            @PathVariable UUID invoiceId,
            @AuthenticationPrincipal User requester,
            @Valid @RequestBody(required = false) InvoiceResendEmailRequest request
    ) {
        return ResponseEntity.ok(invoiceService.resendEmail(
                invoiceId,
                request == null ? new InvoiceResendEmailRequest() : request,
                requester
        ));
    }

    @PostMapping("/offline")
    @PreAuthorize("hasAnyRole('STAFF', 'SUPER_ADMIN')")
    @Operation(summary = "Record an offline payment and generate its invoice")
    public ResponseEntity<InvoiceResponse> generateForOfflinePayment(
            @Valid @RequestBody OfflinePaymentInvoiceRequest request,
            @AuthenticationPrincipal User actor
    ) {
        return ResponseEntity.ok(
                invoiceService.generateForOfflinePayment(request, actor));
    }

    private ResponseEntity<byte[]> pdfResponse(UUID invoiceId, User requester,
                                                String disposition) {
        InvoiceResponse invoice = invoiceService.getInvoice(invoiceId, requester);
        byte[] pdf = invoiceService.download(invoiceId, requester);
        String filename = URLEncoder.encode(
                invoice.getInvoiceNumber() + ".pdf", StandardCharsets.UTF_8)
                .replace("+", " ");

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        disposition + "; filename=\"" + filename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, private")
                .contentLength(pdf.length)
                .body(pdf);
    }
}
