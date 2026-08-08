package com.srinivas.vaultpay.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for the login endpoint.
 *
 * @param email    the user's registered email
 * @param password the user's raw password (never stored — compared against BCrypt hash)
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Schema(description = "Registered email address", example = "srinivas@vaultpay.com")
        String email,

        @NotBlank(message = "Password is required")
        @Schema(description = "Account password", example = "admin123")
        String password
) {}
