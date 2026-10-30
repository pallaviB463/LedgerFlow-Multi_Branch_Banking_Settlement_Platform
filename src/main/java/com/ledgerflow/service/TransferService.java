package com.ledgerflow.service;

import com.ledgerflow.dto.TransferDto;
import com.ledgerflow.entity.Account;
import com.ledgerflow.entity.Transaction;
import com.ledgerflow.exception.InsufficientBalanceException;
import com.ledgerflow.exception.InvalidOperationException;
import com.ledgerflow.exception.ResourceNotFoundException;
import com.ledgerflow.repository.AccountRepository;
import com.ledgerflow.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TransferService — Atomic Fund Transfer Engine.
 *
 * Uses REPEATABLE_READ isolation to prevent lost-update anomalies.
 * Always loads accounts in consistent order (low ID first) to prevent deadlocks.
 * All steps (validate, create transaction, post ledger, update balances) happen
 * within one @Transactional boundary — full rollback on any failure.
 *
 * Dual-write event bus logs compliance audits to MongoDB (NoSQL Paradigm).
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final AuditService auditService;
    private final AtomicLong refSequence = new AtomicLong(0);

    public TransferService(AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           LedgerService ledgerService,
                           @Autowired(required = false) AuditService auditService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerService = ledgerService;
        this.auditService = auditService;
    }

    /**
     * Executes an atomic transfer between two accounts.
     * Full transactional boundary — any failure causes complete rollback.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TransferDto.TransferResponse executeTransfer(TransferDto.TransferRequest request) {
        // Validation: cannot transfer to the same account
        if (request.getSourceAccountId().equals(request.getDestinationAccountId())) {
            throw new InvalidOperationException("Source and destination accounts must be different");
        }

        // Concurrency Control: Load accounts in consistent order (lower ID first) with PESSIMISTIC_WRITE lock
        Long firstId = Math.min(request.getSourceAccountId(), request.getDestinationAccountId());
        Long secondId = Math.max(request.getSourceAccountId(), request.getDestinationAccountId());

        Account firstAccount = loadAccountWithLock(firstId);
        Account secondAccount = loadAccountWithLock(secondId);

        Account sourceAccount = request.getSourceAccountId().equals(firstId) ? firstAccount : secondAccount;
        Account destAccount   = request.getDestinationAccountId().equals(firstId) ? firstAccount : secondAccount;

        // Validation: both accounts must be ACTIVE
        validateAccountActive(sourceAccount, "Source");
        validateAccountActive(destAccount, "Destination");

        // Validation: source must have sufficient funds
        if (sourceAccount.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance in account " + sourceAccount.getAccountNumber()
                    + ". Available: " + sourceAccount.getBalance()
                    + ", Requested: " + request.getAmount());
        }

        // Create transaction record (status: PROCESSING)
        Transaction transaction = new Transaction();
        transaction.setTransactionType(Transaction.TransactionType.TRANSFER);
        transaction.setAmount(request.getAmount());
        transaction.setStatus(Transaction.TransactionStatus.PROCESSING);
        transaction.setDescription(request.getDescription());
        transaction.setReferenceNumber(generateReferenceNumber());
        Transaction savedTxn = transactionRepository.save(transaction);

        // Post double-entry ledger (updates in-memory balances on both accounts)
        String desc = request.getDescription() != null
                ? request.getDescription()
                : "Transfer " + savedTxn.getReferenceNumber();
        ledgerService.postDoubleEntry(sourceAccount, destAccount, request.getAmount(), savedTxn, desc);

        // Persist updated balances
        accountRepository.save(sourceAccount);
        accountRepository.save(destAccount);

        // Mark complete
        savedTxn.setStatus(Transaction.TransactionStatus.COMPLETED);
        savedTxn.setCompletedAt(Instant.now());
        transactionRepository.save(savedTxn);

        log.info("Transfer COMPLETED: ref={}, amount={}, src={} bal={}, dst={} bal={}",
                savedTxn.getReferenceNumber(), request.getAmount(),
                sourceAccount.getAccountNumber(), sourceAccount.getBalance(),
                destAccount.getAccountNumber(), destAccount.getBalance());

        // Publish NoSQL Audit Log event to MongoDB
        if (auditService != null) {
            String branchCode = sourceAccount.getBranch() != null ? sourceAccount.getBranch().getBranchCode() : "HQ";
            auditService.logFinancialEvent(
                    "TRANSFER_RTGS",
                    savedTxn.getReferenceNumber(),
                    sourceAccount.getAccountNumber(),
                    destAccount.getAccountNumber(),
                    request.getAmount(),
                    branchCode,
                    "SYSTEM",
                    "CUSTOMER",
                    "INFO",
                    Map.of("description", desc, "status", "COMPLETED")
            );
        }

        TransferDto.TransferResponse resp = new TransferDto.TransferResponse();
        resp.setTransactionId(savedTxn.getId());
        resp.setReferenceNumber(savedTxn.getReferenceNumber());
        resp.setTransactionType(savedTxn.getTransactionType());
        resp.setStatus(savedTxn.getStatus());
        resp.setAmount(savedTxn.getAmount());
        resp.setCurrency(savedTxn.getCurrency());
        resp.setSourceAccountId(sourceAccount.getId());
        resp.setSourceAccountNumber(sourceAccount.getAccountNumber());
        resp.setSourceBalanceAfter(sourceAccount.getBalance());
        resp.setDestinationAccountId(destAccount.getId());
        resp.setDestinationAccountNumber(destAccount.getAccountNumber());
        resp.setDestinationBalanceAfter(destAccount.getBalance());
        resp.setDescription(savedTxn.getDescription());
        resp.setCreatedAt(savedTxn.getCreatedAt());
        return resp;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public TransferDto.AccountLedgerResponse getAccountLedger(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
        TransferDto.AccountLedgerResponse resp = new TransferDto.AccountLedgerResponse();
        resp.setAccountId(account.getId());
        resp.setAccountNumber(account.getAccountNumber());
        resp.setAccountHolderName(account.getAccountHolderName());
        resp.setCurrentBalance(account.getBalance());
        resp.setLedgerBalance(ledgerService.calculateLedgerBalance(accountId));
        resp.setEntries(ledgerService.getAccountLedger(accountId));
        return resp;
    }

    private Account loadAccountWithLock(Long id) {
        return accountRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
    }

    private void validateAccountActive(Account account, String label) {
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new InvalidOperationException(
                    label + " account " + account.getAccountNumber()
                    + " is not ACTIVE. Current status: " + account.getStatus());
        }
    }

    private String generateReferenceNumber() {
        String datePart = DateTimeFormatter.ofPattern("yyyyMMdd'Z'")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());
        long seq = refSequence.incrementAndGet() % 1_000_000;
        return String.format("TXN-%s-%06d", datePart, seq);
    }
}
