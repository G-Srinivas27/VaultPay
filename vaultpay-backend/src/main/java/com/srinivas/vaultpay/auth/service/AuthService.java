package com.srinivas.vaultpay.auth.service;

import com.srinivas.vaultpay.auth.dto.LoginRequest;
import com.srinivas.vaultpay.auth.dto.LoginResponse;
import com.srinivas.vaultpay.auth.entity.PasswordResetToken;
import com.srinivas.vaultpay.auth.repository.PasswordResetTokenRepository;
import com.srinivas.vaultpay.common.exception.BusinessException;
import com.srinivas.vaultpay.email.service.EmailService;
import com.srinivas.vaultpay.user.entity.User;
import com.srinivas.vaultpay.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * Service handling authentication business logic.
 *
 * <p><b>AuthenticationManager:</b>
 * Spring Security's central authentication coordinator. When we call
 * {@code authenticationManager.authenticate(token)}, Spring Security:
 * <ol>
 *   <li>Calls {@code CustomUserDetailsService.loadUserByUsername(email)}</li>
 *   <li>Gets the stored BCrypt hash from UserDetails</li>
 *   <li>Uses {@code PasswordEncoder.matches(rawPassword, hash)} to verify</li>
 *   <li>If match → returns authenticated token; if not → throws BadCredentialsException</li>
 * </ol>
 * This single line replaces what would be 15 lines of manual password-checking code.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager,
                       CustomUserDetailsService userDetailsService,
                       JwtService jwtService,
                       UserRepository userRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailService emailService,
                       PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * <p>Flow:
     * 1. AuthenticationManager verifies email + password (throws if wrong)
     * 2. Load UserDetails (for JwtService)
     * 3. Generate JWT token
     * 4. Return LoginResponse with token + user info
     *
     * <p>If credentials are wrong, {@code AuthenticationManager} throws
     * {@code BadCredentialsException} — caught by GlobalExceptionHandler → 401.
     */
    public LoginResponse login(LoginRequest request) {
        log.debug("Login attempt for email: {}", request.email());

        // Step 1: Delegate credential verification to Spring Security
        // This throws BadCredentialsException if email/password is wrong
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        // Step 2: If we reach here, credentials are valid — load the user
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());

        // Step 3: Fetch role for the response (UserDetails gives us authority strings)
        User user = userRepository.findByEmail(request.email()).orElseThrow();
        String role = user.getRole().name();

        // Step 4: Generate JWT
        String token = jwtService.generateToken(userDetails);

        log.info("User logged in successfully: {}", request.email());

        return new LoginResponse(token, request.email(), role, jwtService.getExpirationMs());
    }

    /**
     * Initiates the password reset workflow.
     * Generates a token and sends an email via EmailService asynchronously.
     */
    @Transactional
    public void requestPasswordReset(String email) {
        log.info("Password reset requested for email: {}", email);
        
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            // For security, don't reveal if the user exists
            log.warn("Password reset requested for non-existent email: {}", email);
            return;
        }

        User user = optionalUser.get();

        // Delete any existing tokens for this user to ensure only 1 valid token exists
        passwordResetTokenRepository.deleteByUser(user);

        // Generate a new secure UUID token
        String token = UUID.randomUUID().toString() + UUID.randomUUID().toString();
        token = token.replace("-", ""); // 64-char hex string

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();

        passwordResetTokenRepository.save(resetToken);

        // Asynchronously dispatch the email
        emailService.sendPasswordResetEmail(user, token);
    }

    /**
     * Validates the token and updates the user's password.
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Invalid or expired password reset token."));

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BusinessException("Password reset token has expired.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete token to prevent reuse
        passwordResetTokenRepository.delete(resetToken);

        log.info("Password successfully reset for user: {}", user.getEmail());
    }
}
