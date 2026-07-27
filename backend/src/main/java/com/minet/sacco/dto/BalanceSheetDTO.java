package com.minet.sacco.dto;

import java.time.LocalDate;
import java.util.List;
import java.math.BigDecimal;

public class BalanceSheetDTO {
  private LocalDate asOfDate;
  
  // Assets
  private List<BalanceSheetLineDTO> assets;
  private BigDecimal totalAssets;
  
  // Liabilities
  private List<BalanceSheetLineDTO> liabilities;
  private BigDecimal totalLiabilities;
  
  // Equity
  private List<BalanceSheetLineDTO> equity;
  private BigDecimal totalEquity;
  
  // Verification
  private Boolean isBalanced;
  private BalanceSheetValidationDTO validation;
  
  public BalanceSheetDTO() {}
  
  public BalanceSheetDTO(LocalDate asOfDate,
                         List<BalanceSheetLineDTO> assets, BigDecimal totalAssets,
                         List<BalanceSheetLineDTO> liabilities, BigDecimal totalLiabilities,
                         List<BalanceSheetLineDTO> equity, BigDecimal totalEquity,
                         Boolean isBalanced) {
    this.asOfDate = asOfDate;
    this.assets = assets;
    this.totalAssets = totalAssets;
    this.liabilities = liabilities;
    this.totalLiabilities = totalLiabilities;
    this.equity = equity;
    this.totalEquity = totalEquity;
    this.isBalanced = isBalanced;
    this.validation = new BalanceSheetValidationDTO(
      totalAssets,
      totalLiabilities.add(totalEquity),
      isBalanced
    );
  }
  
  public LocalDate getAsOfDate() { return asOfDate; }
  public void setAsOfDate(LocalDate asOfDate) { this.asOfDate = asOfDate; }
  
  public List<BalanceSheetLineDTO> getAssets() { return assets; }
  public void setAssets(List<BalanceSheetLineDTO> assets) { this.assets = assets; }
  
  public BigDecimal getTotalAssets() { return totalAssets; }
  public void setTotalAssets(BigDecimal totalAssets) { this.totalAssets = totalAssets; }
  
  public List<BalanceSheetLineDTO> getLiabilities() { return liabilities; }
  public void setLiabilities(List<BalanceSheetLineDTO> liabilities) { this.liabilities = liabilities; }
  
  public BigDecimal getTotalLiabilities() { return totalLiabilities; }
  public void setTotalLiabilities(BigDecimal totalLiabilities) { this.totalLiabilities = totalLiabilities; }
  
  public List<BalanceSheetLineDTO> getEquity() { return equity; }
  public void setEquity(List<BalanceSheetLineDTO> equity) { this.equity = equity; }
  
  public BigDecimal getTotalEquity() { return totalEquity; }
  public void setTotalEquity(BigDecimal totalEquity) { this.totalEquity = totalEquity; }
  
  public Boolean getIsBalanced() { return isBalanced; }
  public void setIsBalanced(Boolean isBalanced) { this.isBalanced = isBalanced; }
  
  public BalanceSheetValidationDTO getValidation() { return validation; }
  public void setValidation(BalanceSheetValidationDTO validation) { this.validation = validation; }
}
