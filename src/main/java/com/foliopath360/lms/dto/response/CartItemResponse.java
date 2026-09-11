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

    // "COURSE" or "KIT"
    private String itemType;

    private UUID courseId;

    private String courseCode;

    private String courseTitle;

    private UUID kitId;

    private String kitCode;

    private String kitName;

    private String thumbnailUrl;

    private BigDecimal price;

    private LocalDateTime addedAt;
}
