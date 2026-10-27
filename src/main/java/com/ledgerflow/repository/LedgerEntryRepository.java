package com.ledgerflow.repository;

import com.ledgerflow.entity.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Repository for LedgerEntry entities.
 *
 * The ledger is append-only — entries are NEVER deleted or updated.
 * All "corrections" are done via REVERSAL transactions.
 */
@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    /** Get all ledger entries for a specific account, newest first. */
    List<LedgerEntry> findByAccountIdOrderByPostedAtDesc(Long accountId);

    /** Get all ledger entries for a specific transaction. */
    List<LedgerEntry> findByTransactionId(Long transactionId);

    /**
     * Calculate the current balance of an account by summing its ledger entries.
     *
     * This is the "true" balance computed from the immutable ledger history.
     *
     * Formula:
     *   SUM of all CREDIT amounts - SUM of all DEBIT amounts
     *
     * We use JPQL (Java Persistence Query Language) here because the logic
     * is more complex than a simple method name.
     *
     * COALESCE ensures we return 0 instead of NULL if no entries exist.
     */
    @Query("""
            SELECT COALESCE(
                SUM(CASE WHEN e.entryType = 'CREDIT' THEN e.amount ELSE -e.amount END),
                0
            )
            FROM LedgerEntry e
            WHERE e.account.id = :accountId
            """)
    BigDecimal calculateBalanceByAccountId(@Param("accountId") Long accountId);
    boolean existsByAccountId(Long accountId);
}
