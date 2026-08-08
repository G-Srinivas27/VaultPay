package com.srinivas.vaultpay.config;

import com.srinivas.vaultpay.auth.filter.JwtAuthenticationFilter;
import com.srinivas.vaultpay.auth.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security configuration — wires JWT authentication into the filter chain.
 *
 * <p><b>Key concepts added in this version:</b>
 *
 * <p><b>1. Stateless Session (STATELESS):</b>
 * By default Spring Security creates an HTTP session to track authentication.
 * With JWT we don't need sessions — the token IS the session.
 * Setting SessionCreationPolicy.STATELESS tells Spring to NEVER create a session.
 * This is essential for scalable REST APIs — any server instance can handle any request.
 *
 * <p><b>2. DaoAuthenticationProvider:</b>
 * The bridge between our {@link CustomUserDetailsService} (loads user from DB)
 * and our {@link PasswordEncoder} (verifies BCrypt hash).
 * This is what {@code AuthenticationManager} uses internally when we call authenticate().
 *
 * <p><b>3. Filter ordering:</b>
 * {@code addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)}
 * Our JWT filter runs BEFORE Spring's default username/password filter.
 * This ensures JWT tokens are processed on every request before Spring's default auth kicks in.
 *
 * <p><b>4. AuthenticationManager:</b>
 * Exposed as a @Bean so {@link com.srinivas.vaultpay.auth.service.AuthService} can inject it.
 * Without this @Bean declaration, we can't inject AuthenticationManager elsewhere.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // Activates @PreAuthorize, @PostAuthorize, @Secured on methods
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          CustomUserDetailsService userDetailsService) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // Enable CORS using our corsConfigurationSource() bean below.
            // CORS must be enabled here in Spring Security because the Security filter chain
            // runs BEFORE any controller — if Security doesn't allow the preflight OPTIONS
            // request, the browser never reaches the controller at all.
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers ->
                headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
            )
            // STATELESS — no HTTP session, every request must carry its own JWT
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // ── Always public ──────────────────────────────────────────
                .requestMatchers("/").permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers("/h2-console/**").permitAll()

                // ── Swagger UI + OpenAPI spec — public so devs can browse docs ──
                .requestMatchers("/swagger-ui/**").permitAll()
                .requestMatchers("/swagger-ui.html").permitAll()
                .requestMatchers("/v3/api-docs/**").permitAll()

                // ── Auth endpoints — must be public (login/register don't need a token) ──
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/users/register").permitAll()

                // ── Everything else REQUIRES a valid JWT ───────────────────
                .anyRequest().authenticated()
            )
            // Wire in our DaoAuthenticationProvider
            .authenticationProvider(authenticationProvider())
            // JWT filter runs BEFORE Spring's built-in UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Global CORS configuration — allows the React frontend to call this API.
     *
     * <p><b>Why configure CORS in Spring Security and not just @CrossOrigin?</b>
     * Spring Security's filter chain runs BEFORE any Spring MVC controller.
     * A browser sends a preflight OPTIONS request before every cross-origin POST.
     * If Spring Security intercepts and blocks that OPTIONS request first,
     * the controller's @CrossOrigin annotation is never even reached.
     * Configuring CORS here ensures the OPTIONS preflight is allowed at the
     * Security layer itself.
     *
     * <p><b>Allowed origins:</b>
     * localhost:5173 = Vite dev server (development)
     * In production, replace with your actual frontend domain.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Which origins (frontend URLs) are allowed to call this backend
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",   // Vite dev server
                "http://localhost:3000"    // CRA fallback (just in case)
        ));

        // Which HTTP methods are allowed
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Which request headers are allowed
        // Authorization is critical — this is how the JWT Bearer token is sent
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Trace-Id"));

        // Which response headers the browser is allowed to read
        // X-Trace-Id lets the frontend display or log the trace ID
        config.setExposedHeaders(List.of("X-Trace-Id", "X-Rate-Limit-Remaining", "Retry-After"));

        // Allow cookies / Authorization header to be sent cross-origin
        config.setAllowCredentials(true);

        // Cache preflight response for 1 hour (3600 seconds)
        // Browser won't send another OPTIONS request for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);  // Apply to ALL endpoints
        return source;
    }

    /**
     * DaoAuthenticationProvider — connects UserDetailsService + PasswordEncoder.
     * Used by AuthenticationManager to verify credentials during login.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes AuthenticationManager as a Spring Bean.
     * Required so AuthService can inject and call it for login verification.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt password encoder — singleton bean used across the application.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
