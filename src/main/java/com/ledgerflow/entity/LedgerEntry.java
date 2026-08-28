package com.ledgerflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A single line in the double-entry ledger.
 * Every transaction produces exactly one DEBIT and one CREDIT entry.
 * Table: ledger_entries
 */
@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_account_id", columnList = "account_id"),
        @Index(name = "idx_ledger_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_ledger_posted_at", columnList = "posted_at")
})
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;

    @NotNull
    @Positive(message = "Ledger entry amount must be positive")
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @NotNull
    @Column(name = "running_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal runningBalance;

    @Column(length = 255)
    private String description;

    @CreationTimestamp
    @Column(name = "posted_at", nullable = false, updatable = false)
    private Instant postedAt;

    public LedgerEntry() {}

    public LedgerEntry(Account account, Transaction transaction,
                       EntryType entryType, BigDecimal amount,
                       BigDecimal runningBalance, String description) {
        this.account = account;
        this.transaction = transaction;
        this.entryType = entryType;
        this.amount = amount;
        this.runningBalance = runningBalance;
        this.description = description;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }
    public Transaction getTransaction() { return transaction; }
    public void setTransaction(Transaction transaction) { this.transaction = transaction; }
    public EntryType getEntryType() { return entryType; }
    public void setEntryType(EntryType entryType) { this.entryType = entryType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getRunningBalance() { return runningBalance; }
    public void setRunningBalance(BigDecimal runningBalance) { this.runningBalance = runningBalance; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getPostedAt() { return postedAt; }
    public void setPostedAt(Instant postedAt) { this.postedAt = postedAt; }

    public enum EntryType { DEBIT, CREDIT }
}
