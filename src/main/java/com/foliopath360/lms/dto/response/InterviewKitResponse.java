package com.foliopath360.lms.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import com.foliopath360.lms.dto.response.InterviewKitModuleResponse;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewKitResponse {
    private UUID id;
    private String kitCode;
    private String name;
    private String slug;
    private String description;
    private String thumbnailUrl;
    private String level;
    private BigDecimal price;
    private String status;
    private LocalDateTime publishedAt;
    private Long questionCount;
    private Long enrollmentCount;
    private List<InterviewKitModuleResponse> modules;
    private List<InterviewKitQuestionResponse> questions;
}
