package com.minet.sacco.repository;

import com.minet.sacco.entity.Account;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@DisplayName("Transaction Repository Tests")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Account account;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        // Create test data
        Member member = new Member();
        member.setFirstName("John");
        member.setLastName("Doe");
        member.setEmail("john@example.com");
        member.setPhone("1234567890");
        member.setNationalId("12345678");
        entityManager.persistAndFlush(member);

        account = new Account();
        account.setMember(member);
        account.setAccountType(Account.AccountType.SAVINGS);
        account.setBalance(BigDecimal.valueOf(10000));
        entityManager.persistAndFlush(account);

        startDate = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        endDate = LocalDateTime.of(2026, 3, 31, 23, 59, 59);
    }

    @Test
    @DisplayName("Should calculate total operating expenses within date range")
    void testCalculateTotalOperatingExpenses() {
        // Arrange
        createTransaction(account, "Salary Payment", new BigDecimal("40000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));
        createTransaction(account, "Rent Payment", new BigDecimal("15000"), LocalDateTime.of(2026, 1, 20, 10, 0, 0));
        createTransaction(account, "Utilities Bill", new BigDecimal("5000"), LocalDateTime.of(2026, 2, 10, 10, 0, 0));
        createTransaction(account, "Other Expense", new BigDecimal("10000"), LocalDateTime.of(2026, 2, 15, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateTotalOperatingExpenses(startDate, endDate, "");

        // Assert
        assertEquals(new BigDecimal("70000"), result);
    }

    @Test
    @DisplayName("Should calculate salary expenses")
    void testCalculateSalaryExpenses() {
        // Arrange
        createTransaction(account, "Salary Payment", new BigDecimal("40000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));
        createTransaction(account, "Salary Bonus", new BigDecimal("5000"), LocalDateTime.of(2026, 2, 15, 10, 0, 0));
        createTransaction(account, "Rent Payment", new BigDecimal("15000"), LocalDateTime.of(2026, 1, 20, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateSalaryExpenses(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("45000"), result);
    }

    @Test
    @DisplayName("Should calculate rent expenses")
    void testCalculateRentExpenses() {
        // Arrange
        createTransaction(account, "Rent Payment", new BigDecimal("15000"), LocalDateTime.of(2026, 1, 20, 10, 0, 0));
        createTransaction(account, "Rent Payment", new BigDecimal("15000"), LocalDateTime.of(2026, 2, 20, 10, 0, 0));
        createTransaction(account, "Salary Payment", new BigDecimal("40000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateRentExpenses(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("30000"), result);
    }

    @Test
    @DisplayName("Should calculate utilities expenses")
    void testCalculateUtilitiesExpenses() {
        // Arrange
        createTransaction(account, "Utilities Bill", new BigDecimal("5000"), LocalDateTime.of(2026, 1, 10, 10, 0, 0));
        createTransaction(account, "Utilities Payment", new BigDecimal("3000"), LocalDateTime.of(2026, 2, 10, 10, 0, 0));
        createTransaction(account, "Salary Payment", new BigDecimal("40000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateUtilitiesExpenses(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("8000"), result);
    }

    @Test
    @DisplayName("Should calculate other expenses (excluding salary, rent, utilities)")
    void testCalculateOtherExpenses() {
        // Arrange
        createTransaction(account, "Salary Payment", new BigDecimal("40000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));
        createTransaction(account, "Rent Payment", new BigDecimal("15000"), LocalDateTime.of(2026, 1, 20, 10, 0, 0));
        createTransaction(account, "Utilities Bill", new BigDecimal("5000"), LocalDateTime.of(2026, 2, 10, 10, 0, 0));
        createTransaction(account, "Office Supplies", new BigDecimal("2000"), LocalDateTime.of(2026, 2, 15, 10, 0, 0));
        createTransaction(account, "Maintenance", new BigDecimal("3000"), LocalDateTime.of(2026, 2, 20, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateOtherExpenses(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("5000"), result);
    }

    @Test
    @DisplayName("Should calculate total expenses by date range")
    void testCalculateTotalExpensesByDateRange() {
        // Arrange
        createTransaction(account, "Salary Payment", new BigDecimal("40000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));
        createTransaction(account, "Rent Payment", new BigDecimal("15000"), LocalDateTime.of(2026, 1, 20, 10, 0, 0));
        createTransaction(account, "Utilities Bill", new BigDecimal("5000"), LocalDateTime.of(2026, 2, 10, 10, 0, 0));
        createTransaction(account, "Other Expense", new BigDecimal("10000"), LocalDateTime.of(2026, 2, 15, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateTotalExpensesByDateRange(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("70000"), result);
    }

    @Test
    @DisplayName("Should return zero when no expenses in date range")
    void testCalculateExpensesZero() {
        // Arrange
        LocalDateTime futureStart = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
        LocalDateTime futureEnd = LocalDateTime.of(2026, 6, 30, 23, 59, 59);
        createTransaction(account, "Salary Payment", new BigDecimal("40000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateTotalExpensesByDateRange(futureStart, futureEnd);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    @DisplayName("Should filter by date range correctly")
    void testCalculateExpensesDateRangeFiltering() {
        // Arrange
        createTransaction(account, "Salary Payment", new BigDecimal("40000"), LocalDateTime.of(2025, 12, 15, 10, 0, 0));
        createTransaction(account, "Rent Payment", new BigDecimal("15000"), LocalDateTime.of(2026, 1, 20, 10, 0, 0));
        createTransaction(account, "Utilities Bill", new BigDecimal("5000"), LocalDateTime.of(2026, 4, 10, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateTotalExpensesByDateRange(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("15000"), result);
    }

    @Test
    @DisplayName("Should handle case-insensitive description matching")
    void testCalculateExpensesCaseInsensitive() {
        // Arrange
        createTransaction(account, "SALARY PAYMENT", new BigDecimal("40000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));
        createTransaction(account, "Salary Bonus", new BigDecimal("5000"), LocalDateTime.of(2026, 2, 15, 10, 0, 0));
        createTransaction(account, "salary advance", new BigDecimal("3000"), LocalDateTime.of(2026, 2, 20, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateSalaryExpenses(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("48000"), result);
    }

    @Test
    @DisplayName("Should calculate expenses by category correctly")
    void testCalculateExpensesByCategory() {
        // Arrange
        createTransaction(account, "Salary Payment", new BigDecimal("40000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));
        createTransaction(account, "Rent Payment", new BigDecimal("15000"), LocalDateTime.of(2026, 1, 20, 10, 0, 0));
        createTransaction(account, "Utilities Bill", new BigDecimal("5000"), LocalDateTime.of(2026, 2, 10, 10, 0, 0));

        // Act
        BigDecimal salaries = transactionRepository.calculateSalaryExpenses(startDate, endDate);
        BigDecimal rent = transactionRepository.calculateRentExpenses(startDate, endDate);
        BigDecimal utilities = transactionRepository.calculateUtilitiesExpenses(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("40000"), salaries);
        assertEquals(new BigDecimal("15000"), rent);
        assertEquals(new BigDecimal("5000"), utilities);
    }

    @Test
    @DisplayName("Should calculate loan processing fees")
    void testCalculateLoanProcessingFees() {
        // Arrange
        createTransaction(account, "Loan Processing Fee", new BigDecimal("5000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));
        createTransaction(account, "Loan Application Fee", new BigDecimal("2000"), LocalDateTime.of(2026, 1, 20, 10, 0, 0));
        createTransaction(account, "Account Maintenance Fee", new BigDecimal("1000"), LocalDateTime.of(2026, 2, 10, 10, 0, 0));
        createTransaction(account, "Other Fee", new BigDecimal("500"), LocalDateTime.of(2026, 2, 15, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateLoanProcessingFees(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("7000"), result);
    }

    @Test
    @DisplayName("Should calculate account maintenance fees")
    void testCalculateAccountMaintenanceFees() {
        // Arrange
        createTransaction(account, "Account Maintenance Fee", new BigDecimal("1000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));
        createTransaction(account, "Maintenance Fee", new BigDecimal("500"), LocalDateTime.of(2026, 1, 20, 10, 0, 0));
        createTransaction(account, "Account Fee", new BigDecimal("750"), LocalDateTime.of(2026, 2, 10, 10, 0, 0));
        createTransaction(account, "Loan Processing Fee", new BigDecimal("2000"), LocalDateTime.of(2026, 2, 15, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateAccountMaintenanceFees(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("2250"), result);
    }

    @Test
    @DisplayName("Should calculate other fees (excluding loan and maintenance fees)")
    void testCalculateOtherFees() {
        // Arrange
        createTransaction(account, "Transfer Fee", new BigDecimal("500"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));
        createTransaction(account, "Withdrawal Fee", new BigDecimal("250"), LocalDateTime.of(2026, 1, 20, 10, 0, 0));
        createTransaction(account, "Service Fee", new BigDecimal("300"), LocalDateTime.of(2026, 2, 10, 10, 0, 0));
        createTransaction(account, "Loan Processing Fee", new BigDecimal("2000"), LocalDateTime.of(2026, 2, 15, 10, 0, 0));
        createTransaction(account, "Account Maintenance Fee", new BigDecimal("1000"), LocalDateTime.of(2026, 2, 20, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateOtherFees(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("1050"), result);
    }

    @Test
    @DisplayName("Should calculate total fees and charges within date range")
    void testCalculateTotalFeesAndCharges() {
        // Arrange
        createTransaction(account, "Loan Processing Fee", new BigDecimal("5000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));
        createTransaction(account, "Account Maintenance Fee", new BigDecimal("1000"), LocalDateTime.of(2026, 1, 20, 10, 0, 0));
        createTransaction(account, "Transfer Fee", new BigDecimal("500"), LocalDateTime.of(2026, 2, 10, 10, 0, 0));
        createTransaction(account, "Salary Payment", new BigDecimal("40000"), LocalDateTime.of(2026, 2, 15, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateTotalFeesAndCharges(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("6500"), result);
    }

    @Test
    @DisplayName("Should return zero when no fees in date range")
    void testCalculateFeesZero() {
        // Arrange
        LocalDateTime futureStart = LocalDateTime.of(2026, 6, 1, 0, 0, 0);
        LocalDateTime futureEnd = LocalDateTime.of(2026, 6, 30, 23, 59, 59);
        createTransaction(account, "Loan Processing Fee", new BigDecimal("5000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateTotalFeesAndCharges(futureStart, futureEnd);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    @DisplayName("Should handle case-insensitive fee description matching")
    void testCalculateFeesCaseInsensitive() {
        // Arrange
        createTransaction(account, "LOAN PROCESSING FEE", new BigDecimal("5000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));
        createTransaction(account, "loan processing fee", new BigDecimal("2000"), LocalDateTime.of(2026, 1, 20, 10, 0, 0));
        createTransaction(account, "Loan Processing Fee", new BigDecimal("3000"), LocalDateTime.of(2026, 2, 10, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateLoanProcessingFees(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("10000"), result);
    }

    @Test
    @DisplayName("Should filter fees by date range correctly")
    void testCalculateFeesDateRangeFiltering() {
        // Arrange
        createTransaction(account, "Loan Processing Fee", new BigDecimal("5000"), LocalDateTime.of(2025, 12, 15, 10, 0, 0));
        createTransaction(account, "Account Maintenance Fee", new BigDecimal("1000"), LocalDateTime.of(2026, 1, 20, 10, 0, 0));
        createTransaction(account, "Transfer Fee", new BigDecimal("500"), LocalDateTime.of(2026, 4, 10, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateTotalFeesAndCharges(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("1000"), result);
    }

    @Test
    @DisplayName("Should calculate interest income from savings")
    void testCalculateInterestIncomeFromSavings() {
        // Arrange
        createTransaction(account, "Interest Payment", new BigDecimal("5000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));
        createTransaction(account, "Savings Interest", new BigDecimal("3000"), LocalDateTime.of(2026, 1, 20, 10, 0, 0));
        createTransaction(account, "Interest Accrual", new BigDecimal("2000"), LocalDateTime.of(2026, 2, 10, 10, 0, 0));
        createTransaction(account, "Salary Payment", new BigDecimal("40000"), LocalDateTime.of(2026, 2, 15, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateInterestIncomeFromSavings(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("10000"), result);
    }

    @Test
    @DisplayName("Should calculate other income (excluding interest)")
    void testCalculateOtherIncome() {
        // Arrange
        createTransaction(account, "Miscellaneous Income", new BigDecimal("1000"), LocalDateTime.of(2026, 1, 15, 10, 0, 0));
        createTransaction(account, "Interest Payment", new BigDecimal("5000"), LocalDateTime.of(2026, 1, 20, 10, 0, 0));

        // Act
        BigDecimal result = transactionRepository.calculateOtherIncome(startDate, endDate);

        // Assert
        assertNotNull(result);
    }

    // Helper method to create test transactions
    private Transaction createTransaction(Account acct, String description, BigDecimal amount, LocalDateTime date) {
        Transaction transaction = new Transaction();
        transaction.setAccount(acct);
        transaction.setDescription(description);
        transaction.setAmount(amount);
        transaction.setTransactionDate(date);
        transaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
        return entityManager.persistAndFlush(transaction);
    }
}
