package com.foliopath360.lms.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log =
            LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled}")
    private boolean mailEnabled;

    @Value("${app.mail.from}")
    private String from;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendStaffSetupEmail(String to, String firstName, String setupLink) {

        String subject = "Welcome to Foliopath360 - Set your password";

        String html = """
                <html>
                <body>
                    <h2>Welcome, %s!</h2>
                    <p>Your staff account has been created by the administrator.</p>
                    <p>Click the link below to set your password and activate your account:</p>
                    <p><a href="%s" style="background-color:#4CAF50;color:white;padding:12px 24px;text-decoration:none;border-radius:4px;">Set My Password</a></p>
                    <p>Or copy this link into your browser:</p>
                    <p>%s</p>
                    <p><b>This link is valid for 24 hours and can be used only once.</b></p>
                    <p>If you did not expect this email, please ignore it.</p>
                </body>
                </html>
                """.formatted(firstName, setupLink, setupLink);

        send(to, subject, html);
    }

    public void sendOtpEmail(String to, String firstName, String otp) {

        String subject = "Foliopath360 - Verify your email";

        String html = """
                <html>
                <body>
                    <h2>Hello %s,</h2>
                    <p>Thank you for registering with Foliopath360.</p>
                    <p>Your verification code is:</p>
                    <h1 style="letter-spacing:8px;color:#2196F3;">%s</h1>
                    <p><b>This code is valid for 10 minutes.</b></p>
                    <p>If you did not register, please ignore this email.</p>
                </body>
                </html>
                """.formatted(firstName, otp);

        send(to, subject, html);
    }

    public void sendPasswordResetOtpEmail(String to, String firstName, String otp) {

        String subject = "Foliopath360 - Reset your password";

        String html = """
                <html>
                <body>
                    <h2>Hello %s,</h2>
                    <p>We received a request to reset your password.</p>
                    <p>Your password reset code is:</p>
                    <h1 style="letter-spacing:8px;color:#FF5722;">%s</h1>
                    <p><b>This code is valid for 10 minutes.</b></p>
                    <p>If you did not request a password reset, please ignore this email
                       and your password will remain unchanged.</p>
                </body>
                </html>
                """.formatted(firstName, otp);

        send(to, subject, html);
    }

    private void send(String to, String subject, String htmlBody) {

        if (!mailEnabled) {
            log.info("[MAIL DISABLED] Would send email to={} subject={}", to, subject);
            log.info("[MAIL DISABLED] Body:\n{}", htmlBody);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);

            mailSender.send(message);

            log.info("Email sent to={} subject={}", to, subject);

        } catch (Exception e) {
            log.error("Failed to send email to={}", to, e);
            throw new RuntimeException(
                    "Failed to send email. Please try again later.", e
            );
        }
    }
}
