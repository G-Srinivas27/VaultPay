package com.srinivas.vaultpay.common.util;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility to generate beautifully styled, reusable HTML email templates.
 * Uses a dark fintech aesthetic to match the VaultPay web application.
 */
public class EmailTemplateBuilder {

    private static final String PRIMARY_COLOR = "#6366f1";
    private static final String BACKGROUND_COLOR = "#0f111a";
    private static final String CARD_BACKGROUND = "#1a1d27";
    private static final String TEXT_COLOR = "#e2e8f0";

    private static String getBaseTemplate(String title, String content) {
        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background-color: %s; color: %s; margin: 0; padding: 20px; }
                        .container { max-width: 600px; margin: 0 auto; background-color: %s; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px rgba(0,0,0,0.3); }
                        .header { background-color: %s; padding: 30px 20px; text-align: center; }
                        .header h1 { margin: 0; color: #ffffff; font-size: 28px; letter-spacing: 2px; }
                        .content { padding: 30px; font-size: 16px; line-height: 1.6; }
                        .footer { padding: 20px; text-align: center; font-size: 12px; color: #64748b; background-color: #151821; }
                        .button { display: inline-block; padding: 12px 24px; background-color: %s; color: #ffffff; text-decoration: none; border-radius: 6px; font-weight: bold; margin-top: 20px; }
                        .data-box { background-color: rgba(255,255,255,0.05); padding: 15px; border-radius: 8px; margin: 20px 0; border: 1px solid rgba(255,255,255,0.1); }
                        .data-row { display: flex; justify-content: space-between; margin-bottom: 10px; border-bottom: 1px solid rgba(255,255,255,0.05); padding-bottom: 5px; }
                        .data-row:last-child { margin-bottom: 0; border-bottom: none; padding-bottom: 0; }
                        .label { color: #94a3b8; font-weight: 600; }
                        .value { font-weight: bold; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="header">
                            <h1>VAULTPAY</h1>
                        </div>
                        <div class="content">
                            <h2 style="margin-top: 0;">%s</h2>
                            %s
                        </div>
                        <div class="footer">
                            &copy; 2026 VaultPay. Secure Financial Infrastructure.
                        </div>
                    </div>
                </body>
                </html>
                """.formatted(BACKGROUND_COLOR, TEXT_COLOR, CARD_BACKGROUND, CARD_BACKGROUND, PRIMARY_COLOR, title, content);
    }

    public static String buildWelcomeEmail(String name) {
        String content = """
                <p>Hello %s,</p>
                <p>Welcome to VaultPay! Your account has been successfully created and your digital wallet is ready.</p>
                <p>You can now log in to deposit funds, make transfers, and manage your finances securely.</p>
                <center><a href="http://localhost:5173/login" class="button">Log In to VaultPay</a></center>
                """.formatted(name);
        return getBaseTemplate("Welcome to VaultPay!", content);
    }

    public static String buildTransactionEmail(String name, String type, BigDecimal amount, BigDecimal currentBalance, LocalDateTime date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");
        String formattedDate = date.format(formatter);
        String color = type.equalsIgnoreCase("CREDIT") ? "#34d399" : (type.equalsIgnoreCase("DEBIT") ? "#fca5a5" : "#a5b4fc");

        String content = """
                <p>Hello %s,</p>
                <p>A new transaction has been processed on your VaultPay wallet.</p>
                <div class="data-box">
                    <div class="data-row"><span class="label">Type</span> <span class="value" style="color: %s;">%s</span></div>
                    <div class="data-row"><span class="label">Amount</span> <span class="value">$%s</span></div>
                    <div class="data-row"><span class="label">Date & Time</span> <span class="value">%s</span></div>
                    <div class="data-row"><span class="label">Available Balance</span> <span class="value">$%s</span></div>
                </div>
                """.formatted(name, color, type.toUpperCase(), amount.toString(), formattedDate, currentBalance.toString());
        return getBaseTemplate("Transaction Receipt", content);
    }

    public static String buildStatusChangeEmail(String name, boolean active) {
        String status = active ? "ACTIVATED" : "FROZEN";
        String actionText = active ? "You can now log in and use your wallet." : "Please contact support if you believe this is an error.";
        
        String content = """
                <p>Hello %s,</p>
                <p>Your VaultPay account status has been updated to: <strong>%s</strong>.</p>
                <p>%s</p>
                """.formatted(name, status, actionText);
        return getBaseTemplate("Account Status Update", content);
    }

    public static String buildPasswordResetEmail(String name, String resetLink) {
        String content = """
                <p>Hello %s,</p>
                <p>We received a request to reset the password for your VaultPay account.</p>
                <p>Click the button below to set a new password. This link will expire in 15 minutes.</p>
                <center><a href="%s" class="button">Reset Password</a></center>
                <p style="margin-top: 30px; font-size: 12px; color: #64748b;">If you did not request this, please ignore this email.</p>
                """.formatted(name, resetLink);
        return getBaseTemplate("Password Reset Request", content);
    }
}
