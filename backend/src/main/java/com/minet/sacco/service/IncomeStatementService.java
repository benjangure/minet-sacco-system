package com.minet.sacco.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.minet.sacco.dto.*;
import com.minet.sacco.entity.GLAccount;
import com.minet.sacco.entity.GLAccount.AccountType;
import com.minet.sacco.repository.GLAccountRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class IncomeStatementService {
  private static final Logger logger = LoggerFactory.getLogger(IncomeStatementService.class);
  
  @Autowired
  private GLCalculationService glCalculationService;
  
  @Autowired
  private GLAccountRepository glAccountRepository;
  
  /**
   * Generate Income Statement for a date range
   * Net Income = Total Revenues - Total Expenses
   */
  public IncomeStatementDTO generateIncomeStatement(LocalDate fromDate, LocalDate toDate) {
    return generateIncomeStatement(fromDate, toDate, null, null);
  }
  
  /**
   * Generate Income Statement for a date range with optional period filtering
   * Net Income = Total Revenues - Total Expenses
   */
  public IncomeStatementDTO generateIncomeStatement(LocalDate fromDate, LocalDate toDate, 
                                                    Integer periodMonth, Integer periodYear) {
    if (toDate == null) {
      toDate = LocalDate.now();
    }
    if (fromDate == null) {
      // Default to beginning of current month
      fromDate = toDate.withDayOfMonth(1);
    }
    
    // Default to current period if not specified
    if (periodMonth == null) {
      periodMonth = toDate.getMonthValue();
    }
    if (periodYear == null) {
      periodYear = toDate.getYear();
    }
    
    // Get all revenue and expense GL accounts
    List<GLAccount> revenues = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.REVENUE);
    List<GLAccount> expenses = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.EXPENSE);
    
    // Calculate lines
    List<IncomeStatementLineDTO> revenueLines = calculateIncomeStatementLines(revenues, toDate, periodMonth, periodYear);
    List<IncomeStatementLineDTO> expenseLines = calculateIncomeStatementLines(expenses, toDate, periodMonth, periodYear);
    
    // Calculate totals
    BigDecimal totalRevenues = revenueLines.stream()
      .map(line -> line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalExpenses = expenseLines.stream()
      .map(line -> line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    // Calculate net income
    BigDecimal netIncome = totalRevenues.subtract(totalExpenses);
    
    logger.info("Income Statement generated: From=" + fromDate + ", To=" + toDate + 
             ", Revenue=" + totalRevenues + ", Expenses=" + totalExpenses + ", NetIncome=" + netIncome);
    
    return new IncomeStatementDTO(
      fromDate,
      toDate,
      revenueLines,
      totalRevenues,
      expenseLines,
      totalExpenses,
      netIncome
    );
  }
  
  /**
   * Calculate income statement lines for a list of GL accounts
   */
  private List<IncomeStatementLineDTO> calculateIncomeStatementLines(List<GLAccount> accounts, LocalDate asOfDate,
                                                                      Integer periodMonth, Integer periodYear) {
    return accounts.stream()
      .map(acc -> {
        BigDecimal balance = glCalculationService.calculateGLAccountBalance(acc.getId(), asOfDate, periodMonth, periodYear);
        return new IncomeStatementLineDTO(
          acc.getCode(),
          acc.getName(),
          balance.abs()  // Show absolute value
        );
      })
      .filter(line -> line.getAmount() != null && line.getAmount().compareTo(BigDecimal.ZERO) != 0)
      .sorted((a, b) -> {
        // Sort by amount descending
        return b.getAmount().compareTo(a.getAmount());
      })
      .collect(Collectors.toList());
  }
}
