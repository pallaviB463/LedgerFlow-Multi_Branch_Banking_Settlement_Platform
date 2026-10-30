package com.ledgerflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * LedgerFlow Application Entry Point.
 * Configured for Polyglot Dual-Database Architecture:
 * - SQL Paradigm (PostgreSQL): Core double-entry ledger & ACID financial operations
 * - NoSQL Paradigm (MongoDB): Dynamic compliance audit logging & settlement receipt storage
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.ledgerflow.repository")
@EnableMongoRepositories(basePackages = "com.ledgerflow.mongo.repository")
public class LedgerFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(LedgerFlowApplication.class, args);
    }
}
