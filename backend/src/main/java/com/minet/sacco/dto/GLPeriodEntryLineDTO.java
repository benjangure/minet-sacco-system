package com.minet.sacco.dto;

import java.math.BigDecimal;

public class GLPeriodEntryLineDTO {
  private Integer glAccountId;
  private String code;
  private String name;
  private String accountType;
  private String normalBalance;
  private String sectionLabel;
  private String sourceType; // AUTO or MANUAL
  private BigDecimal amount;
  private String periodStatus; // DRAFT, POSTED, APPROVED, LOCKED (for manual only)
  private Integer entryId; // for manual entries only
  private Boolean readOnly;
  
  public GLPeriodEntryLineDTO() {}
  
  public Integer getGlAccountId() { return glAccountId; }
  public void setGlAccountId(Integer glAccountId) { this.glAccountId = glAccountId; }
  
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  
  public String getAccountType() { return accountType; }
  public void setAccountType(String accountType) { this.accountType = accountType; }
  
  public String getNormalBalance() { return normalBalance; }
  public void setNormalBalance(String normalBalance) { this.normalBalance = normalBalance; }
  
  public String getSectionLabel() { return sectionLabel; }
  public void setSectionLabel(String sectionLabel) { this.sectionLabel = sectionLabel; }
  
  public String getSourceType() { return sourceType; }
  public void setSourceType(String sourceType) { this.sourceType = sourceType; }
  
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  
  public String getPeriodStatus() { return periodStatus; }
  public void setPeriodStatus(String periodStatus) { this.periodStatus = periodStatus; }
  
  public Integer getEntryId() { return entryId; }
  public void setEntryId(Integer entryId) { this.entryId = entryId; }
  
  public Boolean getReadOnly() { return readOnly; }
  public void setReadOnly(Boolean readOnly) { this.readOnly = readOnly; }
}
