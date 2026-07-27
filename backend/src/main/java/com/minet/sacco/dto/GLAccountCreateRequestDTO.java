package com.minet.sacco.dto;

public class GLAccountCreateRequestDTO {
  private String code;
  private String name;
  private String accountType; // ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE
  private String balanceCalculationType; // AGGREGATION, FORMULA, MANUAL_ENTRY, COMPUTED
  private String normalBalance; // DEBIT, CREDIT
  private String sectionLabel;
  private Boolean periodSensitive;
  private Integer displayOrder;
  private String dataSource; // LOANS, SAVINGS, SHARES, TRANSACTIONS
  private Integer loanProductId;
  
  public GLAccountCreateRequestDTO() {}
  
  public String getCode() { return code; }
  public void setCode(String code) { this.code = code; }
  
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  
  public String getAccountType() { return accountType; }
  public void setAccountType(String accountType) { this.accountType = accountType; }
  
  public String getBalanceCalculationType() { return balanceCalculationType; }
  public void setBalanceCalculationType(String balanceCalculationType) { 
    this.balanceCalculationType = balanceCalculationType; 
  }
  
  public String getNormalBalance() { return normalBalance; }
  public void setNormalBalance(String normalBalance) { this.normalBalance = normalBalance; }
  
  public String getSectionLabel() { return sectionLabel; }
  public void setSectionLabel(String sectionLabel) { this.sectionLabel = sectionLabel; }
  
  public Boolean getPeriodSensitive() { return periodSensitive; }
  public void setPeriodSensitive(Boolean periodSensitive) { this.periodSensitive = periodSensitive; }
  
  public Integer getDisplayOrder() { return displayOrder; }
  public void setDisplayOrder(Integer displayOrder) { this.displayOrder = displayOrder; }
  
  public String getDataSource() { return dataSource; }
  public void setDataSource(String dataSource) { this.dataSource = dataSource; }
  
  public Integer getLoanProductId() { return loanProductId; }
  public void setLoanProductId(Integer loanProductId) { this.loanProductId = loanProductId; }
}
