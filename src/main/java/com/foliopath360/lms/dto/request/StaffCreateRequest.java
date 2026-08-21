package com.foliopath360.lms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffCreateRequest {

    @NotBlank
    @Email
    @Size(max = 150)
    private String email;

    @NotBlank
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @Size(max = 20)
    private String mobileNumber;

    @Size(max = 150)
    private String designation;

    @Size(max = 150)
    private String department;

    @Size(max = 200)
    private String qualification;

    @Size(max = 200)
    private String specialization;
}
