package com.minet.sacco.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "guarantors")
public class Guarantor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "loan_id")
    private Loan loan;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @DecimalMin(value = "0.00")
    private BigDecimal pledgeAmount;

    @DecimalMin(value = "0.00")
    private BigDecimal guaranteeAmount;  // Amount this guarantor is pledging

    @DecimalMin(value = "0.00")
    private BigDecimal previousGuaranteeAmount;  // Previous amount before reassignment

    private String reassignmentReason;  // Reason for reassignment

    @Column(name = "self_guarantee", nullable = false)
    private boolean selfGuarantee = false;  // True if member is self-guaranteeing

    private String rejectionReason;

    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;

    @Column(name = "migration_status")
    private String migrationStatus = "ACTIVE"; // ACTIVE or MIGRATED

    @Column(name = "pledge_frozen_at_full_amount", nullable = true)
    private Boolean pledgeFrozenAtFullAmount = false;  // true = manually set (don't apply reduction ratio), false = frozen at principal

    // Next of Kin (NOK) Guarantor Support
    @ManyToOne
    @JoinColumn(name = "next_of_kin_guarantor_id")
    private Guarantor nextOfKinGuarantor;  // The NOK backup for this primary guarantor

    @Column(name = "is_next_of_kin", nullable = false)
    private boolean isNextOfKin = false;  // True if this guarantor is a next of kin (backup)

    @ManyToOne
    @JoinColumn(name = "primary_guarantor_id")
    private Guarantor primaryGuarantor;  // The primary guarantor this NOK is backing

    // Replacement tracking
    @Column(name = "replaced_at")
    private LocalDateTime replacedAt;

    @Column(name = "replaced_by_guarantor_id")
    private Long replacedByGuarantorId;

    @Column(name = "replacement_reason", length = 100)
    private String replacementReason;

    public enum Status {
        PENDING, ACCEPTED, REJECTED, REPLACED, ACTIVE, DECLINED, RELEASED, PENDING_REASSIGNMENT,
        REPLACED_DUE_TO_EXIT, ACTIVATED_FROM_NOK
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public BigDecimal getPledgeAmount() { return pledgeAmount; }
    public void setPledgeAmount(BigDecimal pledgeAmount) { this.pledgeAmount = pledgeAmount; }

    public BigDecimal getGuaranteeAmount() { return guaranteeAmount; }
    public void setGuaranteeAmount(BigDecimal guaranteeAmount) { this.guaranteeAmount = guaranteeAmount; }

    public BigDecimal getPreviousGuaranteeAmount() { return previousGuaranteeAmount; }
    public void setPreviousGuaranteeAmount(BigDecimal previousGuaranteeAmount) { this.previousGuaranteeAmount = previousGuaranteeAmount; }

    public String getReassignmentReason() { return reassignmentReason; }
    public void setReassignmentReason(String reassignmentReason) { this.reassignmentReason = reassignmentReason; }

    public boolean isSelfGuarantee() { return selfGuarantee; }
    public void setSelfGuarantee(boolean selfGuarantee) { this.selfGuarantee = selfGuarantee; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getMigrationStatus() { return migrationStatus; }
    public void setMigrationStatus(String migrationStatus) { this.migrationStatus = migrationStatus; }

    public Boolean getPledgeFrozenAtFullAmount() { return pledgeFrozenAtFullAmount; }
    public void setPledgeFrozenAtFullAmount(Boolean pledgeFrozenAtFullAmount) { this.pledgeFrozenAtFullAmount = pledgeFrozenAtFullAmount; }

    // NOK Guarantor Getters and Setters
    public Guarantor getNextOfKinGuarantor() { return nextOfKinGuarantor; }
    public void setNextOfKinGuarantor(Guarantor nextOfKinGuarantor) { this.nextOfKinGuarantor = nextOfKinGuarantor; }

    public boolean isNextOfKin() { return isNextOfKin; }
    public void setNextOfKin(boolean nextOfKin) { isNextOfKin = nextOfKin; }

    public Guarantor getPrimaryGuarantor() { return primaryGuarantor; }
    public void setPrimaryGuarantor(Guarantor primaryGuarantor) { this.primaryGuarantor = primaryGuarantor; }

    public LocalDateTime getReplacedAt() { return replacedAt; }
    public void setReplacedAt(LocalDateTime replacedAt) { this.replacedAt = replacedAt; }

    public Long getReplacedByGuarantorId() { return replacedByGuarantorId; }
    public void setReplacedByGuarantorId(Long replacedByGuarantorId) { this.replacedByGuarantorId = replacedByGuarantorId; }

    public String getReplacementReason() { return replacementReason; }
    public void setReplacementReason(String replacementReason) { this.replacementReason = replacementReason; }
}
