package com.srinivas.vaultpay.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration for VaultPay.
 *
 * <p><b>What this class does:</b>
 * <ul>
 *   <li>Defines the API metadata (title, description, version, contact)</li>
 *   <li>Registers the JWT Bearer authentication scheme in Swagger UI</li>
 *   <li>Makes the "Authorize" button appear in the Swagger UI — so you can
 *       paste your JWT once and it's sent with every request automatically</li>
 *   <li>Lists the server URL (localhost for dev)</li>
 * </ul>
 *
 * <p><b>How JWT works in Swagger UI:</b>
 * <pre>
 *   1. Open http://localhost:8080/swagger-ui.html
 *   2. Call POST /api/auth/login → copy the token from response
 *   3. Click "Authorize" button (top right)
 *   4. Type: Bearer eyJhbGci...
 *   5. Click Authorize → Close
 *   6. Now ALL protected endpoints automatically send the token
 * </pre>
 *
 * <p><b>SecurityScheme explained:</b>
 * We declare a scheme named "bearerAuth" with:
 * <ul>
 *   <li>type = HTTP — uses the standard HTTP Authorization header</li>
 *   <li>scheme = bearer — Authorization: Bearer {token}</li>
 *   <li>bearerFormat = JWT — hints to the UI what format the token is</li>
 * </ul>
 * This is the industry-standard way to document JWT APIs.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI vaultPayOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(localServer()))
                // Register the JWT security scheme globally
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, jwtSecurityScheme())
                )
                // Apply JWT requirement to ALL endpoints by default
                // Individual endpoints can override this with @SecurityRequirements({})
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    /**
     * Describes the API — shows in the Swagger UI header.
     */
    private Info apiInfo() {
        return new Info()
                .title("VaultPay API")
                .description("""
                        ## Digital Wallet Banking API
                        
                        VaultPay is a production-grade digital wallet system built with Spring Boot.
                        
                        ### Features
                        - 👤 **User Management** — register, profile, role management
                        - 💳 **Wallet Management** — create and manage digital wallets
                        - 💸 **Transactions** — deposit, withdraw, peer-to-peer transfer
                        - 🔐 **JWT Authentication** — stateless, secure
                        - 🛡️ **RBAC** — USER and ADMIN roles
                        
                        ### Authentication
                        1. Register via `POST /api/users/register`
                        2. Login via `POST /api/auth/login` → copy the token
                        3. Click **Authorize** above → enter `Bearer <your-token>`
                        4. All protected endpoints will use the token automatically
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Srinivas Gooda")
                        .email("srinivas@vaultpay.com"))
                .license(new License()
                        .name("MIT License"));
    }

    /**
     * Defines the local development server.
     * Add more servers (staging, prod) here when deploying.
     */
    private Server localServer() {
        return new Server()
                .url("http://localhost:8080")
                .description("Local Development Server");
    }

    /**
     * JWT Bearer token security scheme.
     *
     * <p>This is what makes the "Authorize" lock icon appear on each endpoint
     * and the global "Authorize" button appear in the Swagger UI header.
     * Developers paste their JWT once → Swagger sends it as
     * "Authorization: Bearer <token>" on every subsequent request.
     */
    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(SECURITY_SCHEME_NAME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Paste your JWT token here. Get it from POST /api/auth/login");
    }
}
