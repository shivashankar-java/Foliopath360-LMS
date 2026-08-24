package com.foliopath360.lms.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentMockTestScoreResponse {

    private UUID attemptId;

    private UUID mockTestId;

    private String testTitle;

    private String moduleName;

    private String courseTitle;

    private Integer score;

    private Integer totalQuestions;

    private Integer percentage;

    private Boolean passed;

    private LocalDateTime submittedAt;
}
