package com.ledgerflow.dto;

import com.ledgerflow.entity.Account;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/** DTOs for Account operations. */
public class AccountDto {

    /** Request body for POST /api/accounts */
    public static class CreateRequest {
        @NotBlank(message = "Account holder name is required")
        @Size(max = 150)
        private String accountHolderName;

        @NotNull(message = "Account type is required (SAVINGS or CURRENT)")
        private Account.AccountType accountType;

        @NotNull(message = "Initial balance is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Initial balance cannot be negative")
        @Digits(integer = 15, fraction = 4)
        private BigDecimal initialBalance;

        @NotNull(message = "Branch ID is required")
        @Positive(message = "Branch ID must be positive")
        private Long branchId;

        public CreateRequest() {}
        public String getAccountHolderName() { return accountHolderName; }
        public void setAccountHolderName(String v) { this.accountHolderName = v; }
        public Account.AccountType getAccountType() { return accountType; }
        public void setAccountType(Account.AccountType v) { this.accountType = v; }
        public BigDecimal getInitialBalance() { return initialBalance; }
        public void setInitialBalance(BigDecimal v) { this.initialBalance = v; }
        public Long getBranchId() { return branchId; }
        public void setBranchId(Long v) { this.branchId = v; }
    }

    /** Response body for account read operations. */
    public static class Response {
        private Long id;
        private String accountNumber;
        private String accountHolderName;
        private Account.AccountType accountType;
        private BigDecimal balance;
        private Account.AccountStatus status;
        private Long branchId;
        private String branchName;
        private String branchCode;
        private Instant createdAt;
        private Instant updatedAt;

        public Response() {}

        public static Response from(Account account) {
            Response r = new Response();
            r.id = account.getId();
            r.accountNumber = account.getAccountNumber();
            r.accountHolderName = account.getAccountHolderName();
            r.accountType = account.getAccountType();
            r.balance = account.getBalance();
            r.status = account.getStatus();
            r.branchId = account.getBranch().getId();
            r.branchName = account.getBranch().getName();
            r.branchCode = account.getBranch().getBranchCode();
            r.createdAt = account.getCreatedAt();
            r.updatedAt = account.getUpdatedAt();
            return r;
        }

        public Long getId() { return id; }
        public String getAccountNumber() { return accountNumber; }
        public String getAccountHolderName() { return accountHolderName; }
        public Account.AccountType getAccountType() { return accountType; }
        public BigDecimal getBalance() { return balance; }
        public Account.AccountStatus getStatus() { return status; }
        public Long getBranchId() { return branchId; }
        public String getBranchName() { return branchName; }
        public String getBranchCode() { return branchCode; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getUpdatedAt() { return updatedAt; }
    }

    /** Request body for PATCH /api/accounts/{id}/status */
    public static class UpdateStatusRequest {
        @NotNull(message = "Status is required (ACTIVE, FROZEN, or CLOSED)")
        private Account.AccountStatus status;

        public UpdateStatusRequest() {}
        public Account.AccountStatus getStatus() { return status; }
        public void setStatus(Account.AccountStatus status) { this.status = status; }
    }
}
