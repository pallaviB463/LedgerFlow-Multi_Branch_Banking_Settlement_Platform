package com.ledgerflow.service;

import com.ledgerflow.dto.TransferDto;
import com.ledgerflow.entity.Account;
import com.ledgerflow.entity.AchBatch;
import com.ledgerflow.entity.AchTransfer;
import com.ledgerflow.entity.Branch;
import com.ledgerflow.exception.InvalidOperationException;
import com.ledgerflow.exception.ResourceNotFoundException;
import com.ledgerflow.repository.AccountRepository;
import com.ledgerflow.repository.AchBatchRepository;
import com.ledgerflow.repository.AchTransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AchSettlementEngine {

    private static final Logger log = LoggerFactory.getLogger(AchSettlementEngine.class);
    private static final String CLEARING_ACCOUNT_NUMBER = "ACH-CLEARING";

    private final AchBatchRepository achBatchRepository;
    private final AchTransferRepository achTransferRepository;
    private final AccountRepository accountRepository;
    private final TransferService transferService;
    private final AuditService auditService;

    public AchSettlementEngine(AchBatchRepository achBatchRepository,
                               AchTransferRepository achTransferRepository,
                               AccountRepository accountRepository,
                               TransferService transferService,
                               @Autowired(required = false) AuditService auditService) {
        this.achBatchRepository = achBatchRepository;
        this.achTransferRepository = achTransferRepository;
        this.accountRepository = accountRepository;
        this.transferService = transferService;
        this.auditService = auditService;
    }

    /**
     * SAGA STEP 2 & COMPENSATION.
     * Processes an entire ACH batch. Uses pessimistic locking to prevent concurrent processing.
     * Generates immutable NoSQL Clearing Receipts in MongoDB upon settlement.
     */
    @Transactional
    public void processBatch(Long batchId) {
        log.info("Starting settlement for ACH Batch {}", batchId);

        AchBatch batch = achBatchRepository.findByIdWithLock(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("ACH Batch not found with id: " + batchId));

        if (batch.getStatus() != AchBatch.BatchStatus.OPEN) {
            throw new InvalidOperationException("Cannot settle batch " + batchId + " because status is " + batch.getStatus());
        }

        batch.setStatus(AchBatch.BatchStatus.PROCESSING);
        achBatchRepository.save(batch);

        Account clearingAccount = accountRepository.findByAccountNumber(CLEARING_ACCOUNT_NUMBER)
                .orElseThrow(() -> new IllegalStateException("System clearing account not found: " + CLEARING_ACCOUNT_NUMBER));

        List<AchTransfer> transfers = achTransferRepository.findByBatchId(batchId);
        List<Map<String, Object>> settledReceipts = new ArrayList<>();

        for (AchTransfer transfer : transfers) {
            if (transfer.getStatus() != AchTransfer.TransferStatus.FUNDS_RESERVED) {
                log.info("Skipping transfer {} because status is {}", transfer.getId(), transfer.getStatus());
                continue;
            }

            try {
                // Saga Step 2: Settle from CLEARING -> Destination
                TransferDto.TransferRequest settleReq = new TransferDto.TransferRequest();
                settleReq.setSourceAccountId(clearingAccount.getId());
                settleReq.setDestinationAccountId(transfer.getDestinationAccount().getId());
                settleReq.setAmount(transfer.getAmount());
                settleReq.setDescription("ACH Settlement from " + transfer.getSourceAccount().getAccountNumber());
                
                transferService.executeTransfer(settleReq);
                
                transfer.setStatus(AchTransfer.TransferStatus.SETTLED);
                log.info("ACH Transfer {} SETTLED.", transfer.getId());

                settledReceipts.add(Map.of(
                        "transferId", transfer.getId(),
                        "source", transfer.getSourceAccount().getAccountNumber(),
                        "destination", transfer.getDestinationAccount().getAccountNumber(),
                        "amount", transfer.getAmount(),
                        "status", "SETTLED"
                ));
                
            } catch (Exception e) {
                log.warn("ACH Transfer {} failed to settle. Reason: {}. Initiating Compensation...", 
                        transfer.getId(), e.getMessage());
                
                try {
                    // Saga Compensation: Refund to source
                    TransferDto.TransferRequest refundReq = new TransferDto.TransferRequest();
                    refundReq.setSourceAccountId(clearingAccount.getId());
                    refundReq.setDestinationAccountId(transfer.getSourceAccount().getId());
                    refundReq.setAmount(transfer.getAmount());
                    refundReq.setDescription("ACH Compensation/Refund for failed transfer to " 
                            + transfer.getDestinationAccount().getAccountNumber());
                    
                    transferService.executeTransfer(refundReq);
                    
                    transfer.setStatus(AchTransfer.TransferStatus.COMPENSATED);
                    transfer.setErrorMessage(e.getMessage());
                    log.info("ACH Transfer {} COMPENSATED.", transfer.getId());

                    if (auditService != null) {
                        auditService.logFinancialEvent(
                                "SAGA_COMPENSATION",
                                "ACH-COMP-" + transfer.getId(),
                                clearingAccount.getAccountNumber(),
                                transfer.getSourceAccount().getAccountNumber(),
                                transfer.getAmount(),
                                "SYS000",
                                "SYSTEM",
                                "SAGA_ENGINE",
                                "WARNING",
                                Map.of("reason", e.getMessage(), "transferId", transfer.getId())
                        );
                    }
                    
                } catch (Exception compEx) {
                    log.error("CATASTROPHIC FAILURE: Compensation for transfer {} failed! Reason: {}", 
                            transfer.getId(), compEx.getMessage());
                    transfer.setStatus(AchTransfer.TransferStatus.FAILED);
                    transfer.setErrorMessage("Settle failed: " + e.getMessage() + " | Comp failed: " + compEx.getMessage());
                }
            }
            achTransferRepository.save(transfer);
        }

        batch.setStatus(AchBatch.BatchStatus.SETTLED);
        achBatchRepository.save(batch);

        // Calculate netting
        Map<String, BigDecimal> netting = calculateBranchNetting(batchId);

        // Store immutable NoSQL Clearing Receipt in MongoDB
        if (auditService != null) {
            auditService.storeClearingReceipt(
                    batch.getId(),
                    String.valueOf(batch.getId()),
                    batch.getStatus().name(),
                    batch.getTotalAmount(),
                    batch.getTransactionCount(),
                    settledReceipts,
                    netting
            );
        }

        log.info("Finished settlement for ACH Batch {}", batchId);
    }

    /**
     * Netting Algorithm.
     * Calculates the net inflow/outflow per branch for a given batch.
     * Netting is calculated ONLY for SETTLED transfers.
     */
    public Map<String, BigDecimal> calculateBranchNetting(Long batchId) {
        List<AchTransfer> transfers = achTransferRepository.findByBatchId(batchId);
        Map<String, BigDecimal> branchNetPositions = new HashMap<>();

        for (AchTransfer transfer : transfers) {
            if (transfer.getStatus() == AchTransfer.TransferStatus.SETTLED) {
                Branch sourceBranch = transfer.getSourceAccount().getBranch();
                Branch destBranch = transfer.getDestinationAccount().getBranch();

                String srcKey = sourceBranch.getBranchCode() + " (" + sourceBranch.getName() + ")";
                String dstKey = destBranch.getBranchCode() + " (" + destBranch.getName() + ")";

                // Source branch OUTFLOW (negative)
                branchNetPositions.put(srcKey, 
                        branchNetPositions.getOrDefault(srcKey, BigDecimal.ZERO).subtract(transfer.getAmount()));

                // Destination branch INFLOW (positive)
                branchNetPositions.put(dstKey, 
                        branchNetPositions.getOrDefault(dstKey, BigDecimal.ZERO).add(transfer.getAmount()));
            }
        }

        return branchNetPositions;
    }
}
