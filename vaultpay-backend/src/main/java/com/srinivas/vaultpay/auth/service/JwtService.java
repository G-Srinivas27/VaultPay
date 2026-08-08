package com.srinivas.vaultpay.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for all JWT operations: generation, parsing, and validation.
 *
 * <p><b>How JWT works (the full picture):</b>
 * <pre>
 *   LOGIN:
 *   1. User sends email + password
 *   2. We verify password against BCrypt hash in DB
 *   3. We create a JWT: Header.Payload.Signature
 *      - Header:  { "alg": "HS256" }
 *      - Payload: { "sub": "email", "iat": now, "exp": now+24h }
 *      - Signature: HMAC_SHA256(base64(header) + "." + base64(payload), SECRET_KEY)
 *   4. Token is returned to client
 *
 *   EVERY SUBSEQUENT REQUEST:
 *   1. Client sends: Authorization: Bearer <token>
 *   2. We extract the token, re-compute the signature
 *   3. If signatures match → token is valid → we trust the payload
 *   4. No database lookup needed — stateless! ✅
 * </pre>
 *
 * <p><b>@Value("${jwt.secret}"):</b>
 * Reads the value from application-dev.yaml's jwt.secret property.
 * Spring injects it at startup. This keeps secrets out of source code.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    // ─── Token Generation ──────────────────────────────────────────────────

    /**
     * Generates a JWT token for the given user.
     *
     * <p>The token's "subject" (sub claim) is the user's email —
     * this is how we identify who the token belongs to.
     *
     * @param userDetails the authenticated user
     * @return signed JWT token string
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a JWT with additional custom claims.
     *
     * <p><b>Claims</b> are the payload data stored in the token.
     * Standard claims: sub (subject/email), iat (issued at), exp (expiry).
     * Custom claims: anything else — we could add "role", "userId", etc.
     *
     * @param extraClaims additional key-value pairs to embed in the token
     * @param userDetails the authenticated user
     * @return signed JWT token string
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .claims(extraClaims)                       // custom claims (role, etc.)
                .subject(userDetails.getUsername())        // "sub" = email (the identity)
                .issuedAt(new Date(now))                   // "iat" = when token was created
                .expiration(new Date(now + expirationMs))  // "exp" = when token expires
                .signWith(getSigningKey())                 // Signs with HMAC-SHA256
                .compact();                                // Builds the final token string
    }

    // ─── Token Validation ──────────────────────────────────────────────────

    /**
     * Validates a token against the given user.
     *
     * <p>Two checks:
     * 1. Does the email in the token match the user we looked up from DB?
     * 2. Has the token expired?
     *
     * <p>The signature is verified internally by JJWT when we parse it —
     * if the signature is invalid, parsing throws an exception.
     *
     * @param token       the JWT string from the Authorization header
     * @param userDetails the user loaded from database
     * @return true if token is valid for this user and not expired
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        boolean valid = email.equals(userDetails.getUsername()) && !isTokenExpired(token);
        if (!valid) {
            log.warn("Token validation failed for email: {}", email);
        }
        return valid;
    }

    // ─── Claim Extraction ──────────────────────────────────────────────────

    /**
     * Extracts the email (subject) from the token.
     * This is the user's identity — used to load them from the DB.
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Returns the token's expiration duration in milliseconds.
     * Exposed so LoginResponse can tell the client how long the token is valid.
     */
    public long getExpirationMs() {
        return expirationMs;
    }

    /**
     * Generic claim extractor using a Function — avoids writing a separate
     * method for every possible claim type.
     *
     * <p>Example: {@code extractClaim(token, Claims::getSubject)}
     * is equivalent to: parse the token, get all claims, call .getSubject()
     *
     * @param token          the JWT string
     * @param claimsResolver a function to extract a specific claim
     * @param <T>            the return type of the claim
     * @return the extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ─── Private helpers ───────────────────────────────────────────────────

    private boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Parses the token and extracts ALL claims.
     *
     * <p>This is where signature verification happens!
     * JJWT re-computes: HMAC(header.payload, SECRET_KEY)
     * and compares it to the signature in the token.
     * If they don't match → {@code JwtException} is thrown → token is rejected.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())   // Sets the key to verify the signature against
                .build()
                .parseSignedClaims(token)      // Parses AND verifies — throws if invalid
                .getPayload();
    }

    /**
     * Converts the secret string from YAML into a cryptographic key object.
     *
     * <p>HMAC-SHA256 requires at least 256 bits (32 bytes).
     * Keys.hmacShaKeyFor() handles this conversion safely.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
