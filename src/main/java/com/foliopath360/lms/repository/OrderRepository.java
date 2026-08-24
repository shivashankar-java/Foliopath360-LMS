package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.Order;
import com.foliopath360.lms.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserIdOrderByCreatedDtDesc(UUID userId);

    Optional<Order> findByIdAndUserId(UUID id, UUID userId);

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findFirstByUserIdAndStatusInOrderByCreatedDtDesc(
            UUID userId, List<OrderStatus> statuses);
}
