package com.ledgerflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a customer's bank account.
 *
 * An account belongs to a specific branch and holds a balance.
 * IMPORTANT: balance must NEVER be updated directly with arithmetic —
 * it must always be derived from the double-entry ledger (Phase 4+).
 * In Phases 2-3, we allow direct updates for bootstrapping purposes only.
 *
 * Table: accounts
 */
@Entity
@Table(name = "accounts", indexes = {
        @Index(name = "idx_accounts_account_number", columnList = "account_number"),
        @Index(name = "idx_accounts_branch_id", columnList = "branch_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Account {

    /**
     * Auto-generated unique identifier.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable account number (e.g., "ACC-10001").
     * Unique across all branches.
     */
    @NotBlank(message = "Account number is required")
    @Size(max = 30, message = "Account number must not exceed 30 characters")
    @Column(name = "account_number", nullable = false, unique = true, length = 30)
    private String accountNumber;

    /**
     * Name of the account holder.
     */
    @NotBlank(message = "Account holder name is required")
    @Size(max = 150, message = "Account holder name must not exceed 150 characters")
    @Column(name = "account_holder_name", nullable = false, length = 150)
    private String accountHolderName;

    /**
     * Type of account: SAVINGS or CURRENT.
     * Stored as a string in the database (e.g., "SAVINGS").
     */
    @NotNull(message = "Account type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    /**
     * Current balance.
     * IMPORTANT: Money must NEVER be stored as double or float — always BigDecimal.
     * precision = 19: total digits. scale = 4: digits after decimal point.
     * This handles up to 999,999,999,999,999.9999
     */
    @NotNull(message = "Balance is required")
    @PositiveOrZero(message = "Balance cannot be negative")
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * Account status. Only ACTIVE accounts can send or receive transfers.
     */
    @NotNull(message = "Account status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    /**
     * The branch this account belongs to.
     * @ManyToOne: Many accounts can belong to one branch.
     * LAZY fetch: the branch details aren't loaded until accessed.
     * JoinColumn: the foreign key column in the accounts table is 'branch_id'.
     */
    @NotNull(message = "Branch is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    /**
     * All ledger entries (debits/credits) on this account.
     */
    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LedgerEntry> ledgerEntries = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Convenience constructor for creating a new account.
     */
    public Account(String accountNumber, String accountHolderName,
                   AccountType accountType, BigDecimal initialBalance, Branch branch) {
        this.accountNumber = accountNumber;
        this.accountHolderName = accountHolderName;
        this.accountType = accountType;
        this.balance = initialBalance;
        this.branch = branch;
        this.status = AccountStatus.ACTIVE;
    }

    // ---- Enums defined as inner types (closely coupled to Account) ----

    /**
     * Type of the bank account.
     */
    public enum AccountType {
        SAVINGS,   // Savings account
        CURRENT    // Current / checking account
    }

    /**
     * Lifecycle status of the account.
     * Only ACTIVE accounts can participate in transfers.
     */
    public enum AccountStatus {
        ACTIVE,    // Normal operational state
        FROZEN,    // Temporarily blocked (e.g., suspicious activity)
        CLOSED     // Permanently closed
    }
}
