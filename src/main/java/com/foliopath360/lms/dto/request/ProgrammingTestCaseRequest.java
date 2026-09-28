package com.foliopath360.lms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammingTestCaseRequest {

    private String input;

    private String expectedOutput;

    @NotNull
    private Boolean isPublic;

    @NotNull
    @jakarta.validation.constraints.Min(1)
    private Integer displayOrder;
}