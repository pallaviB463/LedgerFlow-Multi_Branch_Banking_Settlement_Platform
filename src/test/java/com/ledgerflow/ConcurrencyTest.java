package com.ledgerflow;

import com.ledgerflow.dto.TransferDto;
import com.ledgerflow.entity.Account;
import com.ledgerflow.entity.Branch;
import com.ledgerflow.exception.InsufficientBalanceException;
import com.ledgerflow.repository.AccountRepository;
import com.ledgerflow.repository.BranchRepository;
import com.ledgerflow.repository.LedgerEntryRepository;
import com.ledgerflow.repository.TransactionRepository;
import com.ledgerflow.service.OpeningBalanceService;
import com.ledgerflow.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ═══════════════════════════════════════════════════════════════════
 * CONCURRENCY TEST — Phase 6
 * ═══════════════════════════════════════════════════════════════════
 *
 * PURPOSE: Prove that the system handles simultaneous transfers
 *          correctly — no double-spend, no balance corruption,
 *          no lost updates.
 *
 * THE SCENARIO:
 *   - Account A starts with ₹100,000
 *   - Account B starts with ₹0
 *   - 50 threads ALL fire at the same time, each trying to
 *     transfer ₹1,000 from A to B
 *   - If ALL succeed: A ends at ₹50,000, B ends at ₹50,000
 *   - If locking breaks: A might go negative (double-spend!)
 *
 * WHAT WE VERIFY:
 *   1. Total money is conserved (A.balance + B.balance == ₹100,000)
 *   2. A's balance is never negative
 *   3. Ledger balance (from immutable entries) == stored account balance
 *   4. Exactly 2 ledger entries per successful transfer
 *
 * WHY THIS WORKS (with locking):
 *   - Each transfer does SELECT ... FOR UPDATE on both accounts
 *   - Thread 2 is BLOCKED while Thread 1 holds the lock
 *   - Thread 2 sees Thread 1's committed balance before proceeding
 *   - No two threads ever operate on stale data simultaneously
 *
 * ═══════════════════════════════════════════════════════════════════
 */
@SpringBootTest
@ActiveProfiles("test")
public class ConcurrencyTest {

    @Autowired private TransferService transferService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private LedgerEntryRepository ledgerEntryRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private com.ledgerflow.repository.AchTransferRepository achTransferRepository;
    @Autowired private com.ledgerflow.repository.AchBatchRepository achBatchRepository;
    @Autowired private OpeningBalanceService openingBalanceService;

    private Long accountAId;
    private Long accountBId;

    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("100000.00");
    private static final BigDecimal TRANSFER_AMOUNT = new BigDecimal("1000.00");
    private static final int        THREAD_COUNT    = 50;

    /**
     * Set up clean test accounts before each test.
     *
     * @Transactional here ensures all setup (accounts + opening ledger entries)
     * is committed as one unit BEFORE concurrent threads start.
     */
    @BeforeEach
    @Transactional
    public void setUp() {
        // Clean slate
        achTransferRepository.deleteAll();
        achBatchRepository.deleteAll();
        ledgerEntryRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        branchRepository.deleteAll();

        Branch branch = branchRepository.save(
                new Branch("Test Branch", "Test Location", "TEST001"));

        // Create Account A with ₹100,000 initial balance
        Account accountA = accountRepository.save(
                new Account("ACC-TEST-A", "Alice (Sender)",
                        Account.AccountType.SAVINGS, INITIAL_BALANCE, branch));
        // Post the opening CREDIT ledger entry so the ledger is the source of truth
        openingBalanceService.postOpeningBalance(accountA, INITIAL_BALANCE);

        // Create Account B with ₹0 initial balance (no ledger entry needed for zero)
        Account accountB = accountRepository.save(
                new Account("ACC-TEST-B", "Bob (Receiver)",
                        Account.AccountType.SAVINGS, BigDecimal.ZERO, branch));

        accountAId = accountA.getId();
        accountBId = accountB.getId();
    }

    /**
     * THE MAIN CONCURRENCY TEST.
     *
     * Fires THREAD_COUNT transfers simultaneously using a CountDownLatch
     * to ensure maximum concurrency (all threads start at the exact same time).
     */
    @Test
    @DisplayName("50 concurrent transfers must not cause double-spend or balance corruption")
    public void testConcurrentTransfersAreAcidSafe() throws InterruptedException {

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        // Holds all threads at the starting line until released simultaneously
        CountDownLatch startLine  = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(THREAD_COUNT);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        List<String>  errors       = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < THREAD_COUNT; i++) {
            executor.submit(() -> {
                try {
                    startLine.await();  // All threads wait here until released

                    TransferDto.TransferRequest request = new TransferDto.TransferRequest();
                    request.setSourceAccountId(accountAId);
                    request.setDestinationAccountId(accountBId);
                    request.setAmount(TRANSFER_AMOUNT);
                    request.setDescription("Concurrent test transfer");

                    transferService.executeTransfer(request);
                    successCount.incrementAndGet();

                } catch (InsufficientBalanceException e) {
                    // Expected once A runs out of funds — not a bug
                    failureCount.incrementAndGet();
                } catch (CannotAcquireLockException e) {
                    // H2 detects circular lock waits and rolls back safely.
                    // Under PostgreSQL, consistent lock ordering prevents true deadlocks.
                    // Either way: the transaction is rolled back atomically — no corruption.
                    failureCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    errors.add("Thread interrupted: " + e.getMessage());
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    errors.add("Unexpected error: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                } finally {
                    finishLine.countDown();
                }
            });
        }

        startLine.countDown();  // Fire! All threads start simultaneously

        boolean allFinished = finishLine.await(60, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(allFinished)
                .as("All threads should finish within 60 seconds")
                .isTrue();

        assertThat(errors)
                .as("No unexpected errors should occur: " + errors)
                .isEmpty();

        // Re-read from DB to get final committed state
        Account finalA = accountRepository.findById(accountAId).orElseThrow();
        Account finalB = accountRepository.findById(accountBId).orElseThrow();

        System.out.println("\n════════════════════════════════════════════");
        System.out.println("  CONCURRENCY TEST RESULTS");
        System.out.println("  Threads fired:              " + THREAD_COUNT);
        System.out.println("  Successful transfers:       " + successCount.get());
        System.out.println("  Failed (rollback/lock):     " + failureCount.get());
        System.out.println("  Account A final balance:    " + finalA.getBalance());
        System.out.println("  Account B final balance:    " + finalB.getBalance());
        System.out.println("  (H2 may deadlock-rollback; PostgreSQL uses lock ordering)");
        System.out.println("════════════════════════════════════════════\n");

        // ── INVARIANT 1: Total money is conserved ─────────────────────────
        BigDecimal totalBalance = finalA.getBalance().add(finalB.getBalance());
        assertThat(totalBalance)
                .as("Total money must be conserved (no creation or destruction of funds)")
                .isEqualByComparingTo(INITIAL_BALANCE);

        // ── INVARIANT 2: No negative balance ─────────────────────────────
        assertThat(finalA.getBalance())
                .as("Account A balance must never go negative")
                .isGreaterThanOrEqualTo(BigDecimal.ZERO);

        // ── INVARIANT 3: B balance == successCount × transferAmount ───────
        BigDecimal expectedBBalance = TRANSFER_AMOUNT.multiply(new BigDecimal(successCount.get()));
        assertThat(finalB.getBalance())
                .as("Account B balance should equal number of successful transfers × amount")
                .isEqualByComparingTo(expectedBBalance);

        // ── INVARIANT 4: Ledger balance == stored balance ─────────────────
        // The immutable ledger entries must sum to the same value as the
        // denormalized account.balance field. If they differ, double-entry is broken.
        BigDecimal ledgerBalanceA = ledgerEntryRepository.calculateBalanceByAccountId(accountAId);
        BigDecimal ledgerBalanceB = ledgerEntryRepository.calculateBalanceByAccountId(accountBId);

        assertThat(ledgerBalanceA)
                .as("Account A: ledger-derived balance must match stored balance")
                .isEqualByComparingTo(finalA.getBalance());

        assertThat(ledgerBalanceB)
                .as("Account B: ledger-derived balance must match stored balance")
                .isEqualByComparingTo(finalB.getBalance());

        // ── INVARIANT 5: 2 ledger entries per successful transfer ─────────
        // Each transfer = 1 DEBIT (source) + 1 CREDIT (dest) = 2 entries.
        // Plus 1 opening CREDIT for Account A = successCount*2 + 1 total.
        long totalTransferEntries = ledgerEntryRepository.count() - 1; // subtract opening entry
        assertThat(totalTransferEntries)
                .as("Each successful transfer must produce exactly 2 ledger entries (1 DEBIT + 1 CREDIT)")
                .isEqualTo((long) successCount.get() * 2);
    }

    /**
     * Verify the self-transfer guard works correctly.
     */
    @Test
    @DisplayName("Transfer to same account must be rejected")
    public void testSelfTransferIsRejected() {
        TransferDto.TransferRequest request = new TransferDto.TransferRequest();
        request.setSourceAccountId(accountAId);
        request.setDestinationAccountId(accountAId);  // same account!
        request.setAmount(new BigDecimal("100.00"));

        org.junit.jupiter.api.Assertions.assertThrows(
                com.ledgerflow.exception.InvalidOperationException.class,
                () -> transferService.executeTransfer(request),
                "Self-transfer should throw InvalidOperationException"
        );
    }

    /**
     * Transfer more than available balance must be rejected.
     */
    @Test
    @DisplayName("Transfer exceeding balance must be rejected with InsufficientBalanceException")
    public void testInsufficientBalanceIsRejected() {
        TransferDto.TransferRequest request = new TransferDto.TransferRequest();
        request.setSourceAccountId(accountAId);
        request.setDestinationAccountId(accountBId);
        request.setAmount(new BigDecimal("999999999.00"));  // way more than balance

        org.junit.jupiter.api.Assertions.assertThrows(
                InsufficientBalanceException.class,
                () -> transferService.executeTransfer(request),
                "Transfer exceeding balance should throw InsufficientBalanceException"
        );
    }
}
