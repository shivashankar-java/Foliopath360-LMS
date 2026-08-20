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
public class LessonRequest {

    @NotBlank
    @Size(max = 50)
    private String lessonCode;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 500)
    private String description;

    @NotNull
    private String lessonType;

    @NotNull
    private String contentType;

    private String content;

    private String codeContent;

    @Size(max = 50)
    private String codeLanguage;

    @Size(max = 500)
    private String documentUrl;

    @NotNull
    private Integer displayOrder;

    private Integer estimatedMinutes;
}
