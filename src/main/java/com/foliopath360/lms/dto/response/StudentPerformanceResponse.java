package com.foliopath360.lms.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentPerformanceResponse {

    private UUID userId;

    private String username;

    private String email;

    private String firstName;

    private String lastName;

    private String mobileNumber;

    private Boolean enabled;

    private String status;

    private String studentCode;

    private LocalDateTime registeredAt;

    // ---- Performance summary ----

    private Long totalEnrolledCourses;

    private Long completedCourses;

    private Long inProgressCourses;

    private Double averageProgressPercentage;

    private Long totalMockTestsTaken;

    private Long mockTestsPassed;

    // ---- Details ----

    private List<StudentEnrolledCourseResponse> enrolledCourses;

    private List<StudentMockTestScoreResponse> mockTestScores;
}
