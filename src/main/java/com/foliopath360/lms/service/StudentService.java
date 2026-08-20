package com.foliopath360.lms.service;

import com.foliopath360.lms.dto.response.CourseResponse;
import com.foliopath360.lms.dto.response.StudentDashboardResponse;
import com.foliopath360.lms.entity.User;

import java.util.List;

public interface StudentService {

    List<CourseResponse> getAvailableCourses();

    StudentDashboardResponse getDashboard(User user);
}
