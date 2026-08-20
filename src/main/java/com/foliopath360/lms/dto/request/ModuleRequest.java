package com.foliopath360.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleRequest {

    @NotBlank
    @Size(max = 50)
    private String moduleCode;

    @NotBlank
    @Size(max = 200)
    private String title;

    private String description;

    @NotNull
    private Integer displayOrder;
}
