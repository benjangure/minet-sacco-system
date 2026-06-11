package com.minet.sacco.dto;

import java.math.BigDecimal;

/**
 * Validation information for Balance Sheet (Assets = Liabilities + Equity)
 */
public class BalanceSheetValidationDTO {
  private BigDecimal totalAssets;
  private BigDecimal totalLiabilitiesAndEquity;
  private Boolean isBalanced;
  
  public BalanceSheetValidationDTO() {}
  
  public BalanceSheetValidationDTO(BigDecimal totalAssets, BigDecimal totalLiabilitiesAndEquity, Boolean isBalanced) {
    this.totalAssets = totalAssets;
    this.totalLiabilitiesAndEquity = totalLiabilitiesAndEquity;
    this.isBalanced = isBalanced;
  }
  
  public BigDecimal getTotalAssets() { return totalAssets; }
  public void setTotalAssets(BigDecimal totalAssets) { this.totalAssets = totalAssets; }
  
  public BigDecimal getTotalLiabilitiesAndEquity() { return totalLiabilitiesAndEquity; }
  public void setTotalLiabilitiesAndEquity(BigDecimal totalLiabilitiesAndEquity) { this.totalLiabilitiesAndEquity = totalLiabilitiesAndEquity; }
  
  public Boolean getIsBalanced() { return isBalanced; }
  public void setIsBalanced(Boolean isBalanced) { this.isBalanced = isBalanced; }
}
