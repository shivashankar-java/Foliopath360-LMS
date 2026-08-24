package com.foliopath360.lms.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Razorpay signature helpers.
 *
 * Payment verification:
 *   signature = HMAC_SHA256(key_secret, razorpay_order_id + "|" + razorpay_payment_id)
 *
 * Webhook verification:
 *   signature = HMAC_SHA256(webhook_secret, raw_request_body)
 */
public final class RazorpaySignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private RazorpaySignatureUtil() {
    }

    public static String hmacSha256Hex(String secret, String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to compute HMAC-SHA256 signature", e);
        }
    }

    /**
     * Constant-time comparison of the expected vs received signature.
     */
    public static boolean isValidSignature(
            String secret, String payload, String receivedSignature) {

        if (secret == null || secret.isBlank()
                || payload == null
                || receivedSignature == null || receivedSignature.isBlank()) {
            return false;
        }

        String expected = hmacSha256Hex(secret, payload);

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                receivedSignature.trim().getBytes(StandardCharsets.UTF_8));
    }
}
