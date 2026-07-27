package com.minet.sacco.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "gl_manual_entries")
public class GLManualEntry implements Serializable {
  private static final long serialVersionUID = 1L;
  
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "gl_account_id", nullable = false)
  private GLAccount glAccount;
  
  @Column(nullable = false)
  private LocalDate entryDate;
  
  @Column(length = 500)
  private String description;
  
  @Column(nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;
  
  @Column(nullable = false)
  private Boolean isDebit;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EntryReason entryReason;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_user_id", nullable = false)
  private User createdByUser;
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_by_user_id")
  private User approvedByUser;
  
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
  
  private LocalDateTime approvedAt;
  
  @Column(nullable = true)
  private Integer periodMonth;
  
  @Column(nullable = true)
  private Integer periodYear;
  
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private PeriodStatus periodStatus = PeriodStatus.DRAFT;
  
  public enum EntryReason {
    ACCRUAL, ADJUSTMENT, ALLOCATION, RECLASSIFICATION
  }
  
  public enum ApprovalStatus {
    PENDING, APPROVED, REJECTED
  }
  
  public enum PeriodStatus {
    DRAFT, POSTED, APPROVED, LOCKED
  }
  
  // Constructors
  public GLManualEntry() {}
  
  public GLManualEntry(GLAccount glAccount, LocalDate entryDate, String description, 
                       BigDecimal amount, Boolean isDebit, EntryReason entryReason, User createdByUser) {
    this.glAccount = glAccount;
    this.entryDate = entryDate;
    this.description = description;
    this.amount = amount;
    this.isDebit = isDebit;
    this.entryReason = entryReason;
    this.createdByUser = createdByUser;
  }
  
  // Getters and Setters
  public Integer getId() { return id; }
  public void setId(Integer id) { this.id = id; }
  
  public GLAccount getGlAccount() { return glAccount; }
  public void setGlAccount(GLAccount glAccount) { this.glAccount = glAccount; }
  
  public LocalDate getEntryDate() { return entryDate; }
  public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }
  
  public String getDescription() { return description; }
  public void setDescription(String description) { this.description = description; }
  
  public BigDecimal getAmount() { return amount; }
  public void setAmount(BigDecimal amount) { this.amount = amount; }
  
  public Boolean getIsDebit() { return isDebit; }
  public void setIsDebit(Boolean isDebit) { this.isDebit = isDebit; }
  
  public EntryReason getEntryReason() { return entryReason; }
  public void setEntryReason(EntryReason entryReason) { this.entryReason = entryReason; }
  
  public ApprovalStatus getApprovalStatus() { return approvalStatus; }
  public void setApprovalStatus(ApprovalStatus approvalStatus) { this.approvalStatus = approvalStatus; }
  
  public User getCreatedByUser() { return createdByUser; }
  public void setCreatedByUser(User createdByUser) { this.createdByUser = createdByUser; }
  
  public User getApprovedByUser() { return approvedByUser; }
  public void setApprovedByUser(User approvedByUser) { this.approvedByUser = approvedByUser; }
  
  public LocalDateTime getCreatedAt() { return createdAt; }
  public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
  
  public LocalDateTime getApprovedAt() { return approvedAt; }
  public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
  
  public Integer getPeriodMonth() { return periodMonth; }
  public void setPeriodMonth(Integer periodMonth) { this.periodMonth = periodMonth; }
  
  public Integer getPeriodYear() { return periodYear; }
  public void setPeriodYear(Integer periodYear) { this.periodYear = periodYear; }
  
  public PeriodStatus getPeriodStatus() { return periodStatus; }
  public void setPeriodStatus(PeriodStatus periodStatus) { this.periodStatus = periodStatus; }
}
