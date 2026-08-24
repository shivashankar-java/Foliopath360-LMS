package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonResponse {

    private UUID id;
    private UUID moduleId;
    private UUID courseId;
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

    /**
     * True when the requester is not enrolled in the parent course
     * (content is withheld server-side).
     */
    private Boolean locked;

    private List<LessonItemResponse> items;
}
