package com.ledgerflow.repository;

import com.ledgerflow.entity.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Account entities.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByBranchId(Long branchId);

    List<Account> findByStatus(Account.AccountStatus status);

    List<Account> findByBranchIdAndAccountType(Long branchId, Account.AccountType accountType);

    /**
     * Load an account with a PESSIMISTIC WRITE lock (SELECT ... FOR UPDATE).
     *
     * WHY THIS MATTERS:
     * Without this lock, two concurrent transactions can both read the same
     * balance, both see "enough funds", and both deduct — causing a double-spend.
     *
     * With this lock:
     *   - Thread 1 loads account → database row is locked
     *   - Thread 2 tries to load the same account → BLOCKED at the DB level
     *   - Thread 1 commits, releasing the lock
     *   - Thread 2 now sees the UPDATED balance from Thread 1
     *   - Thread 2 re-validates "enough funds" against the real current balance
     *
     * This is the core of ACID "I" (Isolation) in action.
     *
     * @Lock(PESSIMISTIC_WRITE) → SQL: SELECT * FROM accounts WHERE id=? FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);
}

