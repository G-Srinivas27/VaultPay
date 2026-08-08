package com.srinivas.vaultpay.auth.filter;

import com.srinivas.vaultpay.auth.service.CustomUserDetailsService;
import com.srinivas.vaultpay.auth.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter — intercepts every HTTP request and validates the JWT token.
 *
 * <p><b>Why extend OncePerRequestFilter?</b>
 * Spring's filter chain can sometimes invoke a filter multiple times per request
 * (e.g., during async dispatch). {@link OncePerRequestFilter} guarantees
 * {@code doFilterInternal} is called EXACTLY ONCE per request — predictable and safe.
 *
 * <p><b>The filter flow for every request:</b>
 * <pre>
 *   HTTP Request arrives
 *        ↓
 *   1. Extract "Authorization" header
 *   2. Does it start with "Bearer "? → NO → skip (let Spring handle as anonymous)
 *        ↓ YES
 *   3. Extract token (remove "Bearer " prefix)
 *   4. Extract email from token (JWT parse + signature verify)
 *   5. Is user already authenticated in this request? → YES → skip
 *        ↓ NO
 *   6. Load UserDetails from DB by email
 *   7. Is token valid for this user? → NO → skip (request will be rejected later)
 *        ↓ YES
 *   8. Build Authentication object, put it in SecurityContext
 *   9. Continue filter chain → request reaches controller ✅
 * </pre>
 *
 * <p><b>SecurityContextHolder:</b>
 * Spring Security's thread-local storage for the current request's authentication.
 * Once we put an Authentication object here, Spring knows "this request is authenticated
 * as user X with roles Y". Every {@code @PreAuthorize} and URL security rule reads from here.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTH_HEADER = "Authorization";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, CustomUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Step 1 & 2: Check for Authorization header with Bearer token
        final String authHeader = request.getHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // No token — pass the request along as anonymous
            filterChain.doFilter(request, response);
            return;
        }

        // Step 3: Extract the token (strip "Bearer " prefix — 7 characters)
        final String jwt = authHeader.substring(7);

        try {
            // Step 4: Extract email from token (JJWT verifies signature here)
            final String email = jwtService.extractEmail(jwt);

            // Step 5: Only proceed if we have an email AND user isn't already authenticated
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Step 6: Load user from database
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Step 7: Validate token (email match + not expired)
                if (jwtService.isTokenValid(jwt, userDetails)) {

                    // Step 8: Build Authentication object
                    // UsernamePasswordAuthenticationToken(principal, credentials, authorities)
                    // credentials = null because we don't need the password after authentication
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    // Attach request metadata (IP, session ID) to the authentication
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Step 8 cont.: Set in SecurityContext — request is now authenticated
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Authenticated user: {} for request: {}", email, request.getRequestURI());
                }
            }
        } catch (Exception ex) {
            // Token is malformed, expired, or signature invalid
            // DO NOT set authentication — the request remains anonymous
            // SecurityConfig will reject it with 401 if the endpoint requires auth
            log.warn("JWT validation failed for request {}: {}", request.getRequestURI(), ex.getMessage());
        }

        // Step 9: Always continue the filter chain — let SecurityConfig make the final call
        filterChain.doFilter(request, response);
    }
}
