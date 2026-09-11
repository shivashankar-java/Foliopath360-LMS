package com.foliopath360.lms.mapper;

import com.foliopath360.lms.dto.response.CartItemResponse;
import com.foliopath360.lms.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "itemId", source = "id")
    @Mapping(target = "itemType", expression = "java(item.getCourse() != null ? \"COURSE\" : (item.getKit() != null ? \"KIT\" : null))")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseCode", source = "course.courseCode")
    @Mapping(target = "courseTitle", source = "course.title")
    @Mapping(target = "kitId", source = "kit.id")
    @Mapping(target = "kitCode", source = "kit.kitCode")
    @Mapping(target = "kitName", source = "kit.name")
    @Mapping(target = "thumbnailUrl", expression = "java(item.getCourse() != null ? item.getCourse().getThumbnailUrl() : (item.getKit() != null ? item.getKit().getThumbnailUrl() : null))")
    CartItemResponse toItemResponse(CartItem item);

    List<CartItemResponse> toItemResponses(List<CartItem> items);
}
