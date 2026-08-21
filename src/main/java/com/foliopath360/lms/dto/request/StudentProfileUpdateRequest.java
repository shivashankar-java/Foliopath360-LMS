package com.foliopath360.lms.dto.request;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentProfileUpdateRequest {

    private LocalDate dateOfBirth;
    private String gender;
    private String qualification;
    private String occupation;
    private String bio;
    private String address;
    private String city;
    private String state;
    private String country;
}
