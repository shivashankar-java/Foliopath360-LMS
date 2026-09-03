package com.foliopath360.lms.dto.response;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewKitModuleResponse {
    private UUID id;
    private UUID kitId;
    private String name;
    private Integer displayOrder;
    private Long questionCount;
    private List<InterviewKitQuestionResponse> questions;
}
