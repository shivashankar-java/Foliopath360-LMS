package com.foliopath360.lms.service.impl;

import com.foliopath360.lms.dto.response.StaffDashboardResponse;
import com.foliopath360.lms.entity.Staff;
import com.foliopath360.lms.entity.User;
import com.foliopath360.lms.repository.StaffRepository;
import com.foliopath360.lms.repository.UserRepository;
import com.foliopath360.lms.service.StaffLmsService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class StaffLmsServiceImpl implements StaffLmsService {

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;

    public StaffLmsServiceImpl(
            StaffRepository staffRepository,
            UserRepository userRepository
    ) {
        this.staffRepository = staffRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public StaffDashboardResponse getDashboard(User user) {

        long totalStudents = userRepository.countByRolesRoleName("STUDENT");

        Staff staff = staffRepository.findByUserId(user.getId()).orElse(null);

        return StaffDashboardResponse.builder()
                .staffCode(staff != null ? staff.getStaffCode() : null)
                .designation(staff != null ? staff.getDesignation() : null)
                .department(staff != null ? staff.getDepartment() : null)
                .totalStudents(totalStudents)
                .build();
    }
}
