package com.foliopath360.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewKitQuestionRequest {

    @NotBlank
    private String questionType;

    @Size(max = 50)
    private String codeLanguage;

    @NotBlank
    private String question;

    private String codeSnippet;

    @NotBlank
    private String answer;

    @NotNull
    private Integer displayOrder;
}
