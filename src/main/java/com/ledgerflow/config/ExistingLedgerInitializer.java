
package com.ledgerflow.config;

import com.ledgerflow.entity.Account;
import com.ledgerflow.repository.AccountRepository;
import com.ledgerflow.repository.LedgerEntryRepository;
import com.ledgerflow.repository.TransactionRepository;
import com.ledgerflow.service.OpeningBalanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
@Profile("!test")
public class ExistingLedgerInitializer implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(ExistingLedgerInitializer.class);

    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final TransactionRepository transactionRepository;
    private final OpeningBalanceService openingBalanceService;

    private static final Map<String, BigDecimal> ORIGINAL_BALANCES = Map.of(
            "ACC-10001", new BigDecimal("50000.00"),
            "ACC-10002", new BigDecimal("30000.00"),
            "ACC-10003", new BigDecimal("100000.00"),
            "ACC-10004", new BigDecimal("75000.00"),
            "ACC-10005", new BigDecimal("200000.00")
    );

    public ExistingLedgerInitializer(
            AccountRepository accountRepository,
            LedgerEntryRepository ledgerEntryRepository,
            TransactionRepository transactionRepository,
            OpeningBalanceService openingBalanceService) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.transactionRepository = transactionRepository;
        this.openingBalanceService = openingBalanceService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        for (Account account : accountRepository.findAll()) {
            String number = account.getAccountNumber();
            String reference = "OPEN-" + number;

            if (transactionRepository.existsByReferenceNumber(reference)) {
                continue;
            }

            BigDecimal original = ORIGINAL_BALANCES.get(number);
            if (original == null || original.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal ledgerBalance =
                    ledgerEntryRepository.calculateBalanceByAccountId(account.getId());
            if (ledgerBalance == null) {
                ledgerBalance = BigDecimal.ZERO;
            }

            BigDecimal expectedCurrent = original.add(ledgerBalance);

            if (expectedCurrent.compareTo(account.getBalance()) != 0) {
                log.warn("Skipping {}: balance mismatch. Stored={}, expected={}",
                        number, account.getBalance(), expectedCurrent);
                continue;
            }

            openingBalanceService.postOpeningBalance(account, original);
            log.info("Opening ledger created for {}: {}", number, original);
        }

        log.info("Existing ledger initialization completed.");
    }
}
