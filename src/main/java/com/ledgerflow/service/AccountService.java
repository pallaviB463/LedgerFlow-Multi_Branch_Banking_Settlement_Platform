package com.ledgerflow.service;

import com.ledgerflow.dto.AccountDto;
import com.ledgerflow.entity.Account;
import com.ledgerflow.entity.Branch;
import com.ledgerflow.exception.DuplicateResourceException;
import com.ledgerflow.exception.InvalidOperationException;
import com.ledgerflow.exception.ResourceNotFoundException;
import com.ledgerflow.repository.AccountRepository;
import com.ledgerflow.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AccountService — business logic for account management.
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final BranchRepository branchRepository;

    // Sequence counter for generating account numbers.
    // In production this would be a database sequence.
    private final AtomicLong accountSequence = new AtomicLong(10000);

    /**
     * Create a new bank account.
     *
     * Business rules:
     *   - The specified branch must exist and be active
     *   - Account number is auto-generated
     *
     * @param request the account creation request
     * @return the created account as a Response DTO
     */
    @Transactional
    public AccountDto.Response createAccount(AccountDto.CreateRequest request) {
        // Validate branch exists and is active
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Branch not found with id: " + request.getBranchId()));

        if (!branch.isActive()) {
            throw new InvalidOperationException(
                    "Cannot create account in inactive branch: " + branch.getName());
        }

        // Generate unique account number
        String accountNumber = generateAccountNumber(branch);

        // Guard against any collision (extremely rare but safe to check)
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new DuplicateResourceException(
                    "Account number collision: " + accountNumber + ". Please retry.");
        }

        Account account = new Account(
                accountNumber,
                request.getAccountHolderName(),
                request.getAccountType(),
                request.getInitialBalance(),
                branch
        );

        Account saved = accountRepository.save(account);
        log.info("Created account {} for {} in branch {}",
                saved.getAccountNumber(), saved.getAccountHolderName(), branch.getName());
        return AccountDto.Response.from(saved);
    }

    /**
     * Get all accounts across all branches.
     */
    @Transactional(readOnly = true)
    public List<AccountDto.Response> getAllAccounts() {
        return accountRepository.findAll()
                .stream()
                .map(AccountDto.Response::from)
                .toList();
    }

    /**
     * Get a single account by its database ID.
     */
    @Transactional(readOnly = true)
    public AccountDto.Response getAccountById(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with id: " + id));
        return AccountDto.Response.from(account);
    }

    /**
     * Get a single account by its account number (e.g., "ACC-10001").
     */
    @Transactional(readOnly = true)
    public AccountDto.Response getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with number: " + accountNumber));
        return AccountDto.Response.from(account);
    }

    /**
     * Get all accounts belonging to a specific branch.
     */
    @Transactional(readOnly = true)
    public List<AccountDto.Response> getAccountsByBranch(Long branchId) {
        // Validate branch exists first
        if (!branchRepository.existsById(branchId)) {
            throw new ResourceNotFoundException("Branch not found with id: " + branchId);
        }
        return accountRepository.findByBranchId(branchId)
                .stream()
                .map(AccountDto.Response::from)
                .toList();
    }

    /**
     * Update the status of an account (ACTIVE / FROZEN / CLOSED).
     *
     * Business rules:
     *   - A CLOSED account cannot be reopened
     */
    @Transactional
    public AccountDto.Response updateAccountStatus(Long id, AccountDto.UpdateStatusRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Account not found with id: " + id));

        // Cannot reopen a closed account
        if (account.getStatus() == Account.AccountStatus.CLOSED
                && request.getStatus() != Account.AccountStatus.CLOSED) {
            throw new InvalidOperationException(
                    "Account " + account.getAccountNumber() + " is CLOSED and cannot be reopened");
        }

        Account.AccountStatus oldStatus = account.getStatus();
        account.setStatus(request.getStatus());
        Account saved = accountRepository.save(account);

        log.info("Account {} status changed: {} → {}",
                saved.getAccountNumber(), oldStatus, request.getStatus());
        return AccountDto.Response.from(saved);
    }

    /**
     * Generates a unique account number using the branch code + date + sequence.
     * Format: ACC-LFLW001-20260828-10001
     * This is deterministic within a JVM session. In production, use a DB sequence.
     */
    private String generateAccountNumber(Branch branch) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long seq = accountSequence.incrementAndGet();
        return "ACC-" + branch.getBranchCode() + "-" + date + "-" + seq;
    }
}
