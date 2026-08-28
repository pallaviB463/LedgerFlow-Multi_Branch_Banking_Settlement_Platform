package com.ledgerflow.controller;

import com.ledgerflow.dto.AccountDto;
import com.ledgerflow.dto.ApiResponse;
import com.ledgerflow.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AccountController — REST API endpoints for account management.
 *
 * Base path: /api/accounts
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * POST /api/accounts
     * Create a new bank account.
     *
     * Request body example:
     * {
     *   "accountHolderName": "John Doe",
     *   "accountType": "SAVINGS",
     *   "initialBalance": 5000.00,
     *   "branchId": 1
     * }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AccountDto.Response>> createAccount(
            @Valid @RequestBody AccountDto.CreateRequest request) {
        AccountDto.Response account = accountService.createAccount(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", account));
    }

    /**
     * GET /api/accounts
     * List all accounts across all branches.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountDto.Response>>> getAllAccounts() {
        List<AccountDto.Response> accounts = accountService.getAllAccounts();
        return ResponseEntity.ok(
                ApiResponse.success("Accounts retrieved successfully", accounts));
    }

    /**
     * GET /api/accounts/{id}
     * Get a single account by its database ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountDto.Response>> getAccountById(
            @PathVariable Long id) {
        AccountDto.Response account = accountService.getAccountById(id);
        return ResponseEntity.ok(
                ApiResponse.success("Account retrieved successfully", account));
    }

    /**
     * GET /api/accounts/number/{accountNumber}
     * Get an account by its account number (e.g., "ACC-LFLW001-20260828-10001").
     */
    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountDto.Response>> getAccountByNumber(
            @PathVariable String accountNumber) {
        AccountDto.Response account = accountService.getAccountByNumber(accountNumber);
        return ResponseEntity.ok(
                ApiResponse.success("Account retrieved successfully", account));
    }

    /**
     * GET /api/accounts/branch/{branchId}
     * Get all accounts belonging to a specific branch.
     */
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<List<AccountDto.Response>>> getAccountsByBranch(
            @PathVariable Long branchId) {
        List<AccountDto.Response> accounts = accountService.getAccountsByBranch(branchId);
        return ResponseEntity.ok(
                ApiResponse.success("Accounts for branch retrieved successfully", accounts));
    }

    /**
     * PATCH /api/accounts/{id}/status
     * Update account status (ACTIVE / FROZEN / CLOSED).
     *
     * Request body:
     * { "status": "FROZEN" }
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AccountDto.Response>> updateAccountStatus(
            @PathVariable Long id,
            @Valid @RequestBody AccountDto.UpdateStatusRequest request) {
        AccountDto.Response account = accountService.updateAccountStatus(id, request);
        return ResponseEntity.ok(
                ApiResponse.success("Account status updated successfully", account));
    }
}
