package com.foliopath360.lms.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentEnrolledCourseResponse {

    private UUID courseId;
    private String courseCode;
    private String title;
    private String slug;
    private String shortDescription;
    private String thumbnailUrl;
    private String level;
    private String enrollmentStatus;
    private Integer progressPercentage;
    private LocalDateTime enrolledAt;
    private LocalDateTime completedAt;
}
