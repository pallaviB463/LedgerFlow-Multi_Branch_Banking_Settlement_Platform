package com.ledgerflow.service;

import com.ledgerflow.dto.ReportDto;
import com.ledgerflow.entity.Account;
import com.ledgerflow.entity.LedgerEntry;
import com.ledgerflow.exception.ResourceNotFoundException;
import com.ledgerflow.repository.AccountRepository;
import com.ledgerflow.repository.LedgerEntryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final JdbcTemplate jdbcTemplate;

    public ReportService(AccountRepository accountRepository, 
                         LedgerEntryRepository ledgerEntryRepository,
                         JdbcTemplate jdbcTemplate) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Queries the database SQL View 'v_trial_balance' directly.
     * Demonstrates Section 7: Advanced SQL Views.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTrialBalanceFromSqlView() {
        return jdbcTemplate.queryForList("SELECT * FROM v_trial_balance ORDER BY branch_name, account_number");
    }

    /**
     * Queries the database SQL View 'v_branch_settlement_summary' directly.
     * Demonstrates Section 7: Advanced SQL Views.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getBranchSummaryFromSqlView() {
        return jdbcTemplate.queryForList("SELECT * FROM v_branch_settlement_summary ORDER BY branch_name");
    }

    /**
     * Generates a Trial Balance report.
     * In a double-entry system, the sum of all accounts (assets - liabilities/equity) 
     * should theoretically sum to zero if everything is balanced.
     */
    @Transactional(readOnly = true)
    public ReportDto.TrialBalanceReport generateTrialBalance() {
        List<Account> allAccounts = accountRepository.findAll();
        
        BigDecimal totalSystemBalance = BigDecimal.ZERO;
        List<ReportDto.AccountBalance> accountBalances = allAccounts.stream().map(account -> {
            // Re-calculate the balance from the ledger to ensure 100% accuracy
            BigDecimal ledgerBalance = ledgerEntryRepository.calculateBalanceByAccountId(account.getId());
            return new ReportDto.AccountBalance(
                    account.getAccountNumber(), 
                    account.getAccountHolderName(), 
                    ledgerBalance
            );
        }).collect(Collectors.toList());

        for (ReportDto.AccountBalance ab : accountBalances) {
            totalSystemBalance = totalSystemBalance.add(ab.getBalance());
        }

        return new ReportDto.TrialBalanceReport(Instant.now(), totalSystemBalance, accountBalances);
    }

    /**
     * Generates a detailed Account Statement with chronological ledger entries.
     */
    @Transactional(readOnly = true)
    public ReportDto.AccountStatement generateAccountStatement(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountNumber));

        List<LedgerEntry> entries = ledgerEntryRepository.findByAccountIdOrderByPostedAtDesc(account.getId());
        
        List<ReportDto.StatementEntry> statementEntries = entries.stream().map(entry -> 
                new ReportDto.StatementEntry(
                        entry.getPostedAt(),
                        entry.getTransaction().getTransactionType().name(),
                        entry.getEntryType().name(),
                        entry.getAmount(),
                        entry.getRunningBalance(),
                        entry.getDescription(),
                        entry.getTransaction().getReferenceNumber()
                )
        ).collect(Collectors.toList());

        return new ReportDto.AccountStatement(
                account.getAccountNumber(),
                account.getAccountHolderName(),
                account.getBalance(),
                statementEntries
        );
    }
}
