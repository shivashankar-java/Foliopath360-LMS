package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.Payment;
import com.foliopath360.lms.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Payment> findFirstByOrderIdOrderByCreatedDtDesc(UUID orderId);

    List<Payment> findByOrderIdAndStatusInOrderByCreatedDtDesc(
            UUID orderId, List<PaymentStatus> statuses);

    List<Payment> findTop30ByStatusOrderByPaidAtDesc(PaymentStatus status);

    Optional<Payment> findFirstByOrderIdAndStatusOrderByCreatedDtDesc(
            UUID orderId, PaymentStatus status);

    List<Payment> findTop50ByStatusInOrderByPaidAtDesc(
            Collection<PaymentStatus> statuses);

    List<Payment> findByStatusInAndPaidAtAfter(
            Collection<PaymentStatus> statuses, LocalDateTime paidAtAfter);

    List<Payment> findByStatus(PaymentStatus status);

    // Sum of signed amounts for refunded payments on an order (negative values)
    // so it can be subtracted from the original paid amount.
    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p " +
            "WHERE p.order.id = :orderId " +
            "AND p.status = com.foliopath360.lms.entity.PaymentStatus.REFUNDED")
    BigDecimal sumRefundedByOrderId(@Param("orderId") UUID orderId);
}
