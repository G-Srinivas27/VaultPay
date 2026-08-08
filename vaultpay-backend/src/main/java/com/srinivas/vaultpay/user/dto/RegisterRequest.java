package com.srinivas.vaultpay.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for the user registration request payload.
 *
 * <p>Uses a Java Record — immutable, concise, perfect for request objects
 * that are read-once and never modified after deserialization.
 *
 * <p><b>Why validate here and not in the Entity?</b>
 * Entities live in the persistence layer — they should reflect DB constraints.
 * DTOs live at the API boundary — they reflect what the CLIENT must provide.
 * Keeping validation in DTOs means the service layer receives guaranteed-valid data.
 *
 * <p><b>Validation annotations used:</b>
 * <ul>
 *   <li>{@code @NotBlank} — rejects null, empty string "", and whitespace-only "  "</li>
 *   <li>{@code @Email} — validates RFC-compliant email format</li>
 *   <li>{@code @Size} — enforces length boundaries (prevents DB column overflow)</li>
 * </ul>
 *
 * <p>These annotations only activate when the controller parameter is annotated with {@code @Valid}.
 *
 * @param firstName the user's first name (2–100 characters)
 * @param lastName  the user's last name (2–100 characters)
 * @param email     a valid, unique email address used as login identifier
 * @param password  raw password (8–100 chars); will be BCrypt-hashed before storage
 */
public record RegisterRequest(

        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 100, message = "First name must be between 2 and 100 characters")
        @Schema(description = "User's first name", example = "Srinivas")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 100, message = "Last name must be between 2 and 100 characters")
        @Schema(description = "User's last name", example = "Gooda")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Schema(description = "Unique email address used as login ID", example = "srinivas@vaultpay.com")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Schema(description = "Password (min 8 characters)", example = "admin123")
        String password

) {}
