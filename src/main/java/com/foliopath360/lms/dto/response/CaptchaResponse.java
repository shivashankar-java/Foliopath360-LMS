package com.foliopath360.lms.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CaptchaResponse {

    private String captchaId;

    // PNG image as data URI: data:image/png;base64,<base64>
    private String image;
}
