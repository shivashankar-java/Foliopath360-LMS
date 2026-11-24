package com.foliopath360.lms.entity;

public enum InvoiceEventType {
    CREATED,
    PDF_GENERATED,
    PDF_REGENERATED,
    EMAIL_SENT,
    EMAIL_FAILED,
    EMAIL_RETRIED,
    PDF_DOWNLOADED,
    OFFLINE_PAYMENT_RECORDED
}
