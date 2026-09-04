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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TransferService — Atomic Fund Transfer Engine.
 *
 * Uses REPEATABLE_READ isolation to prevent lost-update anomalies.
 * Always loads accounts in consistent order (low ID first) to prevent deadlocks.
 * All steps (validate, create transaction, post ledger, update balances) happen
 * within one @Transactional boundary — full rollback on any failure.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final AtomicLong refSequence = new AtomicLong(0);

    public TransferService(AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           LedgerService ledgerService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerService = ledgerService;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public TransferDto.TransferResponse executeTransfer(TransferDto.TransferRequest request) {
        log.info("Initiating transfer: {} → {}, amount={}",
                request.getSourceAccountId(), request.getDestinationAccountId(), request.getAmount());

        // Reject self-transfer
        if (request.getSourceAccountId().equals(request.getDestinationAccountId()))
            throw new InvalidOperationException("Source and destination accounts must be different");

        // Load in consistent order to prevent deadlocks
        Long lowId  = Math.min(request.getSourceAccountId(), request.getDestinationAccountId());
        Long highId = Math.max(request.getSourceAccountId(), request.getDestinationAccountId());
        Account lowAccount  = loadAccountOrThrow(lowId);
        Account highAccount = loadAccountOrThrow(highId);

        Account sourceAccount = request.getSourceAccountId().equals(lowId) ? lowAccount : highAccount;
        Account destAccount   = request.getDestinationAccountId().equals(lowId) ? lowAccount : highAccount;

        // Validate
        validateAccountForDebit(sourceAccount);
        validateAccountForCredit(destAccount);
        validateSufficientBalance(sourceAccount, request.getAmount());

        // Create transaction record
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
        resp.setCompletedAt(savedTxn.getCompletedAt());
        return resp;
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public TransferDto.AccountLedgerResponse getAccountLedger(Long accountId) {
        Account account = loadAccountOrThrow(accountId);
        TransferDto.AccountLedgerResponse resp = new TransferDto.AccountLedgerResponse();
        resp.setAccountId(account.getId());
        resp.setAccountNumber(account.getAccountNumber());
        resp.setAccountHolderName(account.getAccountHolderName());
        resp.setCurrentBalance(account.getBalance());
        resp.setLedgerBalance(ledgerService.calculateLedgerBalance(accountId));
        resp.setEntries(ledgerService.getAccountLedger(accountId));
        return resp;
    }

    private Account loadAccountOrThrow(Long id) {
        // findByIdWithLock → SELECT ... FOR UPDATE
        // This acquires a row-level database lock.
        // Any other transaction trying to lock the same row will WAIT here
        // until this transaction commits or rolls back.
        return accountRepository.findByIdWithLock(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
    }

    private void validateAccountForDebit(Account account) {
        if (account.getStatus() != Account.AccountStatus.ACTIVE)
            throw new InvalidOperationException("Source account " + account.getAccountNumber()
                    + " is not ACTIVE (status: " + account.getStatus() + ")");
    }

    private void validateAccountForCredit(Account account) {
        if (account.getStatus() == Account.AccountStatus.CLOSED)
            throw new InvalidOperationException("Destination account " + account.getAccountNumber() + " is CLOSED");
    }

    private void validateSufficientBalance(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0)
            throw new InsufficientBalanceException(
                    "Insufficient balance in account " + account.getAccountNumber()
                    + ": available=" + account.getBalance() + ", required=" + amount);
    }

    private String generateReferenceNumber() {
        String date = Instant.now().atZone(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        long seq = refSequence.incrementAndGet();
        return String.format("TXN-%s-%06d", date, seq);
    }
}
