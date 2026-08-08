package com.srinivas.vaultpay.auth.controller;

import com.srinivas.vaultpay.auth.dto.ForgotPasswordRequest;
import com.srinivas.vaultpay.auth.dto.LoginRequest;
import com.srinivas.vaultpay.auth.dto.LoginResponse;
import com.srinivas.vaultpay.auth.dto.ResetPasswordRequest;
import com.srinivas.vaultpay.auth.service.AuthService;
import com.srinivas.vaultpay.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication endpoints.
 *
 * <p>/api/auth/** endpoints are always PUBLIC — no token required.
 * You can't ask someone to authenticate before they log in!
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Login to get a JWT token — paste it in the Authorize button above")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/auth/login
     * Authenticates a user and returns a JWT token.
     *
     * @param request email and password
     * @return 200 with JWT token on success, 401 on bad credentials
     */
    @PostMapping("/login")
    @Operation(
            summary = "Login and get JWT token",
            description = "Authenticates with email and password. Returns a JWT token valid for 24 hours. Paste this token in the **Authorize** button at the top of the page."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful — token returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Bad credentials — wrong email or password")
    })
    @SecurityRequirements   // No JWT needed to log in
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset link")
    @SecurityRequirements
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.email());
        return ResponseEntity.ok(ApiResponse.success("If an account with that email exists, a password reset link has been sent.", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using a token")
    @SecurityRequirements
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success("Password has been successfully reset.", null));
    }
}
