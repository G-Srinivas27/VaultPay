package com.srinivas.vaultpay.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Provides the "current actor" for Spring Data JPA Auditing.
 *
 * <p><b>What is AuditorAware?</b>
 * Spring Data JPA's auditing system doesn't know HOW to find the current user —
 * it only knows it needs one to populate @CreatedBy and @LastModifiedBy.
 * {@link AuditorAware} is the bridge: we implement it to tell Spring Data
 * "here's how to find the current user in THIS application."
 *
 * <p><b>How the wiring works:</b>
 * <pre>
 *   User saves a Wallet
 *        ↓
 *   AuditingEntityListener (from @EntityListeners) fires
 *        ↓
 *   Spring calls AuditorAwareImpl.getCurrentAuditor()
 *        ↓
 *   We read SecurityContextHolder → get authenticated email
 *        ↓
 *   Spring sets @CreatedBy / @LastModifiedBy = "srinivas@vaultpay.com"
 * </pre>
 *
 * <p><b>Why "SYSTEM" as fallback?</b>
 * Some operations happen without an authenticated user — for example,
 * POST /api/users/register is a public endpoint (no JWT required).
 * We still want to track WHO created the record; "SYSTEM" makes it clear
 * this was an unauthenticated/automated operation, not a human user action.
 *
 * <p><b>The bean name "auditorAwareImpl" matters!</b>
 * We reference it in @EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
 * in {@link JpaConfig}. The name must match exactly.
 */
@Component("auditorAwareImpl")
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // No authentication in context (e.g., unauthenticated endpoint like /register)
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.of("SYSTEM");
        }

        // AnonymousAuthenticationToken means Spring Security sees no JWT
        // — treat as a system/background operation
        if (authentication instanceof AnonymousAuthenticationToken) {
            return Optional.of("SYSTEM");
        }

        // For JWT-authenticated requests: getName() returns the email (the JWT subject)
        // This is the email we set in JwtService.generateToken() as the token subject
        return Optional.of(authentication.getName());
    }
}
