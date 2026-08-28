package com.ledgerflow.service;

import com.ledgerflow.dto.AccountDto;
import com.ledgerflow.entity.Account;
import com.ledgerflow.entity.Branch;
import com.ledgerflow.exception.DuplicateResourceException;
import com.ledgerflow.exception.InvalidOperationException;
import com.ledgerflow.exception.ResourceNotFoundException;
import com.ledgerflow.repository.AccountRepository;
import com.ledgerflow.repository.BranchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** Business logic for account management. */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private final AccountRepository accountRepository;
    private final BranchRepository branchRepository;
    private final AtomicLong accountSequence = new AtomicLong(10000);

    public AccountService(AccountRepository accountRepository, BranchRepository branchRepository) {
        this.accountRepository = accountRepository;
        this.branchRepository = branchRepository;
    }

    @Transactional
    public AccountDto.Response createAccount(AccountDto.CreateRequest request) {
        Branch branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Branch not found with id: " + request.getBranchId()));
        if (!branch.isActive())
            throw new InvalidOperationException("Cannot create account in inactive branch: " + branch.getName());

        String accountNumber = generateAccountNumber(branch);
        if (accountRepository.existsByAccountNumber(accountNumber))
            throw new DuplicateResourceException("Account number collision: " + accountNumber);

        Account account = new Account(accountNumber, request.getAccountHolderName(),
                request.getAccountType(), request.getInitialBalance(), branch);
        Account saved = accountRepository.save(account);
        log.info("Created account {} for {} in branch {}", saved.getAccountNumber(), saved.getAccountHolderName(), branch.getName());
        return AccountDto.Response.from(saved);
    }

    @Transactional(readOnly = true)
    public List<AccountDto.Response> getAllAccounts() {
        return accountRepository.findAll().stream().map(AccountDto.Response::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountDto.Response getAccountById(Long id) {
        return AccountDto.Response.from(accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id)));
    }

    @Transactional(readOnly = true)
    public AccountDto.Response getAccountByNumber(String accountNumber) {
        return AccountDto.Response.from(accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with number: " + accountNumber)));
    }

    @Transactional(readOnly = true)
    public List<AccountDto.Response> getAccountsByBranch(Long branchId) {
        if (!branchRepository.existsById(branchId))
            throw new ResourceNotFoundException("Branch not found with id: " + branchId);
        return accountRepository.findByBranchId(branchId).stream().map(AccountDto.Response::from).toList();
    }

    @Transactional
    public AccountDto.Response updateAccountStatus(Long id, AccountDto.UpdateStatusRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + id));
        if (account.getStatus() == Account.AccountStatus.CLOSED
                && request.getStatus() != Account.AccountStatus.CLOSED)
            throw new InvalidOperationException("Account " + account.getAccountNumber() + " is CLOSED and cannot be reopened");
        account.setStatus(request.getStatus());
        return AccountDto.Response.from(accountRepository.save(account));
    }

    private String generateAccountNumber(Branch branch) {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long seq = accountSequence.incrementAndGet();
        return "ACC-" + branch.getBranchCode() + "-" + date + "-" + seq;
    }
}
