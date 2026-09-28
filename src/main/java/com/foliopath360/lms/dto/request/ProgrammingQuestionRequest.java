package com.foliopath360.lms.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammingQuestionRequest {

    @Size(max = 50)
    private String questionCode;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    private String problemStatement;

    private String difficulty;

    private List<String> allowedLanguages;

    private String inputFormat;

    private String outputFormat;

    private String constraints;

    private String sampleInput;

    private String sampleOutput;

    @NotNull
    @Min(1)
    private Integer displayOrder;

    @NotEmpty
    @Valid
    private List<ProgrammingTestCaseRequest> testCases;
}