package com.ledgerflow.mongo.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * MongoDB Document Entity: AuditLog
 * Satisfies DBMS Experiential Learning Section 3: Second Database Paradigm (NoSQL MongoDB).
 */
@Document(collection = "audit_logs")
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private String eventType;

    @Indexed
    private String referenceNumber;

    @Indexed
    private String sourceAccountNumber;

    @Indexed
    private String destinationAccountNumber;

    private BigDecimal amount;

    @Indexed
    private String branchCode;

    private String actor;

    private String role;

    private String severity;

    @Indexed
    private Instant timestamp;

    private Map<String, Object> details = new HashMap<>();

    public AuditLog() {
        this.timestamp = Instant.now();
    }

    public AuditLog(String eventType, String referenceNumber, String sourceAccountNumber,
                    String destinationAccountNumber, BigDecimal amount, String branchCode,
                    String actor, String role, String severity, Map<String, Object> details) {
        this.eventType = eventType;
        this.referenceNumber = referenceNumber;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.amount = amount;
        this.branchCode = branchCode;
        this.actor = actor;
        this.role = role;
        this.severity = severity != null ? severity : "INFO";
        this.timestamp = Instant.now();
        if (details != null) {
            this.details.putAll(details);
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getReferenceNumber() { return referenceNumber; }
    public void setReferenceNumber(String referenceNumber) { this.referenceNumber = referenceNumber; }

    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public void setSourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }

    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public void setDestinationAccountNumber(String destinationAccountNumber) { this.destinationAccountNumber = destinationAccountNumber; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getBranchCode() { return branchCode; }
    public void setBranchCode(String branchCode) { this.branchCode = branchCode; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public Map<String, Object> getDetails() { return details; }
    public void setDetails(Map<String, Object> details) { this.details = details; }
}

