package com.ledgerflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LedgerFlow Application Entry Point.
 *
 * This is the main class that starts the entire Spring Boot application.
 * The @SpringBootApplication annotation tells Spring to:
 *   1. Auto-configure based on dependencies (e.g., set up JPA, web server)
 *   2. Scan for components (controllers, services, repositories) in this package and below
 *   3. Enable Spring Boot's configuration mechanisms
 */
@SpringBootApplication
public class LedgerFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerFlowApplication.class, args);
    }
}
