package com.foliopath360.lms.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactUsRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    @Pattern(
        regexp = "^[A-Za-z][A-Za-z .'-]*$",
        message = "Full name can only contain letters, spaces, dots, apostrophes and hyphens"
    )
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Size(max = 20, message = "Mobile number must not exceed 20 characters")
    @Pattern(
        regexp = "^\\+?[0-9][0-9\\s-]{7,19}$",
        message = "Mobile number must be a valid phone number"
    )
    private String mobileNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @NotBlank(message = "Current position is required")
    @Pattern(
        regexp = "STUDENT|DEVELOPER|WORKING_PROFESSIONAL|FREELANCER|DEVOPS_ENGINEER|TEST_ENGINEER|QA_ENGINEER|OTHER",
        message = "Current position is not a valid option"
    )
    private String currentPosition;

    @Size(max = 150, message = "Location must not exceed 150 characters")
    private String location;
}
