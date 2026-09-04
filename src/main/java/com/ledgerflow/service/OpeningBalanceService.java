package com.ledgerflow.service;

import com.ledgerflow.entity.Account;
import com.ledgerflow.entity.LedgerEntry;
import com.ledgerflow.entity.Transaction;
import com.ledgerflow.repository.LedgerEntryRepository;
import com.ledgerflow.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Posts an opening CREDIT ledger entry when a new account is funded.
 *
 * In a real core-banking system, every penny in every account must be traceable
 * to a ledger entry. An initial deposit (opening balance) is modelled as a
 * DEPOSIT transaction: money comes from an external source ("EXTERNAL_CASH")
 * and is CREDITED into the new account.
 *
 * This ensures that calculateBalanceByAccountId() always matches the
 * stored account.balance field — a key integrity invariant.
 *
 * Propagation.MANDATORY: must be called from within an existing transaction.
 */
@Service
public class OpeningBalanceService {

    private static final Logger log = LoggerFactory.getLogger(OpeningBalanceService.class);

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    public OpeningBalanceService(TransactionRepository transactionRepository,
                                 LedgerEntryRepository ledgerEntryRepository) {
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * Post the opening balance as a DEPOSIT transaction + CREDIT ledger entry.
     * Only called if initialBalance > 0.
     *
     * Uses REQUIRED (not MANDATORY) so it can be called both from within
     * AccountService's transaction AND standalone in tests.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void postOpeningBalance(Account account, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return; // Nothing to post for a zero-balance account
        }

        // Create an OPENING DEPOSIT transaction record
        Transaction txn = new Transaction();
        txn.setTransactionType(Transaction.TransactionType.DEPOSIT);
        txn.setAmount(amount);
        txn.setStatus(Transaction.TransactionStatus.COMPLETED);
        txn.setDescription("Opening balance deposit");
        txn.setReferenceNumber("OPEN-" + account.getAccountNumber());
        txn.setCompletedAt(Instant.now());
        Transaction savedTxn = transactionRepository.save(txn);

        // Post a CREDIT entry: money arrives into the account
        LedgerEntry credit = new LedgerEntry(
                account,
                savedTxn,
                LedgerEntry.EntryType.CREDIT,
                amount,
                amount,  // Running balance after the opening credit = the amount itself
                "Opening balance deposit"
        );
        ledgerEntryRepository.save(credit);

        log.info("Opening balance posted: account={}, amount={}",
                account.getAccountNumber(), amount);
    }
}
