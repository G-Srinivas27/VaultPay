package com.srinivas.vaultpay.auth.dto;

/**
 * Response body returned after a successful login.
 *
 * <p>The client stores this token (in localStorage, a cookie, or memory)
 * and sends it with every subsequent request in the Authorization header:
 * {@code Authorization: Bearer <token>}
 *
 * @param token     the JWT access token
 * @param type      always "Bearer" — the standard HTTP authentication scheme for JWT
 * @param email     the logged-in user's email (for client-side display)
 * @param role      the user's role (client can use this for UI decisions)
 * @param expiresIn token validity in milliseconds (helps client schedule refresh)
 */
public record LoginResponse(
        String token,
        String type,
        String email,
        String role,
        long expiresIn
) {
    // Convenience constructor — type is always "Bearer" for JWT
    public LoginResponse(String token, String email, String role, long expiresIn) {
        this(token, "Bearer", email, role, expiresIn);
    }
}
