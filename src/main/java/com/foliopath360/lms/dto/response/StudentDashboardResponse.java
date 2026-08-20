package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentDashboardResponse {

    private UserResponse studentInfo;
    private long totalEnrolledCourses;
    private long completedCourses;
    private long inProgressCourses;
    private List<CourseResponse> availableCourses;
    private List<StudentEnrolledCourseResponse> enrolledCourses;
}
