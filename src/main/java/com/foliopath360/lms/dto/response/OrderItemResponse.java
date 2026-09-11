package com.foliopath360.lms.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private UUID itemId;

    private UUID courseId;

    private String courseCode;

    private String courseTitle;

    private UUID kitId;

    private String kitName;

    private BigDecimal price;
}
