package com.minet.sacco.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a member-initiated loan top-up request
 * Follows the same guarantee approval workflow as regular loan applications
 */
@Entity
@Table(name = "loan_topup_requests")
public class LoanTopUpRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Column(name = "purpose", columnDefinition = "TEXT")
    private String purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private Status status;

    @Column(name = "requested_date", nullable = false)
    private LocalDateTime requestedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "review_date")
    private LocalDateTime reviewDate;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disbursed_by")
    private User disbursedBy;

    @Column(name = "disbursement_date")
    private LocalDateTime disbursementDate;

    @OneToMany(mappedBy = "topUpRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TopUpGuarantor> guarantors = new ArrayList<>();

    @Column(name = "total_guarantee_amount", precision = 15, scale = 2)
    private BigDecimal totalGuaranteeAmount;

    @Column(name = "guarantor_approval_count")
    private Integer guarantorApprovalCount = 0;

    @Column(name = "guarantor_rejection_count")
    private Integer guarantorRejectionCount = 0;

    @Column(name = "all_guarantors_approved")
    private Boolean allGuarantorsApproved = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum Status {
        PENDING_GUARANTOR_APPROVAL,   // Waiting for guarantors to approve
        PENDING_LOAN_OFFICER_REVIEW,  // All guarantors approved, waiting for loan officer
        PENDING_CREDIT_COMMITTEE,     // Loan officer approved, waiting for credit committee
        PENDING_TREASURER,            // Credit committee approved, waiting for treasurer
        APPROVED,                     // Treasurer approved, ready for disbursement
        REJECTED,                     // Rejected at any stage
        DISBURSED,                    // Top-up has been disbursed
        CANCELLED,                    // Cancelled by member

        /**
         * Legacy alias kept for backward compatibility with existing DB rows.
         * New code must not produce this value; the migration renames it to
         * PENDING_LOAN_OFFICER_REVIEW.
         */
        @Deprecated
        PENDING_REVIEW
    }

    // Constructors
    public LoanTopUpRequest() {
        this.requestedDate = LocalDateTime.now();
        this.status = Status.PENDING_GUARANTOR_APPROVAL;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public LoanTopUpRequest(Loan loan, Member member, BigDecimal requestedAmount, String purpose) {
        this();
        this.loan = loan;
        this.member = member;
        this.requestedAmount = requestedAmount;
        this.purpose = purpose;
    }

    // Lifecycle callbacks
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Business logic methods
    public void addGuarantor(TopUpGuarantor guarantor) {
        guarantors.add(guarantor);
        guarantor.setTopUpRequest(this);
        recalculateGuaranteeAmounts();
    }

    public void removeGuarantor(TopUpGuarantor guarantor) {
        guarantors.remove(guarantor);
        guarantor.setTopUpRequest(null);
        recalculateGuaranteeAmounts();
    }

    public void recalculateGuaranteeAmounts() {
        this.totalGuaranteeAmount = guarantors.stream()
            .map(TopUpGuarantor::getGuaranteeAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.guarantorApprovalCount = (int) guarantors.stream()
            .filter(g -> g.getStatus() == TopUpGuarantor.Status.APPROVED)
            .count();

        this.guarantorRejectionCount = (int) guarantors.stream()
            .filter(g -> g.getStatus() == TopUpGuarantor.Status.REJECTED)
            .count();

        // Check if all guarantors have approved
        long pendingCount = guarantors.stream()
            .filter(g -> g.getStatus() == TopUpGuarantor.Status.PENDING)
            .count();

        this.allGuarantorsApproved = !guarantors.isEmpty() 
            && pendingCount == 0 
            && guarantorRejectionCount == 0
            && guarantorApprovalCount == guarantors.size();

        // Auto-update status if all approved
        if (this.allGuarantorsApproved && this.status == Status.PENDING_GUARANTOR_APPROVAL) {
            this.status = Status.PENDING_LOAN_OFFICER_REVIEW;
        }

        // Auto-reject if any guarantor rejected
        if (this.guarantorRejectionCount > 0 && this.status == Status.PENDING_GUARANTOR_APPROVAL) {
            this.status = Status.REJECTED;
            this.rejectionReason = "One or more guarantors rejected the top-up request";
        }
    }

    public boolean canBeApprovedByLoanOfficer() {
        return this.status == Status.PENDING_LOAN_OFFICER_REVIEW && this.allGuarantorsApproved;
    }

    public boolean canBeApprovedByCreditCommittee() {
        return this.status == Status.PENDING_CREDIT_COMMITTEE;
    }

    public boolean canBeApprovedByTreasurer() {
        return this.status == Status.PENDING_TREASURER;
    }

    public boolean canBeDisbursed() {
        return this.status == Status.APPROVED;
    }

    public boolean canBeCancelled() {
        return this.status == Status.PENDING_GUARANTOR_APPROVAL
            || this.status == Status.PENDING_LOAN_OFFICER_REVIEW
            || this.status == Status.PENDING_CREDIT_COMMITTEE
            || this.status == Status.PENDING_TREASURER;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
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

    public User getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(User reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public LocalDateTime getReviewDate() {
        return reviewDate;
    }

    public void setReviewDate(LocalDateTime reviewDate) {
        this.reviewDate = reviewDate;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public User getDisbursedBy() {
        return disbursedBy;
    }

    public void setDisbursedBy(User disbursedBy) {
        this.disbursedBy = disbursedBy;
    }

    public LocalDateTime getDisbursementDate() {
        return disbursementDate;
    }

    public void setDisbursementDate(LocalDateTime disbursementDate) {
        this.disbursementDate = disbursementDate;
    }

    public List<TopUpGuarantor> getGuarantors() {
        return guarantors;
    }

    public void setGuarantors(List<TopUpGuarantor> guarantors) {
        this.guarantors = guarantors;
    }

    public BigDecimal getTotalGuaranteeAmount() {
        return totalGuaranteeAmount;
    }

    public void setTotalGuaranteeAmount(BigDecimal totalGuaranteeAmount) {
        this.totalGuaranteeAmount = totalGuaranteeAmount;
    }

    public Integer getGuarantorApprovalCount() {
        return guarantorApprovalCount;
    }

    public void setGuarantorApprovalCount(Integer guarantorApprovalCount) {
        this.guarantorApprovalCount = guarantorApprovalCount;
    }

    public Integer getGuarantorRejectionCount() {
        return guarantorRejectionCount;
    }

    public void setGuarantorRejectionCount(Integer guarantorRejectionCount) {
        this.guarantorRejectionCount = guarantorRejectionCount;
    }

    public Boolean getAllGuarantorsApproved() {
        return allGuarantorsApproved;
    }

    public void setAllGuarantorsApproved(Boolean allGuarantorsApproved) {
        this.allGuarantorsApproved = allGuarantorsApproved;
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
}
