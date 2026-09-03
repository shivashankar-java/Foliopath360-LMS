package com.foliopath360.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewKitModuleRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotNull
    private Integer displayOrder;
}
