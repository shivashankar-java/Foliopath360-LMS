package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.response.OrderResponse;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.mapper.OrderMapper;
import com.foliopath360.lms.repository.CartRepository;
import com.foliopath360.lms.repository.CourseRepository;
import com.foliopath360.lms.repository.EnrollmentRepository;
import com.foliopath360.lms.repository.OrderRepository;
import com.foliopath360.lms.service.OrderService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    private final CartRepository cartRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    public OrderServiceImpl(
            CartRepository cartRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            OrderRepository orderRepository,
            OrderMapper orderMapper
    ) {
        this.cartRepository = cartRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.orderRepository = orderRepository;
        this.orderMapper = orderMapper;
    }

    @Override
    public OrderResponse checkout(User student) {

        Cart cart = cartRepository
                .findByUserIdAndStatus(student.getId(), CartStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("Your cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new IllegalStateException("Your cart is empty");
        }

        // Re-validate every course and recalculate prices from the live data.
        // The cart snapshot is informational only; the order is authoritative.
        BigDecimal subtotal = BigDecimal.ZERO;

        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {

            Course course = courseRepository.findById(cartItem.getCourse().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Course", "id", cartItem.getCourse().getId()));

            if (course.getStatus() != CourseStatus.PUBLISHED) {
                throw new IllegalArgumentException(
                        "Course '" + course.getTitle()
                                + "' is no longer available. Please remove it from your cart.");
            }

            boolean alreadyPurchased = enrollmentRepository
                    .existsByUserIdAndCourseIdAndStatusNot(
                            student.getId(), course.getId(), EnrollmentStatus.DROPPED);

            if (alreadyPurchased) {
                throw new IllegalArgumentException(
                        "You have already purchased '"
                                + course.getTitle() + "'. Please remove it from your cart.");
            }

            BigDecimal price = course.getPrice();

            OrderItem orderItem = OrderItem.builder()
                    .order(null) // set below once the order exists
                    .course(course)
                    .courseTitle(course.getTitle())
                    .price(price)
                    .build();

            orderItems.add(orderItem);
            subtotal = subtotal.add(price);
        }

        BigDecimal discountAmount = BigDecimal.ZERO; // coupons come later

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(student)
                .totalAmount(subtotal)
                .discountAmount(discountAmount)
                .finalAmount(subtotal.subtract(discountAmount))
                .currency("INR")
                .status(OrderStatus.CREATED)
                .build();

        for (OrderItem item : orderItems) {
            item.setOrder(order);
            order.getItems().add(item);
        }

        Order savedOrder = orderRepository.save(order);

        // The cart has been converted into an order; keep its items as history
        cart.setStatus(CartStatus.CHECKED_OUT);
        cartRepository.save(cart);

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    public List<OrderResponse> getMyOrders(User student) {

        return orderMapper.toResponses(
                orderRepository.findByUserIdOrderByCreatedDtDesc(student.getId()));
    }

    @Override
    public OrderResponse getOrder(User student, UUID orderId) {

        Order order = orderRepository.findByIdAndUserId(orderId, student.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order", "id", orderId));

        return orderMapper.toResponse(order);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /**
     * Human-readable, roughly sequential-looking order number,
     * e.g. FP360-LX2K9F-8A3C1B
     */
    private String generateOrderNumber() {

        String timestampPart = Long.toString(System.currentTimeMillis(), 36)
                .toUpperCase();
        String randomPart = UUID.randomUUID().toString()
                .replace("-", "")
                .substring(0, 6)
                .toUpperCase();

        return "FP360-" + timestampPart + "-" + randomPart;
    }
}
