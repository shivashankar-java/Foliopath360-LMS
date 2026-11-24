package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.InvoiceFilterRequest;
import com.foliopath360.lms.dto.request.InvoiceResendEmailRequest;
import com.foliopath360.lms.dto.request.OfflinePaymentInvoiceRequest;
import com.foliopath360.lms.dto.response.InvoiceResponse;
import com.foliopath360.lms.entity.Payment;
import com.foliopath360.lms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InvoiceService {

    InvoiceResponse ensureInvoiceForPayment(Payment payment);

    InvoiceResponse generateForOfflinePayment(
            OfflinePaymentInvoiceRequest request, User actor);

    Page<InvoiceResponse> list(InvoiceFilterRequest filter, Pageable pageable);

    Page<InvoiceResponse> myInvoices(User student, Pageable pageable);

    InvoiceResponse getInvoice(UUID invoiceId, User requester);

    byte[] download(UUID invoiceId, User requester);

    InvoiceResponse resendEmail(UUID invoiceId,
                                InvoiceResendEmailRequest request,
                                User requester);
}
