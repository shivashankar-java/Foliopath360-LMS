package com.foliopath360.lms.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {

    private UUID itemId;

    private UUID courseId;

    private String courseCode;

    private String courseTitle;

    private String thumbnailUrl;

    private BigDecimal price;

    private LocalDateTime addedAt;
}
