package com.foliopath360.lms.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfileResponse {

    private UUID userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String mobileNumber;
    private String studentCode;
    private LocalDate dateOfBirth;
    private String gender;
    private String qualification;
    private String occupation;
    private String bio;
    private String address;
    private String city;
    private String state;
    private String country;
    private LocalDateTime joinedAt;
}
