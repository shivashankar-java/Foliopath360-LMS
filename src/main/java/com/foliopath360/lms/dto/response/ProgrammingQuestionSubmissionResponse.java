package com.foliopath360.lms.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammingQuestionSubmissionResponse {

    private UUID id;
    private UUID questionId;
    private String questionTitle;
    private String language;
    private String code;
    private boolean accepted;
    private String status;
    private int totalTestCases;
    private int passedTestCases;
    private long executionTimeMs;
    private Long memoryKb;
    private LocalDateTime submittedAt;
}