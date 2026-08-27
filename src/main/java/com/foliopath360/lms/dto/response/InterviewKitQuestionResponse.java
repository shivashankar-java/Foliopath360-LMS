package com.foliopath360.lms.dto.response;

import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterviewKitQuestionResponse {
    private UUID id;
    private UUID kitId;
    private String questionType;
    private String codeLanguage;
    private String question;
    private String codeSnippet;
    private String answer;
    private Integer displayOrder;
}
