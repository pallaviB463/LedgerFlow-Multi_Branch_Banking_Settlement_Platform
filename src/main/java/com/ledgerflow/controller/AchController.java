package com.ledgerflow.controller;

import com.ledgerflow.dto.AchDto;
import com.ledgerflow.entity.AchBatch;
import com.ledgerflow.entity.AchTransfer;
import com.ledgerflow.repository.AchBatchRepository;
import com.ledgerflow.service.AchService;
import com.ledgerflow.service.AchSettlementEngine;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/ach")
public class AchController {

    private final AchService achService;
    private final AchSettlementEngine achSettlementEngine;
    private final AchBatchRepository achBatchRepository;

    public AchController(AchService achService,
                         AchSettlementEngine achSettlementEngine,
                         AchBatchRepository achBatchRepository) {
        this.achService = achService;
        this.achSettlementEngine = achSettlementEngine;
        this.achBatchRepository = achBatchRepository;
    }

    @PostMapping("/transfers")
    public ResponseEntity<AchDto.AchTransferResponse> submitTransfer(@Valid @RequestBody AchDto.AchTransferRequest request) {
        AchTransfer transfer = achService.submitTransfer(
                request.getSourceAccountId(),
                request.getDestinationAccountId(),
                request.getAmount()
        );

        AchDto.AchTransferResponse resp = new AchDto.AchTransferResponse();
        resp.setTransferId(transfer.getId());
        resp.setBatchId(transfer.getBatch().getId());
        resp.setStatus(transfer.getStatus().name());
        resp.setAmount(transfer.getAmount());
        resp.setMessage("ACH Transfer submitted and funds reserved.");

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/batches/current/settle")
    public ResponseEntity<String> settleCurrentBatch() {
        AchBatch batch = achBatchRepository.findFirstByStatusOrderByCreatedAtAsc(AchBatch.BatchStatus.OPEN)
                .orElse(null);
                
        if (batch == null) {
            return ResponseEntity.ok("No OPEN batches to settle.");
        }

        achSettlementEngine.processBatch(batch.getId());
        return ResponseEntity.ok("Batch " + batch.getId() + " settled successfully.");
    }

    @GetMapping("/batches/{batchId}/netting")
    public ResponseEntity<Map<String, BigDecimal>> getBatchNetting(@PathVariable Long batchId) {
        Map<String, BigDecimal> netting = achSettlementEngine.calculateBranchNetting(batchId);
        return ResponseEntity.ok(netting);
    }
}
