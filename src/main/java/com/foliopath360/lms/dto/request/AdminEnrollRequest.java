package com.foliopath360.lms.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminEnrollRequest {

    @NotNull
    private UUID studentId;

    @NotNull
    private UUID courseId;
}
