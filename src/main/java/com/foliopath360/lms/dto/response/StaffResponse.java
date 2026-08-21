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
public class StaffResponse {

    private UUID userId;
    private UUID staffId;
    private String staffCode;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String mobileNumber;
    private String designation;
    private String department;
    private String qualification;
    private Integer experienceYears;
    private String specialization;
    private Boolean enabled;
    private String status;
    private Set<String> roles;
    private LocalDateTime joinedAt;
}
