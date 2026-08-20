package com.foliopath360.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseRequest {

    @NotBlank
    @Size(max = 50)
    private String courseCode;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 250)
    private String slug;

    @Size(max = 500)
    private String shortDescription;

    private String description;

    @Size(max = 500)
    private String thumbnailUrl;

    @NotNull
    private String level;

    @Size(max = 50)
    private String language;

    private BigDecimal price;
}
