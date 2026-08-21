package com.foliopath360.lms.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {

    @NotBlank
    private String usernameOrEmail;

    @NotBlank
    private String password;

    @NotBlank(message = "Captcha id is required")
    private String captchaId;

    @NotBlank(message = "Captcha code is required")
    private String captchaCode;


}
