package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.Order;
import com.foliopath360.lms.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
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

    List<Order> findAllByStatus(OrderStatus status);

    List<Order> findByStatusAndPaidAtAfter(OrderStatus status, LocalDateTime paidAtAfter);

    // Most recent order for a user that includes the given course in one of its items.
    @Query("SELECT o FROM Order o JOIN o.items i " +
            "WHERE o.user.id = :userId AND i.course.id = :courseId " +
            "AND o.status <> com.foliopath360.lms.entity.OrderStatus.CANCELLED " +
            "ORDER BY o.createdDt DESC")
    List<Order> findOrdersByUserIdAndCourseId(
            @Param("userId") UUID userId, @Param("courseId") UUID courseId);
}
