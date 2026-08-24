package com.foliopath360.lms.controller;

import com.foliopath360.lms.dto.request.AddToCartRequest;
import com.foliopath360.lms.dto.response.AddToCartResponse;
import com.foliopath360.lms.dto.response.CartResponse;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/student/cart")
@RequiredArgsConstructor
@PreAuthorize("hasRole('STUDENT')")
public class CartController {

    private final CartService cartService;

    // Add course to cart
    @PostMapping("/items")
    public ResponseEntity<AddToCartResponse> addToCart(
            @AuthenticationPrincipal User student,
            @Valid @RequestBody AddToCartRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cartService.addToCart(student, request));
    }

    // Get my cart
    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @AuthenticationPrincipal User student
    ) {
        return ResponseEntity.ok(cartService.getCart(student));
    }

    // Remove a single item from the cart
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeCartItem(
            @AuthenticationPrincipal User student,
            @PathVariable UUID itemId
    ) {
        cartService.removeCartItem(student, itemId);
        return ResponseEntity.noContent().build();
    }

    // Clear all items from the cart
    @DeleteMapping("/clear")
    public ResponseEntity<MessageResponse> clearCart(
            @AuthenticationPrincipal User student
    ) {
        cartService.clearCart(student);
        return ResponseEntity.ok(
                MessageResponse.builder().message("Cart cleared").build()
        );
    }
}
