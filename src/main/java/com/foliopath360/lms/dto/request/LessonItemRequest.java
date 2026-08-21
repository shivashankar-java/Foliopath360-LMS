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
public class LessonItemRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 500)
    private String description;

    private String content;

    private String codeContent;

    @Size(max = 50)
    private String codeLanguage;

    @NotNull
    private Integer displayOrder;
}
