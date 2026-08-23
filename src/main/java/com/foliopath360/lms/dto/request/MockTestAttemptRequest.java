package com.foliopath360.lms.dto.request;

import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockTestAttemptRequest {

    /**
     * Answers keyed by mock test question id, value is the selected option label (A-D).
     */
    private Map<String, String> answers;

    private String submittedAt;
}
