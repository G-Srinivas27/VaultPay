package com.srinivas.vaultpay.user.controller;

import com.srinivas.vaultpay.common.response.ApiResponse;
import com.srinivas.vaultpay.common.response.PagedResponse;
import com.srinivas.vaultpay.user.dto.RegisterRequest;
import com.srinivas.vaultpay.user.dto.UpdateRoleRequest;
import com.srinivas.vaultpay.user.dto.UserResponse;
import com.srinivas.vaultpay.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller handling all user-related HTTP endpoints.
 *
 * <p><b>@Tag</b> — groups all endpoints in this controller under one section
 * in the Swagger UI. Without this, endpoints appear in the default "user-controller" group.
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Endpoints for user registration, profile, and admin management")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * POST /api/users/register — public, no JWT needed.
     *
     * <p>@SecurityRequirements({}) — overrides the global JWT requirement for this endpoint.
     * Since the global config (OpenApiConfig) applies bearerAuth to ALL endpoints,
     * we explicitly remove it here so the lock icon doesn't appear on the register endpoint.
     */
    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account. No authentication required. Default role is USER."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error — check request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already in use")
    })
    @SecurityRequirements   // No JWT needed for registration
    public ResponseEntity<ApiResponse<UserResponse>> registerUser(
            @Valid @RequestBody RegisterRequest request) {

        UserResponse response = userService.registerUser(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response));
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get own profile",
            description = "Returns the currently authenticated user's profile. Reads identity from the JWT — no ID needed."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {
        UserResponse response = userService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", response));
    }

    @PutMapping("/me")
    @Operation(
            summary = "Update own profile",
            description = "Updates the authenticated user's profile (name and phone). Cannot update role or email."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody com.srinivas.vaultpay.user.dto.UpdateProfileRequest request) {
        
        UserResponse response = userService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get user by ID",
            description = "Returns any user's profile by their ID. **ADMIN only** — regular users get 403."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "User found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "The user's ID", example = "1")
            @PathVariable Long id) {

        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", response));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get all users (paginated)",
            description = "Returns a paginated list of all users. **ADMIN only**. Supports ?page, ?size, ?sort query params."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Users retrieved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required")
    })
    public ResponseEntity<ApiResponse<PagedResponse<UserResponse>>> getAllUsers(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Page<UserResponse> page = userService.getAllUsers(pageable);
        return ResponseEntity.ok(
                ApiResponse.success("Users retrieved successfully", PagedResponse.from(page))
        );
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Update user role",
            description = "Promotes or demotes a user's role. **ADMIN only**. Body: `{ \"role\": \"ADMIN\" }` or `{ \"role\": \"USER\" }`"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Role updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @Parameter(description = "The user's ID", example = "2")
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {

        UserResponse response = userService.updateUserRole(id, request.role());
        return ResponseEntity.ok(ApiResponse.success(
                "User role updated to " + request.role(), response));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Activate or deactivate user",
            description = "Activates or deactivates a user account. **ADMIN only**. Use `?active=false` to deactivate."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Status updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied — ADMIN role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @Parameter(description = "The user's ID", example = "2")
            @PathVariable Long id,
            @Parameter(description = "true = activate, false = deactivate", example = "false")
            @RequestParam boolean active) {

        UserResponse response = userService.updateUserStatus(id, active);
        String message = active ? "User account activated" : "User account deactivated";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }
}
