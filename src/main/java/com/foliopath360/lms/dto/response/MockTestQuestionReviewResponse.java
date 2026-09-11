package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockTestQuestionReviewResponse {

    private UUID questionId;
    private String questionText;
    private String questionType;
    private String codeContent;
    private String codeLanguage;

    /**
     * The option label the student picked (A-D), or null if unanswered.
     */
    private String selectedAnswer;

    /**
     * The correct option label (A-D).
     */
    private String correctAnswer;

    /**
     * Optional admin-provided explanation / solution (may be null).
     */
    private String solution;

    private List<MockTestOptionResponse> options;
}