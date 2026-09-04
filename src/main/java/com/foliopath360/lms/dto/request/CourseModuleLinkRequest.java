package com.foliopath360.lms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseModuleLinkRequest {

    @NotNull
    private UUID courseId;

    @NotNull
    private Integer displayOrder;
}