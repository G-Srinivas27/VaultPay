package com.srinivas.vaultpay.auth.service;

import com.srinivas.vaultpay.user.entity.User;
import com.srinivas.vaultpay.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bridges our VaultPay {@link User} entity with Spring Security's authentication system.
 *
 * <p><b>Why does this class exist?</b>
 * Spring Security's authentication mechanism doesn't know about our {@code User} JPA entity.
 * It works with its own interface: {@link UserDetails}. We implement {@link UserDetailsService}
 * to tell Spring Security: "When you need to look up a user by their login name (email),
 * call THIS method — I'll load them from OUR database and give you a UserDetails object."
 *
 * <p><b>Flow during login:</b>
 * <pre>
 *   POST /api/auth/login { email, password }
 *          ↓
 *   AuthenticationManager calls loadUserByUsername(email)
 *          ↓
 *   We fetch User from DB by email
 *          ↓
 *   We wrap it in Spring's UserDetails
 *          ↓
 *   Spring compares the provided password against UserDetails.getPassword() (BCrypt hash)
 *          ↓
 *   If match → Authentication succeeds → JwtService generates token
 * </pre>
 *
 * <p><b>Authorities (Roles):</b>
 * Spring Security expects roles prefixed with "ROLE_".
 * Our {@code Role.USER} becomes {@code "ROLE_USER"}.
 * This prefix is required for {@code hasRole("USER")} checks to work.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads a user by their email address.
     *
     * <p>Spring Security calls this during:
     * 1. Login — to get the stored password hash for comparison
     * 2. Every authenticated request — to rebuild the SecurityContext from the JWT email
     *
     * @param email the user's email (used as the "username" in our system)
     * @return UserDetails containing email, hashed password, and authorities
     * @throws UsernameNotFoundException if no user exists with this email
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())         // The BCrypt hash — Spring compares against it
                .authorities(List.of(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                ))                                    // e.g., "ROLE_USER" or "ROLE_ADMIN"
                .accountExpired(!user.isActive())     // Maps our 'active' flag to Spring's concept
                .accountLocked(!user.isActive())
                .build();
    }
}
