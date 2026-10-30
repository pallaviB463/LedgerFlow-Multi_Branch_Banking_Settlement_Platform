package com.ledgerflow.controller;

import com.ledgerflow.dto.ReportDto;
import com.ledgerflow.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Get the System Trial Balance (Application Level Aggregation).
     */
    @GetMapping("/trial-balance")
    public ResponseEntity<ReportDto.TrialBalanceReport> getTrialBalance() {
        ReportDto.TrialBalanceReport report = reportService.generateTrialBalance();
        return ResponseEntity.ok(report);
    }

    /**
     * Get the System Trial Balance directly from the Advanced SQL View 'v_trial_balance'.
     * Demonstrates DBMS Experiential Learning Section 7: Advanced SQL Views.
     */
    @GetMapping("/views/trial-balance")
    public ResponseEntity<List<Map<String, Object>>> getTrialBalanceFromSqlView() {
        return ResponseEntity.ok(reportService.getTrialBalanceFromSqlView());
    }

    /**
     * Get the Branch Settlement Summary directly from the Advanced SQL View 'v_branch_settlement_summary'.
     * Demonstrates DBMS Experiential Learning Section 7: Advanced SQL Views.
     */
    @GetMapping("/views/branch-summary")
    public ResponseEntity<List<Map<String, Object>>> getBranchSummaryFromSqlView() {
        return ResponseEntity.ok(reportService.getBranchSummaryFromSqlView());
    }

    /**
     * Get a chronological statement for an account.
     */
    @GetMapping("/statements/{accountNumber}")
    public ResponseEntity<ReportDto.AccountStatement> getAccountStatement(@PathVariable String accountNumber) {
        ReportDto.AccountStatement statement = reportService.generateAccountStatement(accountNumber);
        return ResponseEntity.ok(statement);
    }
}
