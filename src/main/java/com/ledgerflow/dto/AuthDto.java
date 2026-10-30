package com.ledgerflow.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Authentication and RBAC Data Transfer Objects.
 */
public class AuthDto {

    public static class LoginRequest {
        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;

        public LoginRequest() {}

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class AuthResponse {
        private String token;
        private String tokenType = "Bearer";
        private String username;
        private String displayName;
        private String role;
        private String accountNumber;
        private String branchCode;

        public AuthResponse() {}

        public AuthResponse(String token, String username, String displayName,
                            String role, String accountNumber, String branchCode) {
            this.token = token;
            this.username = username;
            this.displayName = displayName;
            this.role = role;
            this.accountNumber = accountNumber;
            this.branchCode = branchCode;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getTokenType() { return tokenType; }
        public void setTokenType(String tokenType) { this.tokenType = tokenType; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getAccountNumber() { return accountNumber; }
        public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
        public String getBranchCode() { return branchCode; }
        public void setBranchCode(String branchCode) { this.branchCode = branchCode; }
    }

    public static class DemoUserDto {
        private String username;
        private String password;
        private String role;
        private String displayName;
        private String description;
        private String accountNumber;

        public DemoUserDto(String username, String password, String role,
                           String displayName, String description, String accountNumber) {
            this.username = username;
            this.password = password;
            this.role = role;
            this.displayName = displayName;
            this.description = description;
            this.accountNumber = accountNumber;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getRole() { return role; }
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public String getAccountNumber() { return accountNumber; }
    }
}

