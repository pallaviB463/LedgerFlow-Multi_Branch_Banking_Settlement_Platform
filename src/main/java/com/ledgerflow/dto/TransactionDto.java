package com.ledgerflow.dto;

import com.ledgerflow.entity.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

/** DTO for exposing Transaction data over the REST API. */
public class TransactionDto {

    private Long id;
    private String referenceNumber;
    private Transaction.TransactionType transactionType;
    private Transaction.TransactionStatus status;
    private BigDecimal amount;
    private String currency;
    private String description;
    private Long achBatchId;
    private Instant createdAt;
    private Instant completedAt;

    public TransactionDto() {}

    public static TransactionDto from(Transaction txn) {
        TransactionDto d = new TransactionDto();
        d.id = txn.getId();
        d.referenceNumber = txn.getReferenceNumber();
        d.transactionType = txn.getTransactionType();
        d.status = txn.getStatus();
        d.amount = txn.getAmount();
        d.currency = txn.getCurrency();
        d.description = txn.getDescription();
        d.achBatchId = txn.getAchBatchId();
        d.createdAt = txn.getCreatedAt();
        d.completedAt = txn.getCompletedAt();
        return d;
    }

    public Long getId() { return id; }
    public String getReferenceNumber() { return referenceNumber; }
    public Transaction.TransactionType getTransactionType() { return transactionType; }
    public Transaction.TransactionStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getDescription() { return description; }
    public Long getAchBatchId() { return achBatchId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
