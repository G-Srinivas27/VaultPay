package com.srinivas.vaultpay.email.service;

import com.srinivas.vaultpay.common.util.EmailTemplateBuilder;
import com.srinivas.vaultpay.user.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@vaultpay.com}")
    private String fromEmail;

    public EmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    protected void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            log.info("Attempting to send email to {} with subject: {}", to, subject);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true); // true indicates html

            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}. Reason: {}", to, e.getMessage());
            // Intentionally swallowed so it doesn't fail the caller's transaction
        } catch (Exception e) {
            log.error("Unexpected error while sending email to {}. Reason: {}", to, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendRegistrationEmail(User user) {
        String htmlBody = EmailTemplateBuilder.buildWelcomeEmail(user.getFirstName());
        sendHtmlEmail(user.getEmail(), "Welcome to VaultPay!", htmlBody);
    }

    @Override
    @Async
    public void sendTransactionEmail(User user, String type, BigDecimal amount, BigDecimal currentBalance, LocalDateTime date) {
        String htmlBody = EmailTemplateBuilder.buildTransactionEmail(user.getFirstName(), type, amount, currentBalance, date);
        sendHtmlEmail(user.getEmail(), "VaultPay Transaction Receipt", htmlBody);
    }

    @Override
    @Async
    public void sendStatusChangeEmail(User user, boolean active) {
        String htmlBody = EmailTemplateBuilder.buildStatusChangeEmail(user.getFirstName(), active);
        sendHtmlEmail(user.getEmail(), "VaultPay Account Status Update", htmlBody);
    }

    @Override
    @Async
    public void sendPasswordResetEmail(User user, String resetToken) {
        // Link will be constructed in the frontend URL, assuming localhost:5173 for now
        String resetLink = "http://localhost:5173/reset-password?token=" + resetToken;
        String htmlBody = EmailTemplateBuilder.buildPasswordResetEmail(user.getFirstName(), resetLink);
        sendHtmlEmail(user.getEmail(), "VaultPay Password Reset Request", htmlBody);
    }
}
