package com.minet.sacco.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "batch_deletion_audit")
public class BatchDeletionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long batchId;

    @Column(length = 100)
    private String batchNumber;

    @Column(length = 50)
    private String batchType;

    @Column(nullable = false)
    private Long deletedByUserId;

    @Column(length = 100)
    private String deletedByUsername;

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime deletedAt;

    @Column(length = 500)
    private String reason;

    @Column(length = 20)
    private String rollbackStatus = "COMPLETED";

    @Column
    private Integer loansDeleted = 0;

    @Column
    private Integer guarantorsReleased = 0;

    @Column
    private Integer transactionsReversed = 0;

    @Column
    private Integer accountsAdjusted = 0;

    @Column(length = 1000)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (deletedAt == null) {
            deletedAt = LocalDateTime.now();
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBatchId() { return batchId; }
    public void setBatchId(Long batchId) { this.batchId = batchId; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public String getBatchType() { return batchType; }
    public void setBatchType(String batchType) { this.batchType = batchType; }

    public Long getDeletedByUserId() { return deletedByUserId; }
    public void setDeletedByUserId(Long deletedByUserId) { this.deletedByUserId = deletedByUserId; }

    public String getDeletedByUsername() { return deletedByUsername; }
    public void setDeletedByUsername(String deletedByUsername) { this.deletedByUsername = deletedByUsername; }

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRollbackStatus() { return rollbackStatus; }
    public void setRollbackStatus(String rollbackStatus) { this.rollbackStatus = rollbackStatus; }

    public Integer getLoansDeleted() { return loansDeleted; }
    public void setLoansDeleted(Integer loansDeleted) { this.loansDeleted = loansDeleted; }

    public Integer getGuarantorsReleased() { return guarantorsReleased; }
    public void setGuarantorsReleased(Integer guarantorsReleased) { this.guarantorsReleased = guarantorsReleased; }

    public Integer getTransactionsReversed() { return transactionsReversed; }
    public void setTransactionsReversed(Integer transactionsReversed) { this.transactionsReversed = transactionsReversed; }

    public Integer getAccountsAdjusted() { return accountsAdjusted; }
    public void setAccountsAdjusted(Integer accountsAdjusted) { this.accountsAdjusted = accountsAdjusted; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
