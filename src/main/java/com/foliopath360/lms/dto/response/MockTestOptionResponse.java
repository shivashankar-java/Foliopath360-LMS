package com.foliopath360.lms.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockTestOptionResponse {

    private UUID id;
    private String label;
    private String text;
    private Integer displayOrder;
}
