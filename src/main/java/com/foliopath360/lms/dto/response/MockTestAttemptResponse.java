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
public class MockTestAttemptResponse {

    private UUID attemptId;
    private UUID mockTestId;
    private String testTitle;
    private UUID userId;
    private Integer score;
    private Integer total;
    private Integer percentage;
    private Boolean passed;
    private Integer passPercentage;
    private LocalDateTime submittedAt;

    /**
     * Per-question review data for the post-submission result page:
     * question, options, student's pick, correct pick and solution.
     */
    private List<MockTestQuestionReviewResponse> questions;
}
