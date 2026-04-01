package com.minet.sacco.service;

import com.minet.sacco.dto.*;
import com.minet.sacco.repository.LoanRepository;
import com.minet.sacco.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class ProfitLossReportService {

    private static final Logger logger = LoggerFactory.getLogger(ProfitLossReportService.class);

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    /**
     * Generate Profit & Loss Report for a given date range
     */
    public ProfitLossReportDTO generateProfitLossReport(LocalDate startDate, LocalDate endDate) {
        logger.info("Generating P&L Report for period: {} to {}", startDate, endDate);

        try {
            // Calculate revenue components
            BigDecimal interestFromLoans = calculateInterestIncome(startDate, endDate);
            BigDecimal interestFromSavings = BigDecimal.ZERO; // Future enhancement
            BigDecimal totalInterestIncome = interestFromLoans.add(interestFromSavings);

            BigDecimal loanProcessingFees = calculateLoanProcessingFees(startDate, endDate);
            BigDecimal accountMaintenanceFees = calculateAccountMaintenanceFees(startDate, endDate);
            BigDecimal otherFees = calculateOtherFees(startDate, endDate);
            BigDecimal totalFeesAndCharges = loanProcessingFees.add(accountMaintenanceFees).add(otherFees);

            BigDecimal otherIncome = calculateOtherIncome(startDate, endDate);

            BigDecimal totalRevenue = totalInterestIncome.add(totalFeesAndCharges).add(otherIncome);

            // Calculate expense components
            BigDecimal operatingExpensesTotal = calculateOperatingExpenses(startDate, endDate);
            BigDecimal loanLossProvisions = calculateLoanLossProvisions(startDate, endDate);
            BigDecimal otherExpenses = calculateOtherExpenses(startDate, endDate);

            BigDecimal totalExpenses = operatingExpensesTotal.add(loanLossProvisions).add(otherExpenses);

            // Calculate net profit/loss
            BigDecimal netProfitLoss = calculateNetProfitLoss(totalRevenue, totalExpenses);

            // Calculate profit margin
            BigDecimal profitMargin = calculateProfitMargin(netProfitLoss, totalRevenue);

            // Build DTOs
            RevenueDTO.InterestIncomeDTO interestIncomeDTO = new RevenueDTO.InterestIncomeDTO(
                    interestFromLoans, interestFromSavings, totalInterestIncome
            );

            RevenueDTO.FeesAndChargesDTO feesAndChargesDTO = new RevenueDTO.FeesAndChargesDTO(
                    loanProcessingFees, accountMaintenanceFees, otherFees, totalFeesAndCharges
            );

            RevenueDTO revenueDTO = new RevenueDTO(
                    interestIncomeDTO, feesAndChargesDTO, otherIncome, totalRevenue
            );

            ExpenseDTO.OperatingExpensesDTO operatingExpensesDTO = new ExpenseDTO.OperatingExpensesDTO(
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, operatingExpensesTotal, operatingExpensesTotal
            );

            ExpenseDTO.LoanLossProvisionsDTO loanLossProvisionsDTO = new ExpenseDTO.LoanLossProvisionsDTO(
                    loanLossProvisions, BigDecimal.ZERO, loanLossProvisions
            );

            ExpenseDTO expenseDTO = new ExpenseDTO(
                    operatingExpensesDTO, loanLossProvisionsDTO, otherExpenses, totalExpenses
            );

            PeriodDTO periodDTO = new PeriodDTO(startDate, endDate);

            ProfitLossReportDTO reportDTO = new ProfitLossReportDTO(
                    periodDTO, revenueDTO, expenseDTO, netProfitLoss, profitMargin
            );

            logger.info("P&L Report generated successfully. Total Revenue: {}, Total Expenses: {}, Net Profit/Loss: {}",
                    totalRevenue, totalExpenses, netProfitLoss);

            return reportDTO;
        } catch (Exception e) {
            logger.error("Error generating P&L Report", e);
            throw new RuntimeException("Failed to generate P&L Report: " + e.getMessage());
        }
    }

    /**
     * Calculate interest income from disbursed/repaid loans
     */
    public BigDecimal calculateInterestIncome(LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating interest income for period: {} to {}", startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        BigDecimal totalInterest = loanRepository.sumInterestIncomeInPeriod(startDateTime, endDateTime);

        logger.debug("Interest income calculated: {}", totalInterest);
        return totalInterest != null ? totalInterest : BigDecimal.ZERO;
    }

    /**
     * Calculate loan loss provisions from defaulted loans
     */
    public BigDecimal calculateLoanLossProvisions(LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating loan loss provisions for period: {} to {}", startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        BigDecimal totalProvisions = loanRepository.sumLoanLossProvisionsInPeriod(startDateTime, endDateTime);

        logger.debug("Loan loss provisions calculated: {}", totalProvisions);
        return totalProvisions != null ? totalProvisions : BigDecimal.ZERO;
    }

    /**
     * Calculate operating expenses from transactions
     */
    public BigDecimal calculateOperatingExpenses(LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating operating expenses for period: {} to {}", startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        BigDecimal salaries = transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "salary");
        BigDecimal rent = transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "rent");
        BigDecimal utilities = transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "utilities");
        BigDecimal other = transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "operational");

        BigDecimal totalExpenses = (salaries != null ? salaries : BigDecimal.ZERO)
                .add(rent != null ? rent : BigDecimal.ZERO)
                .add(utilities != null ? utilities : BigDecimal.ZERO)
                .add(other != null ? other : BigDecimal.ZERO);

        logger.debug("Operating expenses calculated: {}", totalExpenses);
        return totalExpenses;
    }

    /**
     * Calculate loan processing fees
     */
    public BigDecimal calculateLoanProcessingFees(LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating loan processing fees for period: {} to {}", startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        BigDecimal totalFees = transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "loan fee");

        logger.debug("Loan processing fees calculated: {}", totalFees);
        return totalFees != null ? totalFees : BigDecimal.ZERO;
    }

    /**
     * Calculate account maintenance fees
     */
    public BigDecimal calculateAccountMaintenanceFees(LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating account maintenance fees for period: {} to {}", startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        BigDecimal totalFees = transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "account maintenance");

        logger.debug("Account maintenance fees calculated: {}", totalFees);
        return totalFees != null ? totalFees : BigDecimal.ZERO;
    }

    /**
     * Calculate other fees
     */
    public BigDecimal calculateOtherFees(LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating other fees for period: {} to {}", startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        BigDecimal totalFees = transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "fee");

        logger.debug("Other fees calculated: {}", totalFees);
        return totalFees != null ? totalFees : BigDecimal.ZERO;
    }

    /**
     * Calculate other income
     */
    public BigDecimal calculateOtherIncome(LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating other income for period: {} to {}", startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        BigDecimal otherIncome = transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "miscellaneous income");

        logger.debug("Other income calculated: {}", otherIncome);
        return otherIncome != null ? otherIncome : BigDecimal.ZERO;
    }

    /**
     * Calculate other expenses
     */
    public BigDecimal calculateOtherExpenses(LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating other expenses for period: {} to {}", startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        BigDecimal otherExpenses = transactionRepository.sumByDescriptionKeywordInPeriod(startDateTime, endDateTime, "miscellaneous expense");

        logger.debug("Other expenses calculated: {}", otherExpenses);
        return otherExpenses != null ? otherExpenses : BigDecimal.ZERO;
    }

    /**
     * Calculate net profit/loss
     */
    public BigDecimal calculateNetProfitLoss(BigDecimal totalRevenue, BigDecimal totalExpenses) {
        logger.debug("Calculating net profit/loss. Revenue: {}, Expenses: {}", totalRevenue, totalExpenses);

        BigDecimal netProfitLoss = totalRevenue.subtract(totalExpenses);

        logger.debug("Net profit/loss calculated: {}", netProfitLoss);
        return netProfitLoss;
    }

    /**
     * Calculate profit margin percentage
     */
    public BigDecimal calculateProfitMargin(BigDecimal netProfitLoss, BigDecimal totalRevenue) {
        logger.debug("Calculating profit margin. Net P/L: {}, Revenue: {}", netProfitLoss, totalRevenue);

        if (totalRevenue.compareTo(BigDecimal.ZERO) == 0) {
            logger.debug("Revenue is zero, profit margin set to 0");
            return BigDecimal.ZERO;
        }

        BigDecimal profitMargin = netProfitLoss.divide(totalRevenue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP);

        logger.debug("Profit margin calculated: {}%", profitMargin);
        return profitMargin;
    }
}
