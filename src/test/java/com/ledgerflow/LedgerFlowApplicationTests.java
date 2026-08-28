package com.ledgerflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke Test: verifies that the Spring application context loads successfully.
 *
 * This test does NOT test any business logic. It simply checks that:
 *   1. All Spring beans can be created
 *   2. Configuration is valid
 *   3. No startup errors occur
 *
 * The @ActiveProfiles("test") annotation tells Spring to use application-test.yml,
 * which uses the H2 in-memory database instead of PostgreSQL.
 */
@SpringBootTest
@ActiveProfiles("test")
class LedgerFlowApplicationTests {

    @Test
    void contextLoads() {
        // If this test passes, the Spring application context loaded successfully.
        // No assertions needed — the test fails automatically if context loading throws an exception.
    }
}
