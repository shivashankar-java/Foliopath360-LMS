package com.foliopath360.lms.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockTestAttemptResponse {

    private UUID attemptId;
    private UUID mockTestId;
    private UUID userId;
    private Integer score;
    private Integer total;
    private Integer percentage;
    private Boolean passed;
    private LocalDateTime submittedAt;
}
