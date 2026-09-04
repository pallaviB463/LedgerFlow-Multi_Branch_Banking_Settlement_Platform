package com.ledgerflow.controller;

import com.ledgerflow.dto.ApiResponse;
import com.ledgerflow.dto.TransactionDto;
import com.ledgerflow.dto.TransferDto;
import com.ledgerflow.service.TransferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** REST endpoints for fund transfers, transaction history, and ledger queries. */
@RestController
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/api/transfers")
    public ResponseEntity<ApiResponse<TransferDto.TransferResponse>> executeTransfer(
            @Valid @RequestBody TransferDto.TransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transfer completed successfully", transferService.executeTransfer(request)));
    }

    @GetMapping("/api/transactions")
    public ResponseEntity<ApiResponse<List<TransactionDto>>> getAllTransactions() {
        List<TransactionDto> txns = transferService.getAllTransactions()
                .stream().map(TransactionDto::from).toList();
        return ResponseEntity.ok(ApiResponse.success("Transactions retrieved successfully", txns));
    }

    @GetMapping("/api/transactions/{id}")
    public ResponseEntity<ApiResponse<TransactionDto>> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Transaction retrieved successfully",
                TransactionDto.from(transferService.getTransactionById(id))));
    }

    @GetMapping("/api/ledger/accounts/{accountId}")
    public ResponseEntity<ApiResponse<TransferDto.AccountLedgerResponse>> getAccountLedger(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.success("Account ledger retrieved successfully",
                transferService.getAccountLedger(accountId)));
    }
}
