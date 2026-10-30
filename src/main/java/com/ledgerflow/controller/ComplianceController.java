package com.ledgerflow.controller;

import com.ledgerflow.mongo.document.AuditLog;
import com.ledgerflow.service.AuditService;
import com.ledgerflow.service.ComplianceAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * ComplianceController — NoSQL Compliance & Aggregation Pipeline APIs
 * Satisfies DBMS Experiential Learning Section 3 & 7:
 * - Second Database Paradigm (NoSQL MongoDB)
 * - Advanced NoSQL Feature: MongoDB Multi-Stage Aggregation Pipeline
 */
@RestController
@RequestMapping("/api/compliance")
public class ComplianceController {

    private final ComplianceAnalyticsService analyticsService;
    private final AuditService auditService;

    public ComplianceController(ComplianceAnalyticsService analyticsService, AuditService auditService) {
        this.analyticsService = analyticsService;
        this.auditService = auditService;
    }

    /**
     * Executes the MongoDB 4-stage aggregation pipeline to compute branch risk and volume distribution.
     * Demonstrates Section 7: NoSQL Aggregation Pipeline.
     */
    @GetMapping("/analytics/branch-risk")
    public ResponseEntity<List<Map>> getBranchRiskAnalytics() {
        return ResponseEntity.ok(analyticsService.aggregateBranchRiskMetrics());
    }

    /**
     * Executes the MongoDB aggregation pipeline to compute event velocity by event type.
     */
    @GetMapping("/analytics/event-distribution")
    public ResponseEntity<List<Map>> getEventTypeDistribution() {
        return ResponseEntity.ok(analyticsService.aggregateEventTypeDistribution());
    }

    /**
     * Retrieves the latest 50 audit logs stored in MongoDB.
     */
    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditService.getRecentLogs());
    }
}

