package com.foliopath360.lms.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MonthlyEnrollmentResponse {

    private String month;
    private long count;
}
