package com.foliopath360.lms.service;

import com.foliopath360.lms.entity.Invoice;

public interface InvoicePdfService {

    byte[] generate(Invoice invoice);
}
