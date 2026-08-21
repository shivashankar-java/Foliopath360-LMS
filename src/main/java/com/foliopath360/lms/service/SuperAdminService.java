package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.StaffCreateRequest;
import com.foliopath360.lms.dto.request.StaffStatusUpdateRequest;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.StaffResponse;
import com.foliopath360.lms.dto.response.SuperAdminDashboardResponse;

import java.util.List;
import java.util.UUID;

public interface SuperAdminService {

    SuperAdminDashboardResponse getDashboard();

    StaffResponse createStaff(StaffCreateRequest request);

    List<StaffResponse> getAllStaff();

    StaffResponse getStaffById(UUID userId);

    StaffResponse updateStaff(UUID userId, StaffCreateRequest request);

    StaffResponse updateStaffStatus(UUID userId, StaffStatusUpdateRequest request);

    MessageResponse resendSetupLink(UUID userId);

    MessageResponse deleteStaff(UUID userId);
}
