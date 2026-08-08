package com.srinivas.vaultpay.config;

import io.github.bucket4j.Bandwidth;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Defines rate limit rules for different endpoint groups.
 *
 * <p><b>Token Bucket Algorithm — how it works:</b>
 * <pre>
 *   Bucket capacity = max requests allowed in the window
 *   Refill rate     = tokens added back per time unit
 *
 *   Example: capacity=5, refill 5 tokens every 1 minute
 *   → Max 5 requests/minute, then 429 until tokens refill
 * </pre>
 *
 * <p><b>Why different limits per endpoint group?</b>
 * <ul>
 *   <li>Login → very strict (5/min) — brute force prevention</li>
 *   <li>Register → strict (3/min) — fake account prevention</li>
 *   <li>Financial ops → strict (10/min) — abuse prevention</li>
 *   <li>General API → relaxed (60/min) — normal usage</li>
 * </ul>
 *
 * <p><b>Why NOT Swagger endpoints?</b>
 * Swagger UI makes many parallel requests to load its UI assets.
 * Rate limiting these would break the Swagger page itself.
 */
@Configuration
public class RateLimitConfig {

    /**
     * Login endpoint — strictest limit.
     * 5 attempts per minute per IP.
     *
     * <p>Rationale: A real user never needs to try logging in
     * more than 5 times per minute. More than that is almost
     * certainly a brute force attack or credential stuffing.
     */
    public static Bandwidth loginBandwidth() {
        return Bandwidth.builder()
                .capacity(5)
                .refillIntervally(5, Duration.ofMinutes(1))
                .build();
    }

    /**
     * Registration endpoint.
     * 3 registrations per minute per IP.
     *
     * <p>Rationale: No legitimate user creates more than
     * a few accounts. Bots create thousands per second.
     */
    public static Bandwidth registerBandwidth() {
        return Bandwidth.builder()
                .capacity(3)
                .refillIntervally(3, Duration.ofMinutes(1))
                .build();
    }

    /**
     * Financial transaction endpoints (deposit, withdraw, transfer).
     * 10 requests per minute per IP.
     *
     * <p>Rationale: Financial operations are expensive on the DB
     * (locking, ACID guarantees). More than 10/min from one IP
     * suggests automated abuse, not a real user.
     *
     * <p>Production improvement: rate limit per user ID (from JWT)
     * instead of per IP — more accurate, harder to bypass with proxies.
     */
    public static Bandwidth financialBandwidth() {
        return Bandwidth.builder()
                .capacity(10)
                .refillIntervally(10, Duration.ofMinutes(1))
                .build();
    }

    /**
     * General API endpoints — relaxed limit.
     * 60 requests per minute per IP (1 per second average).
     *
     * <p>Rationale: Normal API usage. A frontend app fetching
     * data every few seconds will easily stay within this.
     */
    public static Bandwidth generalBandwidth() {
        return Bandwidth.builder()
                .capacity(60)
                .refillIntervally(60, Duration.ofMinutes(1))
                .build();
    }
}
