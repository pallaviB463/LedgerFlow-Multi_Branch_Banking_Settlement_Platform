package com.ledgerflow.dto;

import com.ledgerflow.entity.Transaction;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** DTOs for Transfer and Ledger operations. */
public class TransferDto {

    /** Request body for POST /api/transfers */
    public static class TransferRequest {
        @NotNull(message = "Source account ID is required")
        @Positive(message = "Source account ID must be positive")
        private Long sourceAccountId;

        @NotNull(message = "Destination account ID is required")
        @Positive(message = "Destination account ID must be positive")
        private Long destinationAccountId;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", inclusive = true, message = "Transfer amount must be at least 0.01")
        @Digits(integer = 15, fraction = 4)
        private BigDecimal amount;

        @Size(max = 255)
        private String description;

        public TransferRequest() {}
        public Long getSourceAccountId() { return sourceAccountId; }
        public void setSourceAccountId(Long v) { this.sourceAccountId = v; }
        public Long getDestinationAccountId() { return destinationAccountId; }
        public void setDestinationAccountId(Long v) { this.destinationAccountId = v; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal v) { this.amount = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { this.description = v; }
    }

    /** Response after a successful transfer. */
    public static class TransferResponse {
        private Long transactionId;
        private String referenceNumber;
        private Transaction.TransactionType transactionType;
        private Transaction.TransactionStatus status;
        private BigDecimal amount;
        private String currency;
        private Long sourceAccountId;
        private String sourceAccountNumber;
        private BigDecimal sourceBalanceAfter;
        private Long destinationAccountId;
        private String destinationAccountNumber;
        private BigDecimal destinationBalanceAfter;
        private String description;
        private Instant createdAt;
        private Instant completedAt;

        public TransferResponse() {}

        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long v) { this.transactionId = v; }
        public String getReferenceNumber() { return referenceNumber; }
        public void setReferenceNumber(String v) { this.referenceNumber = v; }
        public Transaction.TransactionType getTransactionType() { return transactionType; }
        public void setTransactionType(Transaction.TransactionType v) { this.transactionType = v; }
        public Transaction.TransactionStatus getStatus() { return status; }
        public void setStatus(Transaction.TransactionStatus v) { this.status = v; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal v) { this.amount = v; }
        public String getCurrency() { return currency; }
        public void setCurrency(String v) { this.currency = v; }
        public Long getSourceAccountId() { return sourceAccountId; }
        public void setSourceAccountId(Long v) { this.sourceAccountId = v; }
        public String getSourceAccountNumber() { return sourceAccountNumber; }
        public void setSourceAccountNumber(String v) { this.sourceAccountNumber = v; }
        public BigDecimal getSourceBalanceAfter() { return sourceBalanceAfter; }
        public void setSourceBalanceAfter(BigDecimal v) { this.sourceBalanceAfter = v; }
        public Long getDestinationAccountId() { return destinationAccountId; }
        public void setDestinationAccountId(Long v) { this.destinationAccountId = v; }
        public String getDestinationAccountNumber() { return destinationAccountNumber; }
        public void setDestinationAccountNumber(String v) { this.destinationAccountNumber = v; }
        public BigDecimal getDestinationBalanceAfter() { return destinationBalanceAfter; }
        public void setDestinationBalanceAfter(BigDecimal v) { this.destinationBalanceAfter = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { this.description = v; }
        public Instant getCreatedAt() { return createdAt; }
        public void setCreatedAt(Instant v) { this.createdAt = v; }
        public Instant getCompletedAt() { return completedAt; }
        public void setCompletedAt(Instant v) { this.completedAt = v; }
    }

    /** A single line in a ledger statement. */
    public static class LedgerEntryView {
        private Long ledgerEntryId;
        private Long transactionId;
        private String referenceNumber;
        private String entryType;
        private BigDecimal amount;
        private BigDecimal runningBalance;
        private String description;
        private Instant postedAt;

        public LedgerEntryView() {}
        public Long getLedgerEntryId() { return ledgerEntryId; }
        public void setLedgerEntryId(Long v) { this.ledgerEntryId = v; }
        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long v) { this.transactionId = v; }
        public String getReferenceNumber() { return referenceNumber; }
        public void setReferenceNumber(String v) { this.referenceNumber = v; }
        public String getEntryType() { return entryType; }
        public void setEntryType(String v) { this.entryType = v; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal v) { this.amount = v; }
        public BigDecimal getRunningBalance() { return runningBalance; }
        public void setRunningBalance(BigDecimal v) { this.runningBalance = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { this.description = v; }
        public Instant getPostedAt() { return postedAt; }
        public void setPostedAt(Instant v) { this.postedAt = v; }
    }

    /** Full account ledger / statement response. */
    public static class AccountLedgerResponse {
        private Long accountId;
        private String accountNumber;
        private String accountHolderName;
        private BigDecimal currentBalance;
        private BigDecimal ledgerBalance;
        private List<LedgerEntryView> entries;

        public AccountLedgerResponse() {}
        public Long getAccountId() { return accountId; }
        public void setAccountId(Long v) { this.accountId = v; }
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String v) { this.accountNumber = v; }
        public String getAccountHolderName() { return accountHolderName; }
        public void setAccountHolderName(String v) { this.accountHolderName = v; }
        public BigDecimal getCurrentBalance() { return currentBalance; }
        public void setCurrentBalance(BigDecimal v) { this.currentBalance = v; }
        public BigDecimal getLedgerBalance() { return ledgerBalance; }
        public void setLedgerBalance(BigDecimal v) { this.ledgerBalance = v; }
        public List<LedgerEntryView> getEntries() { return entries; }
        public void setEntries(List<LedgerEntryView> v) { this.entries = v; }
    }
}
