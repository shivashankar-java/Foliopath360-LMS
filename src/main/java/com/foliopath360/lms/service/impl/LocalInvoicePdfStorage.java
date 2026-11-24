package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.config.InvoiceProperties;
import com.foliopath360.lms.exception.InvoiceGenerationException;
import com.foliopath360.lms.service.InvoicePdfStorage;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class LocalInvoicePdfStorage implements InvoicePdfStorage {

    private static final String KEY_PREFIX = "invoices/";

    private final Path root;

    public LocalInvoicePdfStorage(InvoiceProperties properties) {
        String configured = properties.getStoragePath();
        if (configured == null || configured.isBlank()
                || configured.contains("${")) {
            configured = System.getProperty("java.io.tmpdir")
                    + "/foliopath360/invoices";
        }
        this.root = Paths.get(configured).toAbsolutePath().normalize();
    }

    @Override
    public String store(String invoiceNumber, byte[] pdf) {
        String safeNumber = invoiceNumber.replaceAll("[^A-Za-z0-9-]", "_");
        String key = KEY_PREFIX + LocalDate.now().getYear() + "/"
                + safeNumber + "/" + UUID.randomUUID() + ".pdf";

        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, pdf);
            return key;
        } catch (IOException ex) {
            throw new InvoiceGenerationException(
                    "Unable to store invoice PDF", ex);
        }
    }

    @Override
    public byte[] read(String storageKey) {
        Path source = resolve(storageKey);
        try {
            return Files.readAllBytes(source);
        } catch (IOException ex) {
            throw new InvoiceGenerationException(
                    "Unable to read invoice PDF", ex);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return false;
        }
        return Files.exists(resolve(storageKey));
    }

    private Path resolve(String storageKey) {
        if (storageKey == null || !storageKey.startsWith(KEY_PREFIX)) {
            throw new InvoiceGenerationException("Invalid invoice PDF storage key");
        }
        Path resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) {
            throw new InvoiceGenerationException("Invalid invoice PDF storage path");
        }
        return resolved;
    }
}
