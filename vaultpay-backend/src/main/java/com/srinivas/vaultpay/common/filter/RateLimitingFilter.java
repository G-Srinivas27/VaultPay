package com.srinivas.vaultpay.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.srinivas.vaultpay.common.response.ApiResponse;
import com.srinivas.vaultpay.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter — the first line of defence against API abuse.
 *
 * <p><b>Why OncePerRequestFilter?</b>
 * Spring's OncePerRequestFilter guarantees this runs EXACTLY ONCE per HTTP request,
 * even if the servlet container dispatches the request multiple times internally
 * (e.g., for error handling). Without this, filters can run twice and consume
 * 2 tokens per request — breaking the rate limit logic.
 *
 * <p><b>@Order(1)</b> — This filter runs FIRST, before JwtAuthenticationFilter.
 * Rate limiting must happen before any business logic, including JWT validation.
 * Otherwise an attacker could flood the server with bad JWTs and still hit
 * the expensive JWT parsing logic on every request.
 *
 * <p><b>ConcurrentHashMap for thread safety:</b>
 * Multiple threads handle requests simultaneously. ConcurrentHashMap ensures
 * bucket lookups and creation are thread-safe without blocking performance.
 * computeIfAbsent() is atomic — no two threads can create a bucket for the same key.
 *
 * <p><b>Bucket key = endpoint_type:clientIP</b>
 * Separating by endpoint type + IP means:
 * - Login buckets don't share tokens with general API buckets
 * - One IP can't exhaust the login bucket and then use general bucket for auth
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    // Thread-safe map: "bucketType:ipAddress" → Bucket
    // Each IP gets its own independent bucket per endpoint group
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    // Paths that bypass rate limiting entirely
    // Swagger makes parallel requests for CSS/JS assets — limiting these breaks the UI
    private static final String[] EXCLUDED_PATHS = {
            "/swagger-ui",
            "/v3/api-docs",
            "/h2-console"
    };

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Skip rate limiting for excluded paths
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = extractClientIp(request);
        String bucketType = determineBucketType(method, path);
        String bucketKey = bucketType + ":" + clientIp;

        // computeIfAbsent is atomic — creates a new bucket only if key doesn't exist
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(bucketType));

        // tryConsumeAndReturnRemaining:
        // - Tries to consume 1 token
        // - Returns a probe with: isConsumed, remainingTokens, nanosToWaitForRefill
        // - Does NOT block — immediately returns success or failure
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Token consumed → request is allowed
            // Add remaining tokens header so clients can see their quota
            response.addHeader("X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens()));

            log.debug("Rate limit OK — IP: {}, path: {}, remaining: {}",
                    clientIp, path, probe.getRemainingTokens());

            filterChain.doFilter(request, response);

        } else {
            // No tokens left → rate limit exceeded
            // Convert nanoseconds to seconds for the Retry-After header
            long retryAfterSeconds = probe.getNanosToWaitForRefill() / 1_000_000_000;

            log.warn("Rate limit EXCEEDED — IP: {}, path: {}, retry after: {}s",
                    clientIp, path, retryAfterSeconds);

            sendRateLimitResponse(response, retryAfterSeconds, bucketType);
        }
    }

    /**
     * Determines which rate limit bucket type applies to this request.
     *
     * <p>The order matters — check more specific paths first,
     * fall through to general for everything else.
     */
    private String determineBucketType(String method, String path) {
        // Login — strictest
        if ("POST".equals(method) && path.equals("/api/auth/login")) {
            return "login";
        }

        // Registration — strict
        if ("POST".equals(method) && path.equals("/api/users/register")) {
            return "register";
        }

        // Financial operations — strict
        if ("POST".equals(method) && path.startsWith("/api/transactions/") &&
                (path.endsWith("/deposit") || path.endsWith("/withdraw") || path.endsWith("/transfer"))) {
            return "financial";
        }

        // Everything else — general (60/min)
        return "general";
    }

    /**
     * Creates a new Token Bucket with the correct bandwidth for the given type.
     */
    private Bucket createBucket(String bucketType) {
        return switch (bucketType) {
            case "login"     -> Bucket.builder().addLimit(RateLimitConfig.loginBandwidth()).build();
            case "register"  -> Bucket.builder().addLimit(RateLimitConfig.registerBandwidth()).build();
            case "financial" -> Bucket.builder().addLimit(RateLimitConfig.financialBandwidth()).build();
            default          -> Bucket.builder().addLimit(RateLimitConfig.generalBandwidth()).build();
        };
    }

    /**
     * Extracts the real client IP address.
     *
     * <p>When a load balancer or reverse proxy (Nginx, AWS ALB) sits in front
     * of the app, the direct connection IP is the proxy's IP — not the client's.
     * The real client IP is forwarded in the X-Forwarded-For header.
     *
     * <p>We check X-Forwarded-For first, fall back to getRemoteAddr().
     * We also split on comma because X-Forwarded-For can contain a chain:
     * "client-ip, proxy1-ip, proxy2-ip" — we want the first (original client).
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");

        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // Take the first IP in the chain — that's the original client
            return xForwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

    /**
     * Checks if the request path should bypass rate limiting.
     */
    private boolean isExcludedPath(String path) {
        for (String excluded : EXCLUDED_PATHS) {
            if (path.startsWith(excluded)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes a 429 Too Many Requests response with JSON body.
     *
     * <p>We write JSON manually here because we're in a filter —
     * Spring MVC's @ExceptionHandler doesn't run at the filter level.
     * GlobalExceptionHandler only catches exceptions from controllers.
     */
    private void sendRateLimitResponse(HttpServletResponse response,
                                       long retryAfterSeconds,
                                       String bucketType) throws IOException {

        String message = switch (bucketType) {
            case "login"     -> "Too many login attempts. Max 5 per minute. Please wait before trying again.";
            case "register"  -> "Too many registration attempts. Max 3 per minute.";
            case "financial" -> "Too many financial requests. Max 10 per minute. Please slow down.";
            default          -> "Too many requests. Max 60 per minute. Please slow down.";
        };

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());  // 429
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.addHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.addHeader("X-Rate-Limit-Remaining", "0");

        // Use our existing ApiResponse wrapper for consistent response format
        ApiResponse<Void> apiResponse = ApiResponse.error(message, "RATE_LIMIT_EXCEEDED");
        objectMapper.writeValue(response.getWriter(), apiResponse);
    }
}
