package com.foliopath360.lms.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewKitEnrollmentResponse {
    private UUID enrollmentId;
    private UUID kitId;
    private String kitName;
    private String enrollmentStatus;
    private LocalDateTime enrolledAt;
}
