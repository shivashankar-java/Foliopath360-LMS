package com.foliopath360.lms.service;

import com.foliopath360.lms.entity.CaptchaToken;
import com.foliopath360.lms.repository.CaptchaTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class CaptchaService {

    private static final String ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private static final int CODE_LENGTH = 5;
    private static final int IMAGE_WIDTH = 170;
    private static final int IMAGE_HEIGHT = 52;

    private final SecureRandom random = new SecureRandom();

    private final CaptchaTokenRepository captchaTokenRepository;

    private final long expiryMinutes;

    public CaptchaService(
            CaptchaTokenRepository captchaTokenRepository,
            @Value("${app.captcha.expiry-minutes}") long expiryMinutes
    ) {
        this.captchaTokenRepository = captchaTokenRepository;
        this.expiryMinutes = expiryMinutes;
    }

    @Transactional
    public CaptchaResult generateCaptcha() {

        // Housekeeping: remove expired captchas
        captchaTokenRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        String code = randomCode();
        String captchaId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        captchaTokenRepository.save(
                CaptchaToken.builder()
                        .id(captchaId)
                        .code(code)
                        .expiresAt(now.plusMinutes(expiryMinutes))
                        .createdAt(now)
                        .build()
        );

        String imageBase64 = drawImage(code);

        return new CaptchaResult(captchaId, imageBase64);
    }

    // REQUIRES_NEW: the captcha must be consumed even if the outer
    // login transaction rolls back (e.g. wrong password)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean validateCaptcha(String captchaId, String answer) {

        if (captchaId == null || answer == null || answer.isBlank()) {
            return false;
        }

        return captchaTokenRepository.findById(captchaId)
                .map(token -> {
                    captchaTokenRepository.delete(token);

                    if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
                        return false;
                    }

                    return token.getCode().equalsIgnoreCase(answer.trim());
                })
                .orElse(false);
    }

    private String randomCode() {

        StringBuilder sb = new StringBuilder(CODE_LENGTH);

        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }

        return sb.toString();
    }

    private String drawImage(String code) {

        BufferedImage image = new BufferedImage(
                IMAGE_WIDTH, IMAGE_HEIGHT, BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = image.createGraphics();

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        // Background
        g.setColor(new Color(245, 245, 245));
        g.fillRect(0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);

        // Noise lines
        for (int i = 0; i < 6; i++) {
            g.setColor(randomPastel());
            g.setStroke(new BasicStroke(1 + random.nextInt(2)));
            g.drawLine(
                    random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT),
                    random.nextInt(IMAGE_WIDTH), random.nextInt(IMAGE_HEIGHT)
            );
        }

        // Noise dots
        for (int i = 0; i < 40; i++) {
            g.setColor(randomPastel());
            g.fillOval(
                    random.nextInt(IMAGE_WIDTH),
                    random.nextInt(IMAGE_HEIGHT),
                    2, 2
            );
        }

        // Characters with rotation
        Font[] fonts = {
                new Font("Arial", Font.BOLD, 30),
                new Font("Verdana", Font.ITALIC, 28),
                new Font("Tahoma", Font.BOLD, 32)
        };

        for (int i = 0; i < code.length(); i++) {

            char ch = code.charAt(i);

            g.setFont(fonts[random.nextInt(fonts.length)]);
            g.setColor(randomColor());

            double rotation = (random.nextDouble() - 0.5) * 0.6;

            int x = 18 + i * 28;
            int y = IMAGE_HEIGHT / 2 + 10;

            g.rotate(rotation, x, y);
            g.drawString(String.valueOf(ch), x, y);
            g.rotate(-rotation, x, y);
        }

        g.dispose();

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate captcha image", e);
        }
    }

    private Color randomColor() {
        return new Color(
                20 + random.nextInt(100),
                20 + random.nextInt(100),
                20 + random.nextInt(100)
        );
    }

    private Color randomPastel() {
        return new Color(
                180 + random.nextInt(75),
                180 + random.nextInt(75),
                180 + random.nextInt(75)
        );
    }

    public record CaptchaResult(String captchaId, String imageBase64) {
    }
}
