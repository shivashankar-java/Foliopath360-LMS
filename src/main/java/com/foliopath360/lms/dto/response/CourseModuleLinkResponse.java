package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseModuleLinkResponse {

    private UUID id;
    private UUID courseId;
    private String courseCode;
    private String title;
    private String shortDescription;
    private String status;
    private Integer displayOrder;
}