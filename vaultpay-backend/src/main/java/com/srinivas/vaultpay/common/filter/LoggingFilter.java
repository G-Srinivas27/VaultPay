package com.srinivas.vaultpay.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Global request/response logging filter with MDC trace correlation.
 *
 * <p><b>Why @Order(1)?</b>
 * This must be the OUTERMOST filter so it wraps every request end-to-end,
 * including rate-limited and authentication-failed requests. This gives
 * a complete picture of all traffic hitting the server.
 *
 * <p><b>What is MDC (Mapped Diagnostic Context)?</b>
 * MDC is a thread-local key-value store built into SLF4J. Values placed
 * in MDC automatically appear in every log line produced by that thread,
 * without needing to pass them through every method call.
 *
 * <p><b>MDC Keys set by this filter:</b>
 * <ul>
 *   <li>{@code traceId} — unique short UUID per request. Filter all logs
 *       by this ID to see the complete lifecycle of one request.</li>
 *   <li>{@code method} — HTTP method (GET, POST, PUT, DELETE)</li>
 *   <li>{@code path}   — request URI path</li>
 *   <li>{@code ip}     — real client IP (X-Forwarded-For aware)</li>
 * </ul>
 *
 * <p><b>X-Trace-Id response header:</b>
 * The traceId is sent back in the response headers. When a user reports
 * a bug, they can share this ID, and you can grep all logs for it to see
 * the exact request that failed — across all log lines, all services.
 *
 * <p><b>MDC.clear() in finally block:</b>
 * Spring uses a thread pool. Without clearing MDC, a thread that processed
 * Alice's request would still have Alice's traceId when it processes Bob's
 * next request. The finally block guarantees clearing even if an exception
 * is thrown anywhere in the request processing chain.
 *
 * <p><b>Response time logging:</b>
 * We capture {@code System.currentTimeMillis()} before the filter chain
 * and subtract after. This includes total server-side time: filter chain,
 * JWT validation, service logic, DB queries, and response serialization.
 */
@Slf4j
@Component
@Order(1)
public class LoggingFilter extends OncePerRequestFilter {

    // Header name for the trace ID sent back to the client
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    // Paths to skip detailed logging (too noisy, no business value)
    private static final String[] EXCLUDED_PATHS = {
            "/swagger-ui",
            "/v3/api-docs",
            "/h2-console",
            "/favicon.ico"
    };

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip logging for noisy infrastructure paths
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Generate a short unique trace ID for this request
        // 8 chars is enough to be unique within a reasonable time window
        // and short enough to be readable in logs
        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String method  = request.getMethod();
        String ip      = extractClientIp(request);

        // Populate MDC — these values will appear in EVERY log line
        // from this thread until MDC.clear() is called
        MDC.put("traceId", traceId);
        MDC.put("method", method);
        MDC.put("path", path);
        MDC.put("ip", ip);

        // Send trace ID back to the client in response header
        // Useful for: debugging, support tickets, distributed tracing
        response.addHeader(TRACE_ID_HEADER, traceId);

        // Record start time for response duration calculation
        long startTime = System.currentTimeMillis();

        // Log the incoming request BEFORE processing
        log.info("→ {} {} | ip={}", method, path, ip);

        try {
            // Continue down the filter chain
            // All downstream logs (JWT filter, service, repository) will
            // automatically include the traceId from MDC
            filterChain.doFilter(request, response);

        } finally {
            // Log the outgoing response AFTER processing completes
            // This runs even if an exception was thrown
            long duration = System.currentTimeMillis() - startTime;
            int status    = response.getStatus();

            // Log at WARN level for error responses (4xx, 5xx) so they
            // stand out in log monitoring dashboards
            if (status >= 400) {
                log.warn("← {} {} | status={} | {}ms", method, path, status, duration);
            } else {
                log.info("← {} {} | status={} | {}ms", method, path, status, duration);
            }

            // CRITICAL: Clear MDC after EVERY request.
            // Spring uses a thread pool — threads are reused across requests.
            // Without this, thread 1 might carry Alice's traceId into Bob's request.
            MDC.clear();
        }
    }

    /**
     * Extracts the real client IP, accounting for reverse proxies.
     *
     * <p>When Nginx or a load balancer sits in front of the app,
     * the connection IP is the proxy's IP. The real client IP is
     * in the X-Forwarded-For header, which may contain a chain:
     * "client, proxy1, proxy2" — the first value is the real client.
     */
    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()
                && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Checks if this path should be excluded from logging.
     */
    private boolean isExcludedPath(String path) {
        for (String excluded : EXCLUDED_PATHS) {
            if (path.startsWith(excluded)) {
                return true;
            }
        }
        return false;
    }
}
