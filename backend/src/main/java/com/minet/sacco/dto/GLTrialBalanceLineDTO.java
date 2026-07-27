package com.minet.sacco.dto;

import java.math.BigDecimal;

public class GLTrialBalanceLineDTO {
  private String code;
  private String name;
  private BigDecimal debitAmount;
  private BigDecimal creditAmount;
  private String sourceType; // AUTO or MANUAL
  
  public GLTrialBalanceLineDTO() {}
  
  public GLTrialBalanceLineDTO(String code, String name, BigDecimal debitAmount, 
                               BigDecimal creditAmount, String sourceType) {
    this.code = code;
    this.name = name;
    this.debitAmount = debitAmount;
    this.creditAmount = creditAmount;
    this.sourceType = sourceType;
  }
  
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  
  public BigDecimal getDebitAmount() { return debitAmount; }
  public void setDebitAmount(BigDecimal debitAmount) { this.debitAmount = debitAmount; }
  
  public BigDecimal getCreditAmount() { return creditAmount; }
  public void setCreditAmount(BigDecimal creditAmount) { this.creditAmount = creditAmount; }
  
  public String getSourceType() { return sourceType; }
  public void setSourceType(String sourceType) { this.sourceType = sourceType; }
}
