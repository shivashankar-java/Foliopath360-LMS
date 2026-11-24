package com.foliopath360.lms.service;

public interface InvoicePdfStorage {

    String store(String invoiceNumber, byte[] pdf);

    byte[] read(String storageKey);

    boolean exists(String storageKey);
}
