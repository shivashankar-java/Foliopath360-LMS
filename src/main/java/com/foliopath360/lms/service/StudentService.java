package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.request.StudentProfileUpdateRequest;
import com.foliopath360.lms.dto.response.StudentDashboardResponse;
import com.foliopath360.lms.dto.response.StudentProfileResponse;
import com.foliopath360.lms.entity.User;

public interface StudentService {

    StudentDashboardResponse getDashboard(User user);

    StudentProfileResponse getProfile(User user);

    StudentProfileResponse updateProfile(User user, StudentProfileUpdateRequest request);
}
