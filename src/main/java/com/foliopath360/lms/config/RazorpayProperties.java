package com.foliopath360.lms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Razorpay configuration. Values are read from environment variables so the
 * key secret never lives in source control.
 *
 * Test keys can be obtained from the Razorpay Dashboard (Test mode).
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "razorpay")
public class RazorpayProperties {

    private String keyId;

    private String keySecret;

    private String webhookSecret;

    private String currency = "INR";

    public boolean isConfigured() {
        return keyId != null && !keyId.isBlank()
                && keySecret != null && !keySecret.isBlank();
    }

    public boolean isWebhookConfigured() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }
}
