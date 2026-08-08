package com.srinivas.vaultpay.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for updating the user profile.
 * Only allowed fields are included here (e.g. no role, email, or password).
 */
public record UpdateProfileRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name cannot exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name cannot exceed 100 characters")
        String lastName,

        @Size(max = 20, message = "Phone number cannot exceed 20 characters")
        String phoneNumber
) {
}
