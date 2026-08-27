package com.foliopath360.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewKitRequest {

    @NotBlank @Size(max = 200)
    private String name;

    @Size(max = 500)
    private String description;

    @Size(max = 500)
    private String thumbnailUrl;

    @NotNull
    private String level;

    private BigDecimal price;
}
