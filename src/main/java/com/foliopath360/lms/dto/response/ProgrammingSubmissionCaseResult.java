package com.foliopath360.lms.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammingSubmissionCaseResult {

    private Integer caseNumber;
    private Boolean passed;
    private Boolean hidden;
    private Long executionTimeMs;
    private String message;
}