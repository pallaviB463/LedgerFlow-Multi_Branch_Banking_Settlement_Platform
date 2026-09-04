package com.ledgerflow.service;

import com.ledgerflow.dto.TransferDto;
import com.ledgerflow.entity.Account;
import com.ledgerflow.entity.LedgerEntry;
import com.ledgerflow.entity.Transaction;
import com.ledgerflow.repository.LedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * LedgerService — the double-entry bookkeeping engine.
 *
 * postDoubleEntry() is the ONLY method in the codebase that creates
 * ledger entries. It uses Propagation.MANDATORY to ensure it always
 * runs inside an existing transaction from the caller (TransferService).
 */
@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);
    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * Post a balanced pair of ledger entries: one DEBIT, one CREDIT.
     * MUST be called within an existing transaction (Propagation.MANDATORY).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void postDoubleEntry(Account debitAccount, Account creditAccount,
                                BigDecimal amount, Transaction transaction,
                                String description) {

        // DEBIT entry: money leaving the source account
        BigDecimal debitRunningBalance = debitAccount.getBalance().subtract(amount);
        LedgerEntry debitEntry = new LedgerEntry(
                debitAccount, transaction, LedgerEntry.EntryType.DEBIT,
                amount, debitRunningBalance, description);
        ledgerEntryRepository.save(debitEntry);
        debitAccount.setBalance(debitRunningBalance);

        // CREDIT entry: money arriving at the destination account
        BigDecimal creditRunningBalance = creditAccount.getBalance().add(amount);
        LedgerEntry creditEntry = new LedgerEntry(
                creditAccount, transaction, LedgerEntry.EntryType.CREDIT,
                amount, creditRunningBalance, description);
        ledgerEntryRepository.save(creditEntry);
        creditAccount.setBalance(creditRunningBalance);

        log.debug("Double-entry posted: DEBIT {} ={} | CREDIT {} ={}",
                debitAccount.getAccountNumber(), debitRunningBalance,
                creditAccount.getAccountNumber(), creditRunningBalance);

        // Invariant check
        if (debitEntry.getAmount().compareTo(creditEntry.getAmount()) != 0) {
            throw new IllegalStateException("Double-entry invariant violated: debit="
                    + debitEntry.getAmount() + " != credit=" + creditEntry.getAmount());
        }
    }

    /** Get the full ledger history for an account, newest first. */
    @Transactional(readOnly = true)
    public List<TransferDto.LedgerEntryView> getAccountLedger(Long accountId) {
        return ledgerEntryRepository.findByAccountIdOrderByPostedAtDesc(accountId)
                .stream()
                .map(entry -> {
                    TransferDto.LedgerEntryView view = new TransferDto.LedgerEntryView();
                    view.setLedgerEntryId(entry.getId());
                    view.setTransactionId(entry.getTransaction().getId());
                    view.setReferenceNumber(entry.getTransaction().getReferenceNumber());
                    view.setEntryType(entry.getEntryType().name());
                    view.setAmount(entry.getAmount());
                    view.setRunningBalance(entry.getRunningBalance());
                    view.setDescription(entry.getDescription());
                    view.setPostedAt(entry.getPostedAt());
                    return view;
                })
                .toList();
    }

    /** Recalculate true balance from the immutable ledger. */
    @Transactional(readOnly = true)
    public BigDecimal calculateLedgerBalance(Long accountId) {
        BigDecimal balance = ledgerEntryRepository.calculateBalanceByAccountId(accountId);
        return balance != null ? balance : BigDecimal.ZERO;
    }
}
