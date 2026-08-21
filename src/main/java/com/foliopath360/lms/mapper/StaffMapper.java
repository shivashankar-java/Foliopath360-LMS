package com.foliopath360.lms.mapper;

import com.foliopath360.lms.dto.request.StaffCreateRequest;
import com.foliopath360.lms.dto.response.StaffResponse;
import com.foliopath360.lms.entity.Staff;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StaffMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "staffCode", ignore = true)
    @Mapping(target = "bio", ignore = true)
    @Mapping(target = "experienceYears", ignore = true)
    @Mapping(target = "companyName", ignore = true)
    @Mapping(target = "linkedinUrl", ignore = true)
    @Mapping(target = "websiteUrl", ignore = true)
    @Mapping(target = "joinedAt", ignore = true)
    Staff toStaffEntity(StaffCreateRequest request);

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "id", target = "staffId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.firstName", target = "firstName")
    @Mapping(source = "user.lastName", target = "lastName")
    @Mapping(source = "user.mobileNumber", target = "mobileNumber")
    @Mapping(source = "user.enabled", target = "enabled")
    @Mapping(source = "user.status", target = "status")
    @Mapping(target = "roles", ignore = true)
    StaffResponse toStaffResponse(Staff staff);
}
