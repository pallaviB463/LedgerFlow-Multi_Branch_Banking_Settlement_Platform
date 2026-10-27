
package com.ledgerflow.config;

import com.ledgerflow.entity.Account;
import com.ledgerflow.entity.Branch;
import com.ledgerflow.repository.AccountRepository;
import com.ledgerflow.repository.BranchRepository;
import com.ledgerflow.service.OpeningBalanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@Profile("!test")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BranchRepository branchRepository;
    private final AccountRepository accountRepository;
    private final OpeningBalanceService openingBalanceService;

    public DataInitializer(
            BranchRepository branchRepository,
            AccountRepository accountRepository,
            OpeningBalanceService openingBalanceService) {
        this.branchRepository = branchRepository;
        this.accountRepository = accountRepository;
        this.openingBalanceService = openingBalanceService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (branchRepository.count() > 0) {
            log.info("Database already seeded — skipping data initialization.");
            return;
        }

        log.info("Seeding database with sample branches and accounts...");

        Branch downtown = branchRepository.save(
                new Branch("Downtown Branch", "123 Main Street, City Center", "LFLW001"));
        Branch airport = branchRepository.save(
                new Branch("Airport Branch", "Terminal 2, International Airport", "LFLW002"));
        Branch tech = branchRepository.save(
                new Branch("Tech Park Branch", "Block A, Tech Park, Whitefield", "LFLW003"));
        Branch system = branchRepository.save(
                new Branch("System Clearing", "Internal", "SYS0000"));

        createAccount("ACC-10001", "Alice Kumar", Account.AccountType.SAVINGS,
                "50000.00", downtown);
        createAccount("ACC-10002", "Bob Sharma", Account.AccountType.SAVINGS,
                "30000.00", downtown);
        createAccount("ACC-10003", "Carol Nair", Account.AccountType.CURRENT,
                "100000.00", airport);
        createAccount("ACC-10004", "David Reddy", Account.AccountType.SAVINGS,
                "75000.00", tech);
        createAccount("ACC-10005", "Eve Patel", Account.AccountType.CURRENT,
                "200000.00", tech);

        createAccount("ACH-CLEARING", "ACH Settlement Suspense",
                Account.AccountType.CURRENT, "0.00", system);

        log.info("Created {} branches and {} accounts",
                branchRepository.count(), accountRepository.count());
        log.info("Data initialization complete.");
    }

    private void createAccount(
            String number,
            String holder,
            Account.AccountType type,
            String initialBalance,
            Branch branch) {

        BigDecimal balance = new BigDecimal(initialBalance);
        Account account = accountRepository.save(
                new Account(number, holder, type, balance, branch));

        openingBalanceService.postOpeningBalance(account, balance);
    }
}
