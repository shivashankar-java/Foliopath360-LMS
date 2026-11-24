package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.InvoiceEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceEventRepository extends JpaRepository<InvoiceEvent, UUID> {

    List<InvoiceEvent> findByInvoiceIdOrderByCreatedDtDesc(UUID invoiceId);
}
