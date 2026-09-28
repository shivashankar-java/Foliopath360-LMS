package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammingQuestionResponse {

    private UUID id;
    private UUID courseId;
    private String questionCode;
    private String title;
    private String problemStatement;
    private String difficulty;
    private List<String> allowedLanguages;
    private String inputFormat;
    private String outputFormat;
    private String constraints;
    private String sampleInput;
    private String sampleOutput;
    private Integer displayOrder;
    private List<ProgrammingTestCaseResponse> testCases;

    /**
     * Solved status derived from the requesting student's latest submissions:
     * NOT_ATTEMPTED, ATTEMPTED or ACCEPTED. Null for staff/admins.
     */
    private String studentStatus;
}