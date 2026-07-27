package com.minet.sacco.dto;

import java.math.BigDecimal;

/**
 * Represents a single account line in a trial balance
 */
public class TrialBalanceLineDTO {
  private String code;
  private String name;
  private String accountType;
  private BigDecimal balance;
  private Boolean isDebit;
  
  public TrialBalanceLineDTO() {}
  
  public TrialBalanceLineDTO(String code, String name, String accountType, BigDecimal balance, Boolean isDebit) {
    this.code = code;
    this.name = name;
    this.accountType = accountType;
    this.balance = balance;
    this.isDebit = isDebit;
  }
  
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  
  public String getAccountType() { return accountType; }
  public void setAccountType(String accountType) { this.accountType = accountType; }
  
  public BigDecimal getBalance() { return balance; }
  public void setBalance(BigDecimal balance) { this.balance = balance; }
  
  public Boolean getIsDebit() { return isDebit; }
  public void setIsDebit(Boolean isDebit) { this.isDebit = isDebit; }
}
