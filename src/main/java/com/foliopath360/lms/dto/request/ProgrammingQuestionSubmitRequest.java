package com.foliopath360.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammingQuestionSubmitRequest {

    @NotBlank
    @Size(max = 50)
    private String language;

    @NotBlank
    @Size(max = 65536)
    private String code;
}