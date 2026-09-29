package com.foliopath360.lms.dto.response;

import lombok.*;

/**
 * Whether the current user is allowed into the standalone programming
 * question bank. Staff always have access; a student needs at least one
 * non-dropped course enrollment OR one non-dropped interview kit enrollment.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammingQuestionAccessResponse {

    private boolean hasAccess;
    private long enrolledCourseCount;
    private long enrolledKitCount;
    private String message;
}
