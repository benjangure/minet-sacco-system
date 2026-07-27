package com.minet.sacco.dto;

import java.math.BigDecimal;

/**
 * Represents a single line item in an income statement (Revenue or Expense)
 */
public class IncomeStatementLineDTO {
  private String code;
  private String name;
  private BigDecimal amount;
  
  public IncomeStatementLineDTO() {}
  
  public IncomeStatementLineDTO(String code, String name, BigDecimal amount) {
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
