package com.asad.expensetracker.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${app.mail.from}")
    private String from;

    /**
     * Sends the password-reset link. If no SMTP host is configured (the local-dev default),
     * this logs the link instead of failing, so the flow is still testable with zero setup.
     */
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        if (smtpHost == null || smtpHost.isBlank()) {
            log.info("SMTP is not configured — password reset link for {}: {}", toEmail, resetLink);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(toEmail);
            message.setSubject("Reset your SpendWise password");
            message.setText("""
                    Someone (hopefully you) asked to reset the password on your SpendWise account.

                    Reset it here: %s

                    This link expires in 30 minutes. If you didn't request this, you can ignore this email.
                    """.formatted(resetLink));
            mailSender.send(message);
        } catch (Exception ex) {
            // Never let a mail-provider hiccup surface as a 500 to the end user — log it instead.
            log.error("Failed to send password reset email to {}", toEmail, ex);
        }
    }
}
