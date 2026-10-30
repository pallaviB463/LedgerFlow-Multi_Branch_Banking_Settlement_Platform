package com.ledgerflow.service;

import com.ledgerflow.mongo.document.AuditLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ComplianceAnalyticsService — MongoDB Aggregation Pipeline Engine
 * Satisfies DBMS Experiential Learning Section 7:
 * "At least one NOSQL/Vector concept, such as NoSQL aggregation pipeline".
 */
@Service
public class ComplianceAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(ComplianceAnalyticsService.class);

    // Optional injection: In test profile without live MongoDB, template will be null or mocked
    private final MongoTemplate mongoTemplate;

    public ComplianceAnalyticsService(@Autowired(required = false) MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Executes a 4-Stage MongoDB Aggregation Pipeline to generate Branch Settlement & Compliance Risk Analytics:
     * Stage 1: $match   - Filter records within the last 30 days
     * Stage 2: $group   - Aggregate total amount, event counts, and average amount per branchCode and eventType
     * Stage 3: $project - Reshape fields and compute net volume metrics
     * Stage 4: $sort    - Order descending by total settled volume
     */
    public List<Map> aggregateBranchRiskMetrics() {
        if (mongoTemplate == null) {
            log.warn("MongoTemplate not active — returning empty analytics in current profile.");
            return Collections.emptyList();
        }

        try {
            Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);

            // Stage 1: $match
            MatchOperation matchStage = Aggregation.match(
                    Criteria.where("timestamp").gte(thirtyDaysAgo)
                            .and("branchCode").ne(null)
            );

            // Stage 2: $group
            GroupOperation groupStage = Aggregation.group("branchCode", "eventType")
                    .sum("amount").as("totalVolume")
                    .count().as("eventCount")
                    .avg("amount").as("avgTransactionAmount");

            // Stage 3: $project
            ProjectionOperation projectStage = Aggregation.project()
                    .and("_id.branchCode").as("branchCode")
                    .and("_id.eventType").as("eventType")
                    .and("totalVolume").as("totalVolume")
                    .and("eventCount").as("eventCount")
                    .and("avgTransactionAmount").as("avgTransactionAmount");

            // Stage 4: $sort
            SortOperation sortStage = Aggregation.sort(Sort.Direction.DESC, "totalVolume");

            TypedAggregation<AuditLog> aggregation = Aggregation.newAggregation(
                    AuditLog.class,
                    matchStage,
                    groupStage,
                    projectStage,
                    sortStage
            );

            AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, Map.class);
            return results.getMappedResults();
        } catch (Exception e) {
            log.error("MongoDB Aggregation Pipeline error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Executes a MongoDB Aggregation Pipeline grouping by Event Type across the whole banking network:
     * Stage 1: $group - sum volume and count by eventType
     * Stage 2: $sort  - order by count descending
     */
    public List<Map> aggregateEventTypeDistribution() {
        if (mongoTemplate == null) {
            return Collections.emptyList();
        }

        try {
            GroupOperation groupStage = Aggregation.group("eventType")
                    .count().as("count")
                    .sum("amount").as("totalAmount");

            ProjectionOperation projectStage = Aggregation.project()
                    .and("_id").as("eventType")
                    .and("count").as("count")
                    .and("totalAmount").as("totalAmount");

            SortOperation sortStage = Aggregation.sort(Sort.Direction.DESC, "count");

            TypedAggregation<AuditLog> aggregation = Aggregation.newAggregation(
                    AuditLog.class,
                    groupStage,
                    projectStage,
                    sortStage
            );

            AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, Map.class);
            return results.getMappedResults();
        } catch (Exception e) {
            log.error("MongoDB Event Distribution Pipeline error: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}

