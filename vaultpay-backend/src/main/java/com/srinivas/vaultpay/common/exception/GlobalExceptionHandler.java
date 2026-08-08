package com.srinivas.vaultpay.common.exception;

import com.srinivas.vaultpay.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Centralized exception handling for the entire VaultPay application.
 *
 * <p>Why @RestControllerAdvice?
 * Without this, every controller would need its own try-catch blocks, and the
 * error response format would be inconsistent. This class intercepts exceptions
 * thrown anywhere in the application and converts them to a uniform ApiResponse.
 *
 * <p>Why NOT @ControllerAdvice?
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody.
 * Since all our endpoints return JSON, @RestControllerAdvice saves us from
 * annotating every @ExceptionHandler method with @ResponseBody.
 *
 * <p>Handler priority: Spring picks the MOST SPECIFIC handler that matches the
 * exception type. Order here is: specific VaultPay exceptions → validation →
 * Spring framework exceptions → catch-all.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles all our custom application exceptions in one place.
     * Because VaultPayException carries its own HttpStatus, we don't need
     * a separate handler for each subtype — polymorphism does the work.
     */
    @ExceptionHandler(VaultPayException.class)
    public ResponseEntity<ApiResponse<Void>> handleVaultPayException(VaultPayException ex) {
        log.warn("Application exception: {} — {}", ex.getClass().getSimpleName(), ex.getMessage());
        return ResponseEntity
                .status(ex.getStatus())
                .body(ApiResponse.error(ex.getMessage(), ex.getClass().getSimpleName()));
    }

    /**
     * Handles invalid login credentials.
     * BadCredentialsException is thrown by AuthenticationManager when
     * email/password combination is wrong. Returns 401 Unauthorized.
     *
     * <p>IMPORTANT: We return a generic message — never tell the client
     * whether the email exists or the password is wrong. That would help
     * attackers enumerate valid accounts (user enumeration attack).
     */
    @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(
            org.springframework.security.authentication.BadCredentialsException ex) {
        log.warn("Failed login attempt: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Invalid email or password", "UNAUTHORIZED"));
    }

    /**
     * Handles Bean Validation failures (@Valid annotation on controller params).
     *
     * <p>Collects ALL field errors and joins them, so the client gets the full
     * picture in one response instead of fixing one error at a time.
     *
     * <p>Example output:
     * "email: must not be blank; password: size must be between 8 and 100"
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Validation failed: {}", errors);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", errors));
    }

    /**
     * Catch-all handler for any unexpected exception.
     *
     * <p>IMPORTANT: We log at ERROR level here (vs WARN above) because unexpected
     * exceptions indicate bugs, not normal user errors. We also deliberately hide
     * the internal message from the client — never leak stack traces or internal
     * details to the caller. That's a security risk.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later.", "INTERNAL_SERVER_ERROR"));
    }
}
