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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    public AchSettlementEngine(AchBatchRepository achBatchRepository,
                               AchTransferRepository achTransferRepository,
                               AccountRepository accountRepository,
                               TransferService transferService) {
        this.achBatchRepository = achBatchRepository;
        this.achTransferRepository = achTransferRepository;
        this.accountRepository = accountRepository;
        this.transferService = transferService;
    }

    /**
     * SAGA STEP 2 & COMPENSATION.
     * Processes an entire ACH batch. Uses pessimistic locking to prevent concurrent processing.
     */
    @Transactional
    public void processBatch(Long batchId) {
        log.info("Starting settlement for ACH Batch {}", batchId);

        // 1. Lock the batch
        AchBatch batch = achBatchRepository.findByIdWithLock(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found"));

        if (batch.getStatus() != AchBatch.BatchStatus.OPEN) {
            throw new InvalidOperationException("Can only process OPEN batches");
        }

        batch.setStatus(AchBatch.BatchStatus.PROCESSING);
        achBatchRepository.save(batch);

        Account clearingAccount = accountRepository.findByAccountNumber(CLEARING_ACCOUNT_NUMBER)
                .orElseThrow(() -> new ResourceNotFoundException("ACH Clearing account not found"));

        List<AchTransfer> transfers = achTransferRepository.findByBatchId(batchId);
        
        for (AchTransfer transfer : transfers) {
            if (transfer.getStatus() != AchTransfer.TransferStatus.FUNDS_RESERVED) {
                continue; // Skip already processed or failed
            }

            try {
                // Saga Step 2: Try to settle to destination
                if (transfer.getDestinationAccount().getStatus() != Account.AccountStatus.ACTIVE) {
                    throw new InvalidOperationException("Destination account is not ACTIVE");
                }

                TransferDto.TransferRequest settleReq = new TransferDto.TransferRequest();
                settleReq.setSourceAccountId(clearingAccount.getId());
                settleReq.setDestinationAccountId(transfer.getDestinationAccount().getId());
                settleReq.setAmount(transfer.getAmount());
                settleReq.setDescription("ACH Settlement from " + transfer.getSourceAccount().getAccountNumber());
                
                transferService.executeTransfer(settleReq);
                
                transfer.setStatus(AchTransfer.TransferStatus.SETTLED);
                log.info("ACH Transfer {} SETTLED.", transfer.getId());
                
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
                    
                } catch (Exception compEx) {
                    // This is a catastrophic failure (e.g. source account is also closed)
                    // In a real system, this requires manual ops intervention.
                    log.error("CATASTROPHIC FAILURE: Compensation for transfer {} failed! Funds are trapped in CLEARING. Reason: {}", 
                            transfer.getId(), compEx.getMessage());
                    transfer.setStatus(AchTransfer.TransferStatus.FAILED);
                    transfer.setErrorMessage("Settle failed: " + e.getMessage() + " | Comp failed: " + compEx.getMessage());
                }
            }
            achTransferRepository.save(transfer);
        }

        batch.setStatus(AchBatch.BatchStatus.SETTLED);
        achBatchRepository.save(batch);
        log.info("Finished settlement for ACH Batch {}", batchId);
    }

    /**
     * Netting Algorithm.
     * Calculates the net inflow/outflow per branch for a given batch.
     * Netting is calculated ONLY for SETTLED transfers.
     */
    @Transactional(readOnly = true)
    public Map<String, BigDecimal> calculateBranchNetting(Long batchId) {
        List<AchTransfer> transfers = achTransferRepository.findByBatchId(batchId);
        
        // Map of BranchCode -> Net Amount
        // Positive means the branch receives money (inflow > outflow)
        // Negative means the branch owes money (outflow > inflow)
        Map<String, BigDecimal> netting = new HashMap<>();

        for (AchTransfer t : transfers) {
            if (t.getStatus() != AchTransfer.TransferStatus.SETTLED) {
                continue;
            }

            Branch srcBranch = t.getSourceAccount().getBranch();
            Branch destBranch = t.getDestinationAccount().getBranch();
            
            String srcCode = srcBranch.getBranchCode() + " (" + srcBranch.getName() + ")";
            String destCode = destBranch.getBranchCode() + " (" + destBranch.getName() + ")";

            // Decrease net for source branch
            netting.put(srcCode, netting.getOrDefault(srcCode, BigDecimal.ZERO).subtract(t.getAmount()));
            // Increase net for dest branch
            netting.put(destCode, netting.getOrDefault(destCode, BigDecimal.ZERO).add(t.getAmount()));
        }

        return netting;
    }
}
