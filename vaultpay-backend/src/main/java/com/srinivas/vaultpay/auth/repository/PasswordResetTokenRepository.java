package com.srinivas.vaultpay.auth.repository;

import com.srinivas.vaultpay.auth.entity.PasswordResetToken;
import com.srinivas.vaultpay.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    void deleteByUser(User user);
}
