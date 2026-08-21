package com.foliopath360.lms.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentEnrollmentResponse {

    private UUID enrollmentId;

    private UUID courseId;

    private String courseCode;

    private String courseTitle;

    private String studentName;

    private String studentEmail;

    private String enrollmentStatus;

    private Integer progressPercentage;

    private LocalDateTime enrolledAt;
}
