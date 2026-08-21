package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.ResetPasswordByAdminRequest;
import com.foliopath360.lms.dto.request.StaffCreateRequest;
import com.foliopath360.lms.dto.request.StaffStatusUpdateRequest;
import com.foliopath360.lms.dto.request.StudentStatusUpdateRequest;
import com.foliopath360.lms.dto.response.MessageResponse;
import com.foliopath360.lms.dto.response.StaffResponse;
import com.foliopath360.lms.dto.response.StudentAdminResponse;
import com.foliopath360.lms.dto.response.StudentProfileAdminResponse;
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

    MessageResponse resetStaffPassword(UUID userId, ResetPasswordByAdminRequest request);

    MessageResponse deleteStaff(UUID userId);

    // ---------------- Student management ----------------

    List<StudentAdminResponse> getAllStudents(String search);

    StudentAdminResponse getStudentDetail(UUID userId);

    StudentProfileAdminResponse getStudentProfile(UUID userId);

    StudentAdminResponse updateStudentStatus(UUID userId, StudentStatusUpdateRequest request);

    MessageResponse resetStudentPassword(UUID userId, ResetPasswordByAdminRequest request);
}
