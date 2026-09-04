package com.ledgerflow.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class ReportDto {

    public static class TrialBalanceReport {
        private Instant generatedAt;
        private BigDecimal totalSystemBalance;
        private List<AccountBalance> accounts;

        public TrialBalanceReport() {}

        public TrialBalanceReport(Instant generatedAt, BigDecimal totalSystemBalance, List<AccountBalance> accounts) {
            this.generatedAt = generatedAt;
            this.totalSystemBalance = totalSystemBalance;
            this.accounts = accounts;
        }

        public Instant getGeneratedAt() {
            return generatedAt;
        }

        public void setGeneratedAt(Instant generatedAt) {
            this.generatedAt = generatedAt;
        }

        public BigDecimal getTotalSystemBalance() {
            return totalSystemBalance;
        }

        public void setTotalSystemBalance(BigDecimal totalSystemBalance) {
            this.totalSystemBalance = totalSystemBalance;
        }

        public List<AccountBalance> getAccounts() {
            return accounts;
        }

        public void setAccounts(List<AccountBalance> accounts) {
            this.accounts = accounts;
        }
    }

    public static class AccountBalance {
        private String accountNumber;
        private String accountName;
        private BigDecimal balance;

        public AccountBalance() {}

        public AccountBalance(String accountNumber, String accountName, BigDecimal balance) {
            this.accountNumber = accountNumber;
            this.accountName = accountName;
            this.balance = balance;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }
    }

    public static class AccountStatement {
        private String accountNumber;
        private String accountName;
        private BigDecimal currentBalance;
        private List<StatementEntry> entries;

        public AccountStatement() {}

        public AccountStatement(String accountNumber, String accountName, BigDecimal currentBalance, List<StatementEntry> entries) {
            this.accountNumber = accountNumber;
            this.accountName = accountName;
            this.currentBalance = currentBalance;
            this.entries = entries;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public String getAccountName() {
            return accountName;
        }

        public void setAccountName(String accountName) {
            this.accountName = accountName;
        }

        public BigDecimal getCurrentBalance() {
            return currentBalance;
        }

        public void setCurrentBalance(BigDecimal currentBalance) {
            this.currentBalance = currentBalance;
        }

        public List<StatementEntry> getEntries() {
            return entries;
        }

        public void setEntries(List<StatementEntry> entries) {
            this.entries = entries;
        }
    }

    public static class StatementEntry {
        private Instant postedAt;
        private String transactionType;
        private String entryType;
        private BigDecimal amount;
        private BigDecimal runningBalance;
        private String description;
        private String referenceNumber;

        public StatementEntry() {}

        public StatementEntry(Instant postedAt, String transactionType, String entryType, BigDecimal amount, BigDecimal runningBalance, String description, String referenceNumber) {
            this.postedAt = postedAt;
            this.transactionType = transactionType;
            this.entryType = entryType;
            this.amount = amount;
            this.runningBalance = runningBalance;
            this.description = description;
            this.referenceNumber = referenceNumber;
        }

        public Instant getPostedAt() {
            return postedAt;
        }

        public void setPostedAt(Instant postedAt) {
            this.postedAt = postedAt;
        }

        public String getTransactionType() {
            return transactionType;
        }

        public void setTransactionType(String transactionType) {
            this.transactionType = transactionType;
        }

        public String getEntryType() {
            return entryType;
        }

        public void setEntryType(String entryType) {
            this.entryType = entryType;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public BigDecimal getRunningBalance() {
            return runningBalance;
        }

        public void setRunningBalance(BigDecimal runningBalance) {
            this.runningBalance = runningBalance;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getReferenceNumber() {
            return referenceNumber;
        }

        public void setReferenceNumber(String referenceNumber) {
            this.referenceNumber = referenceNumber;
        }
    }
}
