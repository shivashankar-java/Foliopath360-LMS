package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.Payment;
import com.foliopath360.lms.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findFirstByOrderIdOrderByCreatedDtDesc(UUID orderId);

    List<Payment> findByOrderIdAndStatusInOrderByCreatedDtDesc(
            UUID orderId, List<PaymentStatus> statuses);
}
