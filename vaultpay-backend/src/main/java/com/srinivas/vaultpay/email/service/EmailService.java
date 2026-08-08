package com.srinivas.vaultpay.email.service;

import com.srinivas.vaultpay.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface EmailService {
    void sendRegistrationEmail(User user);
    void sendTransactionEmail(User user, String type, BigDecimal amount, BigDecimal currentBalance, LocalDateTime date);
    void sendStatusChangeEmail(User user, boolean active);
    void sendPasswordResetEmail(User user, String resetToken);
}
