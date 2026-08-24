package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.request.AddToCartRequest;
import com.foliopath360.lms.dto.response.AddToCartResponse;
import com.foliopath360.lms.dto.response.CartItemResponse;
import com.foliopath360.lms.dto.response.CartResponse;
import com.foliopath360.lms.entity.*;
import com.foliopath360.lms.exception.ResourceNotFoundException;
import com.foliopath360.lms.mapper.CartMapper;
import com.foliopath360.lms.repository.CartRepository;
import com.foliopath360.lms.repository.CourseRepository;
import com.foliopath360.lms.repository.EnrollmentRepository;
import com.foliopath360.lms.service.CartService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CartMapper cartMapper;

    public CartServiceImpl(
            CartRepository cartRepository,
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            CartMapper cartMapper
    ) {
        this.cartRepository = cartRepository;
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.cartMapper = cartMapper;
    }

    @Override
    public AddToCartResponse addToCart(User student, AddToCartRequest request) {

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Course", "id", request.getCourseId()));

        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new IllegalArgumentException(
                    "Only published courses can be added to the cart"
            );
        }

        if (course.getPrice() == null
                || course.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "This course is not available for purchase. You can enroll in it directly."
            );
        }

        boolean alreadyPurchased = enrollmentRepository
                .existsByUserIdAndCourseIdAndStatusNot(
                        student.getId(), course.getId(), EnrollmentStatus.DROPPED
                );

        if (alreadyPurchased) {
            throw new IllegalStateException(
                    "You have already purchased this course"
            );
        }

        Cart cart = getOrCreateActiveCart(student);

        boolean alreadyInCart = cart.getItems().stream()
                .anyMatch(i -> i.getCourse().getId().equals(course.getId()));

        if (alreadyInCart) {
            throw new IllegalStateException("Course already in cart");
        }

        // Snapshot the current price; checkout always re-validates against the live price
        CartItem item = CartItem.builder()
                .cart(cart)
                .course(course)
                .price(course.getPrice())
                .addedAt(LocalDateTime.now())
                .build();

        cart.getItems().add(item);
        cartRepository.save(cart);

        return AddToCartResponse.builder()
                .message("Course added to cart")
                .itemId(item.getId())
                .courseId(course.getId())
                .courseName(course.getTitle())
                .price(item.getPrice())
                .build();
    }

    @Override
    public CartResponse getCart(User student) {

        Cart cart = getOrCreateActiveCart(student);

        List<CartItemResponse> items = cartMapper.toItemResponses(cart.getItems());

        BigDecimal subtotal = cart.getItems().stream()
                .map(CartItem::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .status(cart.getStatus().name())
                .itemCount(items.size())
                .items(items)
                .subtotal(subtotal)
                .discount(BigDecimal.ZERO) // coupons/discounts come later
                .total(subtotal)
                .build();
    }

    @Override
    public void removeCartItem(User student, UUID itemId) {

        Cart cart = getOrCreateActiveCart(student);

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException("Cart item", "id", itemId));

        cart.getItems().remove(item);
        cartRepository.save(cart);
    }

    @Override
    public void clearCart(User student) {

        Cart cart = getOrCreateActiveCart(student);

        cart.getItems().clear();
        cartRepository.save(cart);
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Cart getOrCreateActiveCart(User student) {

        return cartRepository.findByUserIdAndStatus(student.getId(), CartStatus.ACTIVE)
                .orElseGet(() -> cartRepository.save(
                        Cart.builder()
                                .user(student)
                                .status(CartStatus.ACTIVE)
                                .build()
                ));
    }
}
