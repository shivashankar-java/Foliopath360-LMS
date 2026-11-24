package com.foliopath360.lms.repository;

import com.foliopath360.lms.dto.request.InvoiceFilterRequest;
import com.foliopath360.lms.entity.Invoice;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.util.InvoiceDateParser;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class InvoiceSpecification {

    private InvoiceSpecification() {
    }

    public static Specification<Invoice> fromFilter(InvoiceFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (InvoiceDateParser.hasText(filter.getInvoiceNumber())) {
                String value = "%" + filter.getInvoiceNumber().trim()
                        .toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(
                        cb.lower(root.get("invoiceNumber")), value));
            }

            Join<Invoice, User> student = root.join("student", JoinType.LEFT);

            if (InvoiceDateParser.hasText(filter.getStudentName())) {
                String value = "%" + filter.getStudentName().trim()
                        .toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("studentName")), value),
                        cb.like(cb.lower(student.get("firstName")), value),
                        cb.like(cb.lower(student.get("lastName")), value),
                        cb.like(cb.concat(
                                        cb.lower(student.get("firstName")),
                                        cb.concat(cb.literal(" "),
                                                cb.lower(student.get("lastName")))),
                                value)
                ));
            }

            if (InvoiceDateParser.hasText(filter.getStudentId())) {
                try {
                    UUID studentId = UUID.fromString(filter.getStudentId().trim());
                    predicates.add(cb.equal(student.get("id"), studentId));
                } catch (IllegalArgumentException ex) {
                    predicates.add(cb.disjunction());
                }
            }

            if (InvoiceDateParser.hasText(filter.getEmail())) {
                String value = "%" + filter.getEmail().trim()
                        .toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.like(cb.lower(root.get("studentEmail")), value));
            }

            if (filter.getCourseId() != null) {
                predicates.add(cb.equal(
                        root.join("course", JoinType.LEFT).get("id"),
                        filter.getCourseId()));
            }

            if (InvoiceDateParser.hasText(filter.getPaymentStatus())) {
                predicates.add(cb.equal(
                        cb.upper(root.get("paymentStatus")),
                        filter.getPaymentStatus().trim().toUpperCase(Locale.ROOT)));
            }

            LocalDate from = InvoiceDateParser.parse(filter.getDateFrom(), "dateFrom");
            LocalDate to = InvoiceDateParser.parse(filter.getDateTo(), "dateTo");

            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("paymentDate"), from.atStartOfDay()));
            }
            if (to != null) {
                predicates.add(cb.lessThan(
                        root.get("paymentDate"), to.plusDays(1).atStartOfDay()));
            }

            query.distinct(true);
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

}
