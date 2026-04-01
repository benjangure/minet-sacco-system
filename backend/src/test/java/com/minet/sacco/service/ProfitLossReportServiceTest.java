package com.minet.sacco.service;

import com.minet.sacco.dto.*;
import com.minet.sacco.repository.LoanRepository;
import com.minet.sacco.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Profit & Loss Report Service Tests")
class ProfitLossReportServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ProfitLossReportService profitLossReportService;

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        startDate = LocalDate.of(2026, 1, 1);
        endDate = LocalDate.of(2026, 3, 31);
        startDateTime = startDate.atStartOfDay();
        endDateTime = endDate.atTime(23, 59, 59);
    }

    @Test
    @DisplayName("Should calculate interest income from disbursed/repaid loans")
    void testCalculateInterestIncome() {
        // Arrange
        BigDecimal expectedInterest = new BigDecimal("15000");
        when(loanRepository.sumInterestIncomeInPeriod(startDateTime, endDateTime))
                .thenReturn(expectedInterest);

        // Act
        BigDecimal result = profitLossReportService.calculateInterestIncome(startDate, endDate);

        // Assert
        assertEquals(expectedInterest, result);
        verify(loanRepository, times(1)).sumInterestIncomeInPeriod(startDateTime, endDateTime);
    }

    @Test
    @DisplayName("Should return zero when no interest income in period")
    void testCalculateInterestIncomeZero() {
        // Arrange
        when(loanRepository.sumInterestIncomeInPeriod(startDateTime, endDateTime))
                .thenReturn(null);

        // Act
        BigDecimal result = profitLossReportService.calculateInterestIncome(startDate, endDate);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    @DisplayName("Should calculate loan loss provisions from defaulted loans")
    void testCalculateLoanLossProvisions() {
        // Arrange
        BigDecimal expectedProvisions = new BigDecimal("75000");
        when(loanRepository.sumLoanLossProvisionsInPeriod(startDateTime, endDateTime))
                .thenReturn(expectedProvisions);

        // Act
        BigDecimal result = profitLossReportService.calculateLoanLossProvisions(startDate, endDate);

        // Assert
        assertEquals(expectedProvisions, result);
        verify(loanRepository, times(1)).sumLoanLossProvisionsInPeriod(startDateTime, endDateTime);
    }

    @Test
    @DisplayName("Should return zero when no loan loss provisions in period")
    void testCalculateLoanLossProvisionsZero() {
        // Arrange
        when(loanRepository.sumLoanLossProvisionsInPeriod(startDateTime, endDateTime))
                .thenReturn(null);

        // Act
        BigDecimal result = profitLossReportService.calculateLoanLossProvisions(startDate, endDate);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    @DisplayName("Should calculate operating expenses")
    void testCalculateOperatingExpenses() {
        // Arrange
        BigDecimal salaries = new BigDecimal("40000");
        BigDecimal rent = new BigDecimal("15000");
        BigDecimal utilities = new BigDecimal("5000");
        BigDecimal other = new BigDecimal("10000");

        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "salary"))
                .thenReturn(salaries);
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "rent"))
                .thenReturn(rent);
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "utilities"))
                .thenReturn(utilities);
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "operational"))
                .thenReturn(other);

        // Act
        BigDecimal result = profitLossReportService.calculateOperatingExpenses(startDate, endDate);

        // Assert
        assertEquals(new BigDecimal("70000"), result);
    }

    @Test
    @DisplayName("Should calculate loan processing fees")
    void testCalculateLoanProcessingFees() {
        // Arrange
        BigDecimal expectedFees = new BigDecimal("15000");
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "loan fee"))
                .thenReturn(expectedFees);

        // Act
        BigDecimal result = profitLossReportService.calculateLoanProcessingFees(startDate, endDate);

        // Assert
        assertEquals(expectedFees, result);
    }

    @Test
    @DisplayName("Should calculate account maintenance fees")
    void testCalculateAccountMaintenanceFees() {
        // Arrange
        BigDecimal expectedFees = new BigDecimal("5000");
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "account maintenance"))
                .thenReturn(expectedFees);

        // Act
        BigDecimal result = profitLossReportService.calculateAccountMaintenanceFees(startDate, endDate);

        // Assert
        assertEquals(expectedFees, result);
    }

    @Test
    @DisplayName("Should calculate other fees")
    void testCalculateOtherFees() {
        // Arrange
        BigDecimal expectedFees = new BigDecimal("2000");
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "fee"))
                .thenReturn(expectedFees);

        // Act
        BigDecimal result = profitLossReportService.calculateOtherFees(startDate, endDate);

        // Assert
        assertEquals(expectedFees, result);
    }

    @Test
    @DisplayName("Should calculate other income")
    void testCalculateOtherIncome() {
        // Arrange
        BigDecimal expectedIncome = new BigDecimal("3000");
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "miscellaneous income"))
                .thenReturn(expectedIncome);

        // Act
        BigDecimal result = profitLossReportService.calculateOtherIncome(startDate, endDate);

        // Assert
        assertEquals(expectedIncome, result);
    }

    @Test
    @DisplayName("Should calculate other expenses")
    void testCalculateOtherExpenses() {
        // Arrange
        BigDecimal expectedExpenses = new BigDecimal("5000");
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "miscellaneous expense"))
                .thenReturn(expectedExpenses);

        // Act
        BigDecimal result = profitLossReportService.calculateOtherExpenses(startDate, endDate);

        // Assert
        assertEquals(expectedExpenses, result);
    }

    @Test
    @DisplayName("Should calculate net profit when revenue exceeds expenses")
    void testCalculateNetProfitLoss_Profit() {
        // Arrange
        BigDecimal revenue = new BigDecimal("100000");
        BigDecimal expenses = new BigDecimal("60000");

        // Act
        BigDecimal result = profitLossReportService.calculateNetProfitLoss(revenue, expenses);

        // Assert
        assertEquals(new BigDecimal("40000"), result);
    }

    @Test
    @DisplayName("Should calculate net loss when expenses exceed revenue")
    void testCalculateNetProfitLoss_Loss() {
        // Arrange
        BigDecimal revenue = new BigDecimal("50000");
        BigDecimal expenses = new BigDecimal("80000");

        // Act
        BigDecimal result = profitLossReportService.calculateNetProfitLoss(revenue, expenses);

        // Assert
        assertEquals(new BigDecimal("-30000"), result);
    }

    @Test
    @DisplayName("Should calculate zero net profit when revenue equals expenses")
    void testCalculateNetProfitLoss_BreakEven() {
        // Arrange
        BigDecimal revenue = new BigDecimal("100000");
        BigDecimal expenses = new BigDecimal("100000");

        // Act
        BigDecimal result = profitLossReportService.calculateNetProfitLoss(revenue, expenses);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    @DisplayName("Should calculate profit margin correctly")
    void testCalculateProfitMargin() {
        // Arrange
        BigDecimal netProfitLoss = new BigDecimal("30000");
        BigDecimal revenue = new BigDecimal("100000");

        // Act
        BigDecimal result = profitLossReportService.calculateProfitMargin(netProfitLoss, revenue);

        // Assert
        assertEquals(new BigDecimal("30.00"), result);
    }

    @Test
    @DisplayName("Should calculate negative profit margin for loss")
    void testCalculateProfitMargin_Negative() {
        // Arrange
        BigDecimal netProfitLoss = new BigDecimal("-20000");
        BigDecimal revenue = new BigDecimal("100000");

        // Act
        BigDecimal result = profitLossReportService.calculateProfitMargin(netProfitLoss, revenue);

        // Assert
        assertEquals(new BigDecimal("-20.00"), result);
    }

    @Test
    @DisplayName("Should return zero profit margin when revenue is zero")
    void testCalculateProfitMargin_ZeroRevenue() {
        // Arrange
        BigDecimal netProfitLoss = new BigDecimal("0");
        BigDecimal revenue = BigDecimal.ZERO;

        // Act
        BigDecimal result = profitLossReportService.calculateProfitMargin(netProfitLoss, revenue);

        // Assert
        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    @DisplayName("Should generate complete P&L report")
    void testGenerateProfitLossReport() {
        // Arrange
        when(loanRepository.sumInterestIncomeInPeriod(startDateTime, endDateTime))
                .thenReturn(new BigDecimal("10000"));
        when(loanRepository.sumLoanLossProvisionsInPeriod(startDateTime, endDateTime))
                .thenReturn(new BigDecimal("5000"));
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "salary"))
                .thenReturn(new BigDecimal("40000"));
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "rent"))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "utilities"))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "operational"))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "loan fee"))
                .thenReturn(new BigDecimal("5000"));
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "account maintenance"))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "fee"))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "miscellaneous income"))
                .thenReturn(BigDecimal.ZERO);
        when(transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "miscellaneous expense"))
                .thenReturn(BigDecimal.ZERO);

        // Act
        ProfitLossReportDTO report = profitLossReportService.generateProfitLossReport(startDate, endDate);

        // Assert
        assertNotNull(report);
        assertNotNull(report.getPeriod());
        assertEquals(startDate, report.getPeriod().getStartDate());
        assertEquals(endDate, report.getPeriod().getEndDate());
        
        assertNotNull(report.getRevenue());
        assertEquals(new BigDecimal("10000"), report.getRevenue().getInterestIncome().getFromLoans());
        
        assertNotNull(report.getExpenses());
        assertNotNull(report.getNetProfitLoss());
        assertNotNull(report.getProfitMargin());
        assertNotNull(report.getGeneratedAt());
    }

    @Test
    @DisplayName("Should handle empty data")
    void testGenerateProfitLossReport_EmptyData() {
        // Arrange
        when(loanRepository.sumInterestIncomeInPeriod(startDateTime, endDateTime))
                .thenReturn(null);
        when(loanRepository.sumLoanLossProvisionsInPeriod(startDateTime, endDateTime))
                .thenReturn(null);
        when(transactionRepository.sumByDescriptionKeywordInPeriod(any(), any(), any()))
                .thenReturn(null);

        // Act
        ProfitLossReportDTO report = profitLossReportService.generateProfitLossReport(startDate, endDate);

        // Assert
        assertNotNull(report);
        assertEquals(BigDecimal.ZERO, report.getRevenue().getTotalRevenue());
        assertEquals(BigDecimal.ZERO, report.getExpenses().getTotalExpenses());
        assertEquals(BigDecimal.ZERO, report.getNetProfitLoss());
    }
}
