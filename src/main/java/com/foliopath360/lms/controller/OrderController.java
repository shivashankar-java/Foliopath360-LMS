package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.response.OrderResponse;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/student/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class OrderController {

    private final OrderService orderService;

    // Checkout: converts the active cart into an order
    @PostMapping
    public ResponseEntity<OrderResponse> checkout(
            @AuthenticationPrincipal User student
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.checkout(student));
    }

    // My order history
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal User student
    ) {
        return ResponseEntity.ok(orderService.getMyOrders(student));
    }

    // Single order details
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @AuthenticationPrincipal User student,
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(orderService.getOrder(student, orderId));
    }
}
