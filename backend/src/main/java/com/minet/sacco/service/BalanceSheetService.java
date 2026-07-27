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
public class BalanceSheetService {
  private static final Logger logger = LoggerFactory.getLogger(BalanceSheetService.class);
  
  @Autowired
  private GLCalculationService glCalculationService;
  
  @Autowired
  private GLAccountRepository glAccountRepository;
  
  /**
   * Generate Balance Sheet for a specific date
   * Assets = Liabilities + Equity
   */
  public BalanceSheetDTO generateBalanceSheet(LocalDate asOfDate) {
    return generateBalanceSheet(asOfDate, null, null);
  }
  
  /**
   * Generate Balance Sheet for a specific date with optional period filtering
   * Assets = Liabilities + Equity
   */
  public BalanceSheetDTO generateBalanceSheet(LocalDate asOfDate, Integer periodMonth, Integer periodYear) {
    if (asOfDate == null) {
      asOfDate = LocalDate.now();
    }
    
    // Default to current period if not specified
    if (periodMonth == null) {
      periodMonth = asOfDate.getMonthValue();
    }
    if (periodYear == null) {
      periodYear = asOfDate.getYear();
    }
    
    // Get all active GL accounts by type
    List<GLAccount> assets = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.ASSET);
    List<GLAccount> liabilities = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.LIABILITY);
    List<GLAccount> equity = glAccountRepository.findByAccountTypeAndIsActiveTrue(AccountType.EQUITY);
    
    // Calculate lines for each section
    List<BalanceSheetLineDTO> assetLines = calculateBalanceSheetLines(assets, asOfDate, periodMonth, periodYear);
    List<BalanceSheetLineDTO> liabilityLines = calculateBalanceSheetLines(liabilities, asOfDate, periodMonth, periodYear);
    List<BalanceSheetLineDTO> equityLines = calculateBalanceSheetLines(equity, asOfDate, periodMonth, periodYear);
    
    // Calculate totals
    BigDecimal totalAssets = assetLines.stream()
      .map(line -> line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalLiabilities = liabilityLines.stream()
      .map(line -> line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    BigDecimal totalEquity = equityLines.stream()
      .map(line -> line.getAmount() != null ? line.getAmount() : BigDecimal.ZERO)
      .reduce(BigDecimal.ZERO, BigDecimal::add);
    
    // Check if balanced: Assets = Liabilities + Equity
    BigDecimal leftSide = totalAssets;
    BigDecimal rightSide = totalLiabilities.add(totalEquity);
    Boolean isBalanced = leftSide.compareTo(rightSide) == 0;
    
    if (!isBalanced) {
      logger.warn("Balance Sheet not balanced for date " + asOfDate + ". Assets: " + leftSide + ", Liabilities+Equity: " + rightSide);
    }
    
    return new BalanceSheetDTO(
      asOfDate,
      assetLines, totalAssets,
      liabilityLines, totalLiabilities,
      equityLines, totalEquity,
      isBalanced
    );
  }
  
  /**
   * Calculate balance sheet lines for a list of GL accounts
   */
  private List<BalanceSheetLineDTO> calculateBalanceSheetLines(List<GLAccount> accounts, LocalDate asOfDate, 
                                                               Integer periodMonth, Integer periodYear) {
    return accounts.stream()
      .map(acc -> {
        BigDecimal balance = glCalculationService.calculateGLAccountBalance(acc.getId(), asOfDate, periodMonth, periodYear);
        return new BalanceSheetLineDTO(
          acc.getCode(),
          acc.getName(),
          balance
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
