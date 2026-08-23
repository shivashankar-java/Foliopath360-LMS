package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockTestQuestionResponse {

    private UUID id;
    private String questionCode;
    private String questionType;
    private String questionText;
    private String codeContent;
    private String codeLanguage;

    /**
     * Populated only for SUPER_ADMIN / STAFF requests; null for students.
     */
    private String correctOption;

    private Integer displayOrder;
    private List<MockTestOptionResponse> options;
}
