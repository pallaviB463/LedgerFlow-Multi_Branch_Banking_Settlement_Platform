package com.ledgerflow.controller;

import com.ledgerflow.dto.ReportDto;
import com.ledgerflow.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Get the System Trial Balance.
     */
    @GetMapping("/trial-balance")
    public ResponseEntity<ReportDto.TrialBalanceReport> getTrialBalance() {
        ReportDto.TrialBalanceReport report = reportService.generateTrialBalance();
        return ResponseEntity.ok(report);
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
