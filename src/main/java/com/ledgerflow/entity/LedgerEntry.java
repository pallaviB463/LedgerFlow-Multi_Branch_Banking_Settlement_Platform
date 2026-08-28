package com.ledgerflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a single line in the double-entry ledger.
 *
 * DOUBLE-ENTRY BOOKKEEPING EXPLAINED:
 * Every financial transaction must be recorded as two ledger entries:
 *
 *   Example — Alice transfers ₹500 to Bob:
 *   ┌──────────────────────────────────────────────────────────┐
 *   │ LedgerEntry 1: Account=Alice, Type=DEBIT,  Amount=500   │
 *   │ LedgerEntry 2: Account=Bob,   Type=CREDIT, Amount=500   │
 *   └──────────────────────────────────────────────────────────┘
 *
 * The accounting equation must always hold:
 *   Total DEBITS = Total CREDITS  (for every transaction)
 *
 * We NEVER delete a ledger entry to fix a mistake.
 * Instead, we post a REVERSAL transaction with opposing entries.
 * This keeps the audit trail complete and tamper-evident.
 *
 * Table: ledger_entries
 */
@Entity
@Table(name = "ledger_entries", indexes = {
        @Index(name = "idx_ledger_account_id", columnList = "account_id"),
        @Index(name = "idx_ledger_transaction_id", columnList = "transaction_id"),
        @Index(name = "idx_ledger_posted_at", columnList = "posted_at")
})
@Getter
@Setter
@NoArgsConstructor
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The account this entry belongs to.
     * Foreign key → accounts.id
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    /**
     * The transaction this entry belongs to.
     * Foreign key → transactions.id
     */
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    /**
     * Whether this entry is a DEBIT or a CREDIT.
     *
     * DEBIT  = money is flowing OUT of the account (or being charged)
     * CREDIT = money is flowing INTO the account (or being received)
     *
     * For a transfer of ₹500 from Alice to Bob:
     *   Alice gets a DEBIT  entry (her balance goes down)
     *   Bob   gets a CREDIT entry (his balance goes up)
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;

    /**
     * The monetary amount of this entry.
     * Always positive — the sign of the money movement is captured by entryType.
     */
    @NotNull
    @Positive(message = "Ledger entry amount must be positive")
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * Running balance of the account AFTER this entry was posted.
     * This is a denormalized snapshot — useful for statement generation
     * without having to replay all entries.
     */
    @NotNull
    @Column(name = "running_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal runningBalance;

    /**
     * Optional description of this specific ledger line.
     * Usually copied from the transaction description.
     */
    @Column(length = 255)
    private String description;

    /**
     * Timestamp when this entry was posted to the ledger.
     * Immutable — never updated after INSERT.
     */
    @CreationTimestamp
    @Column(name = "posted_at", nullable = false, updatable = false)
    private Instant postedAt;

    /**
     * Convenience constructor for creating a ledger entry.
     */
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

    // ---------- Enum ----------

    /**
     * The type of this ledger entry.
     *
     * DEBIT  → money leaving the account  (account balance decreases)
     * CREDIT → money entering the account (account balance increases)
     */
    public enum EntryType {
        DEBIT,
        CREDIT
    }
}
