package com.foliopath360.lms.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactUsResponse {

    private UUID id;
    private String fullName;
    private String mobileNumber;
    private String email;
    private String currentPosition;
    private String location;
    private LocalDateTime createdDt;
}
