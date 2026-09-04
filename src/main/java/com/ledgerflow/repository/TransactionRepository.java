package com.ledgerflow.repository;

import com.ledgerflow.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Transaction entities.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /** Find a transaction by its reference number. */
    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    /** Find all transactions belonging to an ACH batch. */
    List<Transaction> findByAchBatchId(Long achBatchId);

    /** Find all transactions with a specific status. */
    List<Transaction> findByStatus(Transaction.TransactionStatus status);

    /** Check if a reference number is already used. */
    boolean existsByReferenceNumber(String referenceNumber);
}
