package com.minet.sacco.dto;

import java.math.BigDecimal;

/**
 * Summary totals for a trial balance (Debit = Credit verification)
 */
public class TrialBalanceSummaryDTO {
  private BigDecimal totalDebit;
  private BigDecimal totalCredit;
  private Boolean isBalanced;
  
  public TrialBalanceSummaryDTO() {}
  
  public TrialBalanceSummaryDTO(BigDecimal totalDebit, BigDecimal totalCredit, Boolean isBalanced) {
    this.totalDebit = totalDebit;
    this.totalCredit = totalCredit;
    this.isBalanced = isBalanced;
  }
  
  public BigDecimal getTotalDebit() { return totalDebit; }
  public void setTotalDebit(BigDecimal totalDebit) { this.totalDebit = totalDebit; }
  
  public BigDecimal getTotalCredit() { return totalCredit; }
  public void setTotalCredit(BigDecimal totalCredit) { this.totalCredit = totalCredit; }
  
  public Boolean getIsBalanced() { return isBalanced; }
  public void setIsBalanced(Boolean isBalanced) { this.isBalanced = isBalanced; }
}
