package com.minet.sacco.dto;

import java.math.BigDecimal;

public class GLPeriodEntryRequestDTO {
  private Integer glAccountId;
  private BigDecimal amount;
  private Integer periodMonth;
  private Integer periodYear;
  private String description;
  private String entryReason; // ACCRUAL, ADJUSTMENT, ALLOCATION, RECLASSIFICATION
  private Boolean isDebit;   // optional override; if null, inferred from account normalBalance
  
  public GLPeriodEntryRequestDTO() {}
  
  public Integer getGlAccountId() { return glAccountId; }
  public void setGlAccountId(Integer glAccountId) { this.glAccountId = glAccountId; }
  
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  
  public Integer getPeriodMonth() { return periodMonth; }
  public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }
  
  public Integer getPeriodYear() { return periodYear; }
  public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
  
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  
  public String getEntryReason() { return entryReason; }
  public void setEntryReason(String entryReason) { this.entryReason = entryReason; }

  public Boolean getIsDebit() { return isDebit; }
  public void setIsDebit(Boolean isDebit) { this.isDebit = isDebit; }
}
