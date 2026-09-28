package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgrammingTestCaseResponse {

    private UUID id;
    private String input;
    private String expectedOutput;
    private Boolean isPublic;
    private Integer displayOrder;
}