package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonItemResponse {

    private UUID id;

    private String title;

    private String description;

    private String content;

    private String codeContent;

    private String codeLanguage;

    private Integer displayOrder;
}
