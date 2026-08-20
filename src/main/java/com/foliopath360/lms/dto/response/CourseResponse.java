package com.foliopath360.lms.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseResponse {

    private UUID id;
    private String courseCode;
    private String title;
    private String slug;
    private String shortDescription;
    private String description;
    private String thumbnailUrl;
    private String level;
    private String language;
    private BigDecimal price;
    private String status;
    private LocalDateTime publishedAt;
    private List<ModuleResponse> modules;
}
