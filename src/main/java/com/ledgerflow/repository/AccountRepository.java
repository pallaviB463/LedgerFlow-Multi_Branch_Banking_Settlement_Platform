package com.ledgerflow.repository;

import com.ledgerflow.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Account entities.
 *
 * Provides database access for accounts without writing SQL.
 * Spring Data JPA generates queries from the method names.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /** Find an account by its account number (e.g., "ACC-10001"). */
    Optional<Account> findByAccountNumber(String accountNumber);

    /** Check if an account number is already in use. */
    boolean existsByAccountNumber(String accountNumber);

    /** Find all accounts belonging to a specific branch. */
    List<Account> findByBranchId(Long branchId);

    /** Find all accounts with a specific status. */
    List<Account> findByStatus(Account.AccountStatus status);

    /** Find all accounts of a specific type in a branch. */
    List<Account> findByBranchIdAndAccountType(Long branchId, Account.AccountType accountType);
}
