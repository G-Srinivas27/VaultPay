package com.srinivas.vaultpay.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * A generic, immutable API response envelope used across all endpoints.
 *
 * <p>Why a generic wrapper?
 * Consumers (frontend, mobile apps, other services) expect a CONSISTENT response shape
 * regardless of the endpoint. This prevents every controller from inventing its own format.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>Uses a Java Record (Java 16+) — immutable by design, no boilerplate</li>
 *   <li>{@code @JsonInclude(NON_NULL)} — omits null fields from JSON output.
 *       On success, 'error' is null. On failure, 'data' is null. No clutter.</li>
 *   <li>Generic {@code <T>} — works for any data type (UserResponse, WalletResponse, List, etc.)</li>
 * </ul>
 *
 * <p>Example success response:
 * <pre>
 * {
 *   "success": true,
 *   "message": "User registered successfully",
 *   "data": { "id": 1, "email": "john@example.com" }
 * }
 * </pre>
 *
 * <p>Example error response:
 * <pre>
 * {
 *   "success": false,
 *   "message": "Validation failed",
 *   "error": "Email is required"
 * }
 * </pre>
 *
 * @param success true if the request was processed successfully
 * @param message a human-readable status message
 * @param data    the response payload (null on error)
 * @param error   error detail string (null on success)
 * @param <T>     the type of the data payload
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        String error
) {

    /**
     * Factory method for successful responses with data.
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    /**
     * Factory method for successful responses without data (e.g., DELETE operations).
     */
    public static <T> ApiResponse<T> success(String message) {
        return new ApiResponse<>(true, message, null, null);
    }

    /**
     * Factory method for error responses.
     */
    public static <T> ApiResponse<T> error(String message, String error) {
        return new ApiResponse<>(false, message, null, error);
    }
}
