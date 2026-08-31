package com.foliopath360.lms.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyRevenueResponse {

    private String month;
    private BigDecimal revenue;
}
