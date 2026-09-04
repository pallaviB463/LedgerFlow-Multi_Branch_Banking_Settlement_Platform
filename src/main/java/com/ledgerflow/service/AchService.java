package com.ledgerflow.service;

import com.ledgerflow.dto.TransferDto;
import com.ledgerflow.entity.Account;
import com.ledgerflow.entity.AchBatch;
import com.ledgerflow.entity.AchTransfer;
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

@Service
public class AchService {

    private static final Logger log = LoggerFactory.getLogger(AchService.class);
    private static final String CLEARING_ACCOUNT_NUMBER = "ACH-CLEARING";

    private final AchBatchRepository achBatchRepository;
    private final AchTransferRepository achTransferRepository;
    private final AccountRepository accountRepository;
    private final TransferService transferService;

    public AchService(AchBatchRepository achBatchRepository,
                      AchTransferRepository achTransferRepository,
                      AccountRepository accountRepository,
                      TransferService transferService) {
        this.achBatchRepository = achBatchRepository;
        this.achTransferRepository = achTransferRepository;
        this.accountRepository = accountRepository;
        this.transferService = transferService;
    }

    /**
     * SAGA STEP 1: Initiation and Funds Reservation.
     * Submits an ACH transfer to the currently OPEN batch.
     * This immediately debits the sender and credits the central clearing account.
     */
    @Transactional
    public AchTransfer submitTransfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount) {
        log.info("Submitting ACH transfer: {} -> {}, amount={}", sourceAccountId, destinationAccountId, amount);

        if (sourceAccountId.equals(destinationAccountId)) {
            throw new InvalidOperationException("Source and destination accounts must be different");
        }

        // Get accounts to validate they exist before we start transferring
        Account sourceAccount = accountRepository.findById(sourceAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Source account not found"));
        Account destAccount = accountRepository.findById(destinationAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Destination account not found"));
        Account clearingAccount = accountRepository.findByAccountNumber(CLEARING_ACCOUNT_NUMBER)
                .orElseThrow(() -> new ResourceNotFoundException("ACH Clearing account not found"));

        // 1. Get or create OPEN batch
        AchBatch batch = achBatchRepository.findFirstByStatusOrderByCreatedAtAsc(AchBatch.BatchStatus.OPEN)
                .orElseGet(() -> {
                    AchBatch newBatch = new AchBatch();
                    return achBatchRepository.save(newBatch);
                });

        // 2. Reserve funds (Saga Step 1): Transfer from Source to ACH Clearing
        TransferDto.TransferRequest reserveReq = new TransferDto.TransferRequest();
        reserveReq.setSourceAccountId(sourceAccount.getId());
        reserveReq.setDestinationAccountId(clearingAccount.getId());
        reserveReq.setAmount(amount);
        reserveReq.setDescription("ACH Reserve for " + destAccount.getAccountNumber());
        
        // This will throw InsufficientBalanceException if they don't have the money.
        transferService.executeTransfer(reserveReq);

        // 3. Create the AchTransfer record
        AchTransfer achTransfer = new AchTransfer(batch, sourceAccount, destAccount, amount);
        achTransfer.setStatus(AchTransfer.TransferStatus.FUNDS_RESERVED);
        
        achTransfer = achTransferRepository.save(achTransfer);
        batch.addTransfer(achTransfer);
        achBatchRepository.save(batch);

        log.info("ACH transfer reserved successfully. Batch={}, TransferId={}", batch.getId(), achTransfer.getId());
        return achTransfer;
    }
}
