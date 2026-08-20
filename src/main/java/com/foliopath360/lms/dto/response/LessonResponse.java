package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResponse {

    private UUID id;
    private UUID moduleId;
    private String lessonCode;
    private String title;
    private String description;
    private String lessonType;
    private String contentType;
    private String content;
    private String codeContent;
    private String codeLanguage;
    private String documentUrl;
    private Integer displayOrder;
    private Integer estimatedMinutes;
    private String status;
}
