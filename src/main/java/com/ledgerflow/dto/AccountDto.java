package com.ledgerflow.dto;

import com.ledgerflow.entity.Account;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Data Transfer Objects for Account.
 */
public class AccountDto {

    /**
     * Request DTO: used when creating a new account (POST /api/accounts).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        @NotBlank(message = "Account holder name is required")
        @Size(max = 150, message = "Account holder name must not exceed 150 characters")
        private String accountHolderName;

        @NotNull(message = "Account type is required (SAVINGS or CURRENT)")
        private Account.AccountType accountType;

        @NotNull(message = "Initial balance is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Initial balance cannot be negative")
        @Digits(integer = 15, fraction = 4, message = "Invalid balance format")
        private BigDecimal initialBalance;

        @NotNull(message = "Branch ID is required")
        @Positive(message = "Branch ID must be a positive number")
        private Long branchId;
    }

    /**
     * Response DTO: returned when reading account data.
     * Note: we include branch info inline so callers don't need a second request.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
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

        /** Convert an Account entity to a Response DTO. */
        public static Response from(Account account) {
            return Response.builder()
                    .id(account.getId())
                    .accountNumber(account.getAccountNumber())
                    .accountHolderName(account.getAccountHolderName())
                    .accountType(account.getAccountType())
                    .balance(account.getBalance())
                    .status(account.getStatus())
                    .branchId(account.getBranch().getId())
                    .branchName(account.getBranch().getName())
                    .branchCode(account.getBranch().getBranchCode())
                    .createdAt(account.getCreatedAt())
                    .updatedAt(account.getUpdatedAt())
                    .build();
        }
    }

    /**
     * Request DTO: used when updating account status (PATCH /api/accounts/{id}/status).
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        @NotNull(message = "Status is required (ACTIVE, FROZEN, or CLOSED)")
        private Account.AccountStatus status;
    }
}
