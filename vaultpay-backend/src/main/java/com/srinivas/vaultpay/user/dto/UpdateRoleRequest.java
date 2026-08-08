package com.srinivas.vaultpay.user.dto;

import com.srinivas.vaultpay.user.entity.Role;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating a user's role.
 *
 * <p>Used by ADMIN-only endpoint: PATCH /api/users/{id}/role
 * Allows an admin to promote a USER to ADMIN or demote an ADMIN back to USER.
 *
 * @param role the new role to assign — must not be null
 */
public record UpdateRoleRequest(
        @NotNull(message = "Role is required")
        Role role
) {}
