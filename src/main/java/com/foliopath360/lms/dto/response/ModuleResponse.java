package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleResponse {

    private UUID id;
    private UUID courseId;
    private String moduleCode;
    private String title;
    private String description;
    private Integer displayOrder;
    private String status;
    private List<LessonResponse> lessons;
}
