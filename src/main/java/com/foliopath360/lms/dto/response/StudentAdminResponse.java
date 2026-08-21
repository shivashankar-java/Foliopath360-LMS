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
public class StudentAdminResponse {

    private UUID userId;

    private String username;

    private String email;

    private String firstName;

    private String lastName;

    private String mobileNumber;

    private Boolean enabled;

    private String status;

    private Boolean emailVerified;

    private Set<String> roles;

    private LocalDateTime registeredAt;
}
