package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.response.OrderResponse;
import com.foliopath360.lms.entity.User;

import java.util.List;
import java.util.UUID;

public interface OrderService {

    OrderResponse checkout(User student);

    List<OrderResponse> getMyOrders(User student);

    OrderResponse getOrder(User student, UUID orderId);
}
