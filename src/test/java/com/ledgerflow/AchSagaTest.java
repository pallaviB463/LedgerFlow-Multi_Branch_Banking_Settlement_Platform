package com.ledgerflow;

import com.ledgerflow.entity.Account;
import com.ledgerflow.entity.AchBatch;
import com.ledgerflow.entity.AchTransfer;
import com.ledgerflow.entity.Branch;
import com.ledgerflow.repository.AccountRepository;
import com.ledgerflow.repository.AchBatchRepository;
import com.ledgerflow.repository.AchTransferRepository;
import com.ledgerflow.repository.BranchRepository;
import com.ledgerflow.service.AchService;
import com.ledgerflow.service.AchSettlementEngine;
import com.ledgerflow.service.OpeningBalanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class AchSagaTest {

    @Autowired private AchService achService;
    @Autowired private AchSettlementEngine achSettlementEngine;
    @Autowired private AchBatchRepository achBatchRepository;
    @Autowired private AchTransferRepository achTransferRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private com.ledgerflow.repository.TransactionRepository transactionRepository;
    @Autowired private com.ledgerflow.repository.LedgerEntryRepository ledgerEntryRepository;
    @Autowired private OpeningBalanceService openingBalanceService;

    private Long senderAccountId;
    private Long receiverAccountId;
    private Long clearingAccountId;
    private String senderBranchCode;
    private String receiverBranchCode;

    @BeforeEach
    @Transactional
    public void setUp() {
        // Clean everything for test isolation
        achTransferRepository.deleteAll();
        achBatchRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        branchRepository.deleteAll();

        // 1. Create Branches
        Branch branch1 = new Branch("Branch 1", "NYC", "BR001");
        Branch branch2 = new Branch("Branch 2", "LA", "BR002");
        Branch sysBranch = new Branch("System", "Sys", "SYS000");
        branchRepository.save(branch1);
        branchRepository.save(branch2);
        branchRepository.save(sysBranch);
        
        senderBranchCode = "BR001 (Branch 1)";
        receiverBranchCode = "BR002 (Branch 2)";

        // 2. Create Accounts
        Account sender = new Account("SENDER-01", "Alice", Account.AccountType.SAVINGS, new BigDecimal("1000.00"), branch1);
        Account receiver = new Account("RECEIVER-01", "Bob", Account.AccountType.SAVINGS, new BigDecimal("500.00"), branch2);
        Account clearing = new Account("ACH-CLEARING", "Clearing", Account.AccountType.CURRENT, BigDecimal.ZERO, sysBranch);
        
        senderAccountId = accountRepository.save(sender).getId();
        receiverAccountId = accountRepository.save(receiver).getId();
        clearingAccountId = accountRepository.save(clearing).getId();

        openingBalanceService.postOpeningBalance(sender, new BigDecimal("1000.00"));
        openingBalanceService.postOpeningBalance(receiver, new BigDecimal("500.00"));
    }

    @Test
    public void testSagaSuccessAndNetting() {
        // Step 1: Submit ACH transfer (Saga Step 1: Funds Reserved)
        AchTransfer transfer = achService.submitTransfer(senderAccountId, receiverAccountId, new BigDecimal("200.00"));

        assertEquals(AchTransfer.TransferStatus.FUNDS_RESERVED, transfer.getStatus());
        
        // Sender balance should be debited immediately
        Account sender = accountRepository.findById(senderAccountId).get();
        assertEquals(new BigDecimal("800.00"), sender.getBalance().setScale(2));

        // Clearing account holds the funds
        Account clearing = accountRepository.findById(clearingAccountId).get();
        assertEquals(new BigDecimal("200.00"), clearing.getBalance().setScale(2));

        // Receiver balance is unchanged
        Account receiver = accountRepository.findById(receiverAccountId).get();
        assertEquals(new BigDecimal("500.00"), receiver.getBalance().setScale(2));

        // Step 2: Settle the batch (Saga Step 2: Settlement)
        Long batchId = transfer.getBatch().getId();
        achSettlementEngine.processBatch(batchId);

        // Verify status
        Optional<AchTransfer> updatedTransfer = achTransferRepository.findById(transfer.getId());
        assertEquals(AchTransfer.TransferStatus.SETTLED, updatedTransfer.get().getStatus());
        assertEquals(AchBatch.BatchStatus.SETTLED, achBatchRepository.findById(batchId).get().getStatus());

        // Verify balances after settlement
        clearing = accountRepository.findById(clearingAccountId).get();
        assertEquals(new BigDecimal("0.00"), clearing.getBalance().setScale(2)); // Clearing empty
        
        receiver = accountRepository.findById(receiverAccountId).get();
        assertEquals(new BigDecimal("700.00"), receiver.getBalance().setScale(2)); // Receiver got funds

        // Verify Netting Algorithm
        Map<String, BigDecimal> netting = achSettlementEngine.calculateBranchNetting(batchId);
        
        // Branch 1 sent 200, so net is -200
        assertEquals(new BigDecimal("-200.00"), netting.get(senderBranchCode).setScale(2));
        // Branch 2 received 200, so net is +200
        assertEquals(new BigDecimal("200.00"), netting.get(receiverBranchCode).setScale(2));
    }

    @Test
    public void testSagaCompensation() {
        // Step 1: Submit ACH transfer
        AchTransfer transfer = achService.submitTransfer(senderAccountId, receiverAccountId, new BigDecimal("300.00"));
        Long batchId = transfer.getBatch().getId();

        // SIMULATE FAILURE CONDITION: Close the receiver account before settlement!
        Account receiver = accountRepository.findById(receiverAccountId).get();
        receiver.setStatus(Account.AccountStatus.CLOSED);
        accountRepository.save(receiver);

        // Step 2: Try to settle the batch. The transfer to receiver will fail (CLOSED account).
        // The Engine should catch it and run Compensation.
        achSettlementEngine.processBatch(batchId);

        // Verify transfer status is COMPENSATED
        AchTransfer updatedTransfer = achTransferRepository.findById(transfer.getId()).get();
        assertEquals(AchTransfer.TransferStatus.COMPENSATED, updatedTransfer.getStatus());
        assertTrue(updatedTransfer.getErrorMessage().contains("not ACTIVE"));

        // Verify funds were returned to Sender (1000.00)
        Account sender = accountRepository.findById(senderAccountId).get();
        assertEquals(new BigDecimal("1000.00"), sender.getBalance().setScale(2));

        // Verify Clearing is empty
        Account clearing = accountRepository.findById(clearingAccountId).get();
        assertEquals(new BigDecimal("0.00"), clearing.getBalance().setScale(2));
    }
}
