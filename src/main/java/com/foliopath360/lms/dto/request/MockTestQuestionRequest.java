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
public class MockTestQuestionRequest {

    @Size(max = 50)
    private String questionCode;

    @NotNull
    private String questionType;

    @NotBlank
    @Size(max = 2000)
    private String questionText;

    private String codeContent;

    @Size(max = 50)
    private String codeLanguage;

    /**
     * Correct option label (A, B, C or D).
     */
    @NotBlank
    @Size(max = 1)
    private String correctOption;

    @NotNull
    private Integer displayOrder;

    @NotEmpty
    @Valid
    private List<MockTestOptionRequest> options;
}
