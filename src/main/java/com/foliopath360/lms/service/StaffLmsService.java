package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.response.StaffDashboardResponse;
import com.foliopath360.lms.entity.User;

public interface StaffLmsService {

    StaffDashboardResponse getDashboard(User user);
}
