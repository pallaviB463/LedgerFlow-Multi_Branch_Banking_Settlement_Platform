package com.ledgerflow;

import com.ledgerflow.dto.ReportDto;
import com.ledgerflow.entity.Account;
import com.ledgerflow.entity.Branch;
import com.ledgerflow.repository.AccountRepository;
import com.ledgerflow.repository.BranchRepository;
import com.ledgerflow.repository.LedgerEntryRepository;
import com.ledgerflow.repository.TransactionRepository;
import com.ledgerflow.service.OpeningBalanceService;
import com.ledgerflow.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class ReportServiceTest {

    @Autowired private ReportService reportService;
    @Autowired private AccountRepository accountRepository;
    @Autowired private BranchRepository branchRepository;
    @Autowired private LedgerEntryRepository ledgerEntryRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private OpeningBalanceService openingBalanceService;

    private String testAccountNumber;

    @BeforeEach
    @Transactional
    public void setUp() {
        ledgerEntryRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        branchRepository.deleteAll();

        Branch branch = branchRepository.save(
                new Branch("B-RPT", "Report Branch", "Test City")
        );

        Account account = new Account(
                "ACC-RPT-1",
                "Test User",
                Account.AccountType.SAVINGS,
                new BigDecimal("1000.00"),
                branch
        );
        account = accountRepository.save(account);
        testAccountNumber = account.getAccountNumber();

        // Add 1000 balance
        openingBalanceService.postOpeningBalance(account, new BigDecimal("1000.00"));
    }

    @Test
    public void testGenerateTrialBalance() {
        ReportDto.TrialBalanceReport report = reportService.generateTrialBalance();
        assertNotNull(report.getGeneratedAt());
        assertTrue(report.getAccounts().size() > 0);
        
        // Find our test account in the report
        ReportDto.AccountBalance accBal = report.getAccounts().stream()
                .filter(a -> a.getAccountNumber().equals(testAccountNumber))
                .findFirst()
                .orElseThrow();
                
        assertEquals(new BigDecimal("1000.00"), accBal.getBalance().setScale(2));
    }

    @Test
    public void testGenerateAccountStatement() {
        ReportDto.AccountStatement statement = reportService.generateAccountStatement(testAccountNumber);
        
        assertEquals(testAccountNumber, statement.getAccountNumber());
        assertEquals(new BigDecimal("1000.00"), statement.getCurrentBalance().setScale(2));
        assertEquals(1, statement.getEntries().size());
        
        ReportDto.StatementEntry entry = statement.getEntries().get(0);
        assertEquals(new BigDecimal("1000.00"), entry.getAmount().setScale(2));
        assertEquals("Opening balance deposit", entry.getDescription());
    }
}
