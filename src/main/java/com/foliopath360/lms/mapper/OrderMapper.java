package com.foliopath360.lms.mapper;

import com.foliopath360.lms.dto.response.OrderItemResponse;
import com.foliopath360.lms.dto.response.OrderResponse;
import com.foliopath360.lms.entity.Order;
import com.foliopath360.lms.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "itemId", source = "id")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseCode", source = "course.courseCode")
    @Mapping(target = "courseTitle", source = "courseTitle")
    OrderItemResponse toItemResponse(OrderItem item);

    List<OrderItemResponse> toItemResponses(List<OrderItem> items);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "createdAt", source = "createdDt")
    @Mapping(target = "items", source = "items")
    OrderResponse toResponse(Order order);

    List<OrderResponse> toResponses(List<Order> orders);
}
