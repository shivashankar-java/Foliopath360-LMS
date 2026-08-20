package com.foliopath360.lms.mapper;

import com.foliopath360.lms.dto.request.RegisterRequest;
import com.foliopath360.lms.dto.response.RegisterResponse;
import com.foliopath360.lms.dto.response.UserResponse;
import com.foliopath360.lms.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "profileImage", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "mobileVerified", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "roles", ignore = true)
    User toEntity(RegisterRequest request);

    @Mapping(source = "id", target = "userId")
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "message", ignore = true)
    RegisterResponse toRegisterResponse(User user);

    @Mapping(target = "roles", ignore = true)
    UserResponse toUserResponse(User user);
}
