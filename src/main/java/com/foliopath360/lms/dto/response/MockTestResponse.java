package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockTestResponse {

    private UUID id;
    private UUID moduleId;
    private String testCode;
    private String title;
    private String description;
    private Integer durationMinutes;
    private Integer passPercentage;
    private Integer displayOrder;
    private List<MockTestQuestionResponse> questions;
}
