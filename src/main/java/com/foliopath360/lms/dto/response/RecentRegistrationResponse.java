package com.foliopath360.lms.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecentRegistrationResponse {

    private UUID userId;

    private String firstName;

    private String lastName;

    private String email;

    private Set<String> roles;

    private String status;

    private LocalDateTime registeredAt;
}
