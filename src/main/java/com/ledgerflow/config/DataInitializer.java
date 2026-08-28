package com.ledgerflow.config;

import com.ledgerflow.entity.Account;
import com.ledgerflow.entity.Branch;
import com.ledgerflow.repository.AccountRepository;
import com.ledgerflow.repository.BranchRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * DataInitializer — Seeds the database with sample data on startup.
 *
 * This runs automatically when the app starts (only in dev, not in tests).
 * It creates a few branches and accounts so we have something to work with
 * without manually calling APIs every time.
 *
 * CommandLineRunner: Spring calls the run() method after the app starts.
 * @Profile("!test"): This bean is skipped when running tests
 *                    (tests use their own data setup).
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BranchRepository branchRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // Only seed if the database is empty
        if (branchRepository.count() > 0) {
            log.info("Database already seeded — skipping data initialization.");
            return;
        }

        log.info("Seeding database with sample branches and accounts...");

        // --- Create Branches ---
        Branch downtown = branchRepository.save(
                new Branch("Downtown Branch", "123 Main Street, City Center", "LFLW001"));

        Branch airport = branchRepository.save(
                new Branch("Airport Branch", "Terminal 2, International Airport", "LFLW002"));

        Branch tech = branchRepository.save(
                new Branch("Tech Park Branch", "Block A, Tech Park, Whitefield", "LFLW003"));

        log.info("Created {} branches", branchRepository.count());

        // --- Create Accounts ---
        accountRepository.save(new Account(
                "ACC-10001", "Alice Kumar", Account.AccountType.SAVINGS,
                new BigDecimal("50000.00"), downtown));

        accountRepository.save(new Account(
                "ACC-10002", "Bob Sharma", Account.AccountType.SAVINGS,
                new BigDecimal("30000.00"), downtown));

        accountRepository.save(new Account(
                "ACC-10003", "Carol Nair", Account.AccountType.CURRENT,
                new BigDecimal("100000.00"), airport));

        accountRepository.save(new Account(
                "ACC-10004", "David Reddy", Account.AccountType.SAVINGS,
                new BigDecimal("75000.00"), tech));

        accountRepository.save(new Account(
                "ACC-10005", "Eve Patel", Account.AccountType.CURRENT,
                new BigDecimal("200000.00"), tech));

        log.info("Created {} accounts", accountRepository.count());
        log.info("Data initialization complete.");
    }
}
