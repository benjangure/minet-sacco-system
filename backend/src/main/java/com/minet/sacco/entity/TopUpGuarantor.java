package com.minet.sacco.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a guarantor for a loan top-up request
 * Similar to regular Guarantor but for top-up requests
 */
@Entity
@Table(name = "topup_guarantors")
public class TopUpGuarantor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topup_request_id", nullable = false)
    private LoanTopUpRequest topUpRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "guarantee_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal guaranteeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private Status status;

    @Column(name = "requested_date", nullable = false)
    private LocalDateTime requestedDate;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "pledge_amount", precision = 15, scale = 2)
    private BigDecimal pledgeAmount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Next of Kin (NOK) Guarantor Support
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_of_kin_guarantor_id")
    private TopUpGuarantor nextOfKinGuarantor;  // The NOK backup for this primary guarantor

    @Column(name = "is_next_of_kin", nullable = false)
    private boolean isNextOfKin = false;  // True if this guarantor is a next of kin (backup)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_guarantor_id")
    private TopUpGuarantor primaryGuarantor;  // The primary guarantor this NOK is backing

    // Replacement tracking
    @Column(name = "replaced_at")
    private LocalDateTime replacedAt;

    @Column(name = "replaced_by_guarantor_id")
    private Long replacedByGuarantorId;

    @Column(name = "replacement_reason", length = 100)
    private String replacementReason;

    public enum Status {
        PENDING,    // Waiting for guarantor approval
        APPROVED,   // Guarantor approved
        REJECTED,   // Guarantor rejected
        REPLACED_DUE_TO_EXIT,  // Primary was replaced due to member exit
        ACTIVATED_FROM_NOK     // NOK was activated to replace primary
    }

    // Constructors
    public TopUpGuarantor() {
        this.status = Status.PENDING;
        this.requestedDate = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public TopUpGuarantor(LoanTopUpRequest topUpRequest, Member member, BigDecimal guaranteeAmount) {
        this();
        this.topUpRequest = topUpRequest;
        this.member = member;
        this.guaranteeAmount = guaranteeAmount;
        this.pledgeAmount = BigDecimal.ZERO; // Will be set upon approval/disbursement
    }

    // Lifecycle callbacks
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic methods
    public void approve() {
        if (this.status != Status.PENDING) {
            throw new IllegalStateException("Can only approve pending guarantor requests");
        }
        this.status = Status.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.pledgeAmount = this.guaranteeAmount; // Lock the pledge amount
        
        // Trigger recalculation on parent
        if (this.topUpRequest != null) {
            this.topUpRequest.recalculateGuaranteeAmounts();
        }
    }

    public void reject(String reason) {
        if (this.status != Status.PENDING) {
            throw new IllegalStateException("Can only reject pending guarantor requests");
        }
        this.status = Status.REJECTED;
        this.rejectedAt = LocalDateTime.now();
        this.rejectionReason = reason;
        
        // Trigger recalculation on parent
        if (this.topUpRequest != null) {
            this.topUpRequest.recalculateGuaranteeAmounts();
        }
    }

    public boolean isPending() {
        return this.status == Status.PENDING;
    }

    public boolean isApproved() {
        return this.status == Status.APPROVED;
    }

    public boolean isRejected() {
        return this.status == Status.REJECTED;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LoanTopUpRequest getTopUpRequest() {
        return topUpRequest;
    }

    public void setTopUpRequest(LoanTopUpRequest topUpRequest) {
        this.topUpRequest = topUpRequest;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public BigDecimal getGuaranteeAmount() {
        return guaranteeAmount;
    }

    public void setGuaranteeAmount(BigDecimal guaranteeAmount) {
        this.guaranteeAmount = guaranteeAmount;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getRequestedDate() {
        return requestedDate;
    }

    public void setRequestedDate(LocalDateTime requestedDate) {
        this.requestedDate = requestedDate;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(LocalDateTime rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public BigDecimal getPledgeAmount() {
        return pledgeAmount;
    }

    public void setPledgeAmount(BigDecimal pledgeAmount) {
        this.pledgeAmount = pledgeAmount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // NOK Guarantor Getters and Setters
    public TopUpGuarantor getNextOfKinGuarantor() { return nextOfKinGuarantor; }
    public void setNextOfKinGuarantor(TopUpGuarantor nextOfKinGuarantor) { this.nextOfKinGuarantor = nextOfKinGuarantor; }

    public boolean isNextOfKin() { return isNextOfKin; }
    public void setNextOfKin(boolean nextOfKin) { isNextOfKin = nextOfKin; }

    public TopUpGuarantor getPrimaryGuarantor() { return primaryGuarantor; }
    public void setPrimaryGuarantor(TopUpGuarantor primaryGuarantor) { this.primaryGuarantor = primaryGuarantor; }

    public LocalDateTime getReplacedAt() { return replacedAt; }
    public void setReplacedAt(LocalDateTime replacedAt) { this.replacedAt = replacedAt; }

    public Long getReplacedByGuarantorId() { return replacedByGuarantorId; }
    public void setReplacedByGuarantorId(Long replacedByGuarantorId) { this.replacedByGuarantorId = replacedByGuarantorId; }

    public String getReplacementReason() { return replacementReason; }
    public void setReplacementReason(String replacementReason) { this.replacementReason = replacementReason; }
}
