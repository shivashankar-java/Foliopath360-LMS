package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.AddToCartRequest;
import com.foliopath360.lms.dto.response.AddToCartResponse;
import com.foliopath360.lms.dto.response.CartResponse;
import com.foliopath360.lms.entity.User;

import java.util.UUID;

public interface CartService {

    AddToCartResponse addToCart(User student, AddToCartRequest request);

    CartResponse getCart(User student);

    void removeCartItem(User student, UUID itemId);

    void clearCart(User student);
}
