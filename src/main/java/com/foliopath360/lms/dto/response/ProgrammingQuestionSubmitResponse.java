package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammingQuestionSubmitResponse {

    private UUID questionId;
    private UUID submissionId;
    private String title;
    private String language;
    private boolean accepted;
    private int totalTestCases;
    private int passedTestCases;
    private int failedTestCases;
    private long totalExecutionTimeMs;
    private Long memoryKb;
    private String verdict;
    @Builder.Default
    private List<ProgrammingSubmissionCaseResult> caseResults = new ArrayList<>();
}