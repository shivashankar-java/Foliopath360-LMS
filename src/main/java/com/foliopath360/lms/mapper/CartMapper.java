package com.foliopath360.lms.mapper;

import com.foliopath360.lms.dto.response.CartItemResponse;
import com.foliopath360.lms.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "itemId", source = "id")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseCode", source = "course.courseCode")
    @Mapping(target = "courseTitle", source = "course.title")
    @Mapping(target = "thumbnailUrl", source = "course.thumbnailUrl")
    CartItemResponse toItemResponse(CartItem item);

    List<CartItemResponse> toItemResponses(List<CartItem> items);
}
