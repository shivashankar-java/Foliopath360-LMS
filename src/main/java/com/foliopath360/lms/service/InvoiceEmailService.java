package com.foliopath360.lms.service;

import java.util.UUID;

public interface InvoiceEmailService {

    boolean send(UUID invoiceId, String recipientEmail, String actorEmail);
}
