package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GLManualEntryRequest {
  private Integer glAccountId;
  private LocalDate entryDate;
  private String description;
  private BigDecimal amount;
  private Boolean isDebit;
  private String entryReason; // ACCRUAL, ADJUSTMENT, ALLOCATION, RECLASSIFICATION

  public GLManualEntryRequest() {}

  public GLManualEntryRequest(Integer glAccountId, LocalDate entryDate, String description,
                             BigDecimal amount, Boolean isDebit, String entryReason) {
    this.glAccountId = glAccountId;
    this.entryDate = entryDate;
    this.description = description;
    this.amount = amount;
    this.isDebit = isDebit;
    this.entryReason = entryReason;
  }

  public Integer getGlAccountId() { return glAccountId; }
  public void setGlAccountId(Integer glAccountId) { this.glAccountId = glAccountId; }

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
}
