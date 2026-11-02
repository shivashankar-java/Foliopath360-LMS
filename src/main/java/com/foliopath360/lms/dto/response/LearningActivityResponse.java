package com.foliopath360.lms.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LearningActivityResponse {
    private String id;
    private String type;
    private String action;
    private String title;
    private String description;
    private LocalDateTime occurredAt;
    private String targetId;
    private String targetUrl;
}
