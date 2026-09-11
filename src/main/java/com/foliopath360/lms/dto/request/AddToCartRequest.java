package com.foliopath360.lms.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {

    // Exactly one of courseId / kitId must be provided
    private UUID courseId;

    private UUID kitId;
}
