package com.foliopath360.lms.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentEnrolledKitResponse {
    private UUID kitId;
    private String kitCode;
    private String name;
    private String slug;
    private String description;
    private String thumbnailUrl;
    private String level;
    private BigDecimal price;
    private String enrollmentStatus;
    private LocalDateTime enrolledAt;
}
