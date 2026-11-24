package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID>,
        JpaSpecificationExecutor<Invoice> {

    Optional<Invoice> findByPaymentId(UUID paymentId);

    Optional<Invoice> findByOrderId(UUID orderId);

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByPaymentId(UUID paymentId);

    Page<Invoice> findByStudentId(UUID studentId, Pageable pageable);
}
