package com.srinivas.vaultpay.user.dto;

import com.srinivas.vaultpay.user.entity.Role;
import com.srinivas.vaultpay.user.entity.User;

import java.time.LocalDateTime;

/**
 * DTO for the user data returned in API responses.
 *
 * <p><b>Key security decision: password is deliberately absent.</b>
 * Even though it's a hash, returning it is unnecessary and a security smell.
 * Attackers could use BCrypt hashes for offline cracking attempts.
 * The rule: return ONLY what the client needs, nothing more.
 *
 * <p>Uses a Java Record for immutability — response objects should never
 * be mutated after they are created from an entity.
 *
 * <p>The static {@code from(User user)} factory method is the canonical way
 * to create a UserResponse from a User entity. This keeps mapping logic
 * in one place — if the entity changes, there's exactly one place to update.
 *
 * @param id        the unique user identifier
 * @param firstName user's first name
 * @param lastName  user's last name
 * @param email     user's email (login identifier)
 * @param role      user's system role (USER or ADMIN)
 * @param active    whether the account is currently active
 * @param createdAt when the account was created (ISO-8601 format in JSON)
 */
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Role role,
        boolean active,
        LocalDateTime createdAt
) {

    /**
     * Factory method to build a UserResponse from a User entity.
     *
     * <p>Keeping this mapping logic here (in the DTO) rather than in the service
     * follows the Single Responsibility Principle — the service should orchestrate
     * business logic, not field-by-field mapping.
     *
     * @param user the User entity to map from
     * @return a populated UserResponse
     */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isActive(),
                user.getCreatedAt()
        );
    }
}
