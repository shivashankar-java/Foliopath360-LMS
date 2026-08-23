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
public class MockTestOptionRequest {

    @NotBlank
    @Size(max = 1)
    private String label;

    @NotBlank
    @Size(max = 1000)
    private String text;

    @NotNull
    private Integer displayOrder;
}
