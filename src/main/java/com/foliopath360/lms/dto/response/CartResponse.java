package com.foliopath360.lms.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartResponse {

    private UUID cartId;

    private String status;

    private Integer itemCount;

    private List<CartItemResponse> items;

    private BigDecimal subtotal;

    private BigDecimal discount;

    private BigDecimal total;
}
