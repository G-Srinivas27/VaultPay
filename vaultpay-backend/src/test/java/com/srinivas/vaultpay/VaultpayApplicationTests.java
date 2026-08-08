package com.srinivas.vaultpay;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test — verifies the full Spring application context loads correctly.
 *
 * <p><b>@ActiveProfiles("dev"):</b>
 * Pins this test to the 'dev' profile which uses H2 in-memory database.
 * This ensures 'mvn test' never tries to connect to MySQL — tests are
 * fully self-contained and runnable without any external database.
 *
 * <p>The 'local' profile (MySQL) is only used when running the app manually.
 * Tests ALWAYS use H2, regardless of which profile is active in application.yaml.
 */
@SpringBootTest
@ActiveProfiles("dev")
class VaultpayApplicationTests {

    @Test
    void contextLoads() {
        // Verifies the entire Spring context (Security, JPA, JWT, Auditing) loads without errors
    }
}
