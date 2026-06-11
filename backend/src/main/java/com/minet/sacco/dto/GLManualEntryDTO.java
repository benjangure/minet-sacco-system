package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class GLManualEntryDTO {
  private Integer id;
  private Integer glAccountId;
  private String glAccountCode;
  private String glAccountName;
  private LocalDate entryDate;
  private String description;
  private BigDecimal amount;
  private Boolean isDebit;
  private String entryReason;
  private String approvalStatus;
  private String createdByUserName;
  private String approvedByUserName;
  private LocalDateTime createdAt;
  private LocalDateTime approvedAt;
  private String periodStatus;
  private String workflowStatus;
  private String entrySource;
  private Integer periodMonth;
  private Integer periodYear;

  public GLManualEntryDTO() {}

  public GLManualEntryDTO(Integer id, Integer glAccountId, String glAccountCode, String glAccountName,
                         LocalDate entryDate, String description, BigDecimal amount, Boolean isDebit,
                         String entryReason, String approvalStatus, String createdByUserName,
                         String approvedByUserName, LocalDateTime createdAt, LocalDateTime approvedAt,
                         String periodStatus, String workflowStatus, String entrySource,
                         Integer periodMonth, Integer periodYear) {
    this.id = id;
    this.glAccountId = glAccountId;
    this.glAccountCode = glAccountCode;
    this.glAccountName = glAccountName;
    this.entryDate = entryDate;
    this.description = description;
    this.amount = amount;
    this.isDebit = isDebit;
    this.entryReason = entryReason;
    this.approvalStatus = approvalStatus;
    this.createdByUserName = createdByUserName;
    this.approvedByUserName = approvedByUserName;
    this.createdAt = createdAt;
    this.approvedAt = approvedAt;
    this.periodStatus = periodStatus;
    this.workflowStatus = workflowStatus;
    this.entrySource = entrySource;
    this.periodMonth = periodMonth;
    this.periodYear = periodYear;
  }

  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }

  public Integer getGlAccountId() { return glAccountId; }
  public void setGlAccountId(Integer glAccountId) { this.glAccountId = glAccountId; }

  public String getGlAccountCode() { return glAccountCode; }
  public void setGlAccountCode(String glAccountCode) { this.glAccountCode = glAccountCode; }

  public String getGlAccountName() { return glAccountName; }
  public void setGlAccountName(String glAccountName) { this.glAccountName = glAccountName; }

  public LocalDate getEntryDate() { return entryDate; }
  public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }

  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }

  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }

  public Boolean getIsDebit() { return isDebit; }
  public void setIsDebit(Boolean isDebit) { this.isDebit = isDebit; }

  public String getEntryReason() { return entryReason; }
  public void setEntryReason(String entryReason) { this.entryReason = entryReason; }

  public String getApprovalStatus() { return approvalStatus; }
  public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

  public String getCreatedByUserName() { return createdByUserName; }
  public void setCreatedByUserName(String createdByUserName) { this.createdByUserName = createdByUserName; }

  public String getApprovedByUserName() { return approvedByUserName; }
  public void setApprovedByUserName(String approvedByUserName) { this.approvedByUserName = approvedByUserName; }

  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

  public LocalDateTime getApprovedAt() { return approvedAt; }
  public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

  public String getPeriodStatus() { return periodStatus; }
  public void setPeriodStatus(String periodStatus) { this.periodStatus = periodStatus; }

  public String getWorkflowStatus() { return workflowStatus; }
  public void setWorkflowStatus(String workflowStatus) { this.workflowStatus = workflowStatus; }

  public String getEntrySource() { return entrySource; }
  public void setEntrySource(String entrySource) { this.entrySource = entrySource; }

  public Integer getPeriodMonth() { return periodMonth; }
  public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }

  public Integer getPeriodYear() { return periodYear; }
  public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
}
