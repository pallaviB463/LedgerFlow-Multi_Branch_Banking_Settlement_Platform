package com.ledgerflow.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health Check Controller.
 *
 * Provides a simple endpoint to verify:
 *   1. The application is running
 *   2. The database connection is working
 *
 * This is useful for:
 *   - Quick manual verification during development
 *   - Person B's frontend to check if the backend is alive
 *   - Docker/deployment health checks in the future
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final DataSource dataSource;

    /**
     * Constructor injection: Spring automatically provides the DataSource
     * (database connection pool) that was configured in application.yml.
     */
    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * GET /api/health
     *
     * Returns a JSON object with:
     *   - status: "UP" if everything is working
     *   - application: the app name
     *   - database: whether the DB connection is healthy
     *   - timestamp: current server time
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("application", "LedgerFlow");
        response.put("timestamp", Instant.now().toString());

        // Test database connectivity
        String dbStatus;
        try (Connection conn = dataSource.getConnection()) {
            dbStatus = conn.isValid(2) ? "CONNECTED" : "UNREACHABLE";
        } catch (Exception e) {
            dbStatus = "ERROR: " + e.getMessage();
        }
        response.put("database", dbStatus);

        return ResponseEntity.ok(response);
    }
}
