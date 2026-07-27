package com.minet.sacco.dto;

import java.math.BigDecimal;

/**
 * Represents a single line item in a balance sheet (Asset, Liability, or Equity)
 */
public class BalanceSheetLineDTO {
  private String code;
  private String name;
  private BigDecimal amount;
  
  public BalanceSheetLineDTO() {}
  
  public BalanceSheetLineDTO(String code, String name, BigDecimal amount) {
    this.code = code;
    this.name = name;
    this.amount = amount;
  }
  
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
}
