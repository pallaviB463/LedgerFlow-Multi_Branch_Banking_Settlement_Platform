package com.ledgerflow.controller;

import com.ledgerflow.dto.AccountDto;
import com.ledgerflow.dto.ApiResponse;
import com.ledgerflow.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST endpoints for account management. Base: /api/accounts */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AccountDto.Response>> createAccount(
            @Valid @RequestBody AccountDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Account created successfully", accountService.createAccount(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountDto.Response>>> getAllAccounts() {
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully", accountService.getAllAccounts()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountDto.Response>> getAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Account retrieved successfully", accountService.getAccountById(id)));
    }

    @GetMapping("/number/{accountNumber}")
    public ResponseEntity<ApiResponse<AccountDto.Response>> getAccountByNumber(@PathVariable String accountNumber) {
        return ResponseEntity.ok(ApiResponse.success("Account retrieved successfully", accountService.getAccountByNumber(accountNumber)));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<ApiResponse<List<AccountDto.Response>>> getAccountsByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(ApiResponse.success("Accounts retrieved successfully", accountService.getAccountsByBranch(branchId)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AccountDto.Response>> updateAccountStatus(
            @PathVariable Long id, @Valid @RequestBody AccountDto.UpdateStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Account status updated", accountService.updateAccountStatus(id, request)));
    }
}
