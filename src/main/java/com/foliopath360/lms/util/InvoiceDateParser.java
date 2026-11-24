package com.foliopath360.lms.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

public final class InvoiceDateParser {

    private static final List<DateTimeFormatter> FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ISO_LOCAL_DATE
    );

    private InvoiceDateParser() {
    }

    public static LocalDate parse(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return LocalDate.parse(trimmed, formatter);
            } catch (DateTimeParseException ignored) {
                // try the next supported format
            }
        }
        throw new IllegalArgumentException(
                fieldName + " must be in DD-MM-YYYY or YYYY-MM-DD format");
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
