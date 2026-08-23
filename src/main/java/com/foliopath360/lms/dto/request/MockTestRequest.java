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
public class MockTestRequest {

    @Size(max = 50)
    private String testCode;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 500)
    private String description;

    @NotNull
    @Min(1)
    private Integer durationMinutes;

    @NotNull
    @Min(0)
    @Max(100)
    private Integer passPercentage;

    @NotNull
    @Min(1)
    private Integer displayOrder;

    @NotEmpty
    @Valid
    private List<MockTestQuestionRequest> questions;
}
