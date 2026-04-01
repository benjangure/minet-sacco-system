package com.minet.sacco.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 50)
    private String loanNumber;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "loan_product_id")
    private LoanProduct loanProduct;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal amount;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal interestRate;

    @NotNull
    @Min(1)
    private Integer termMonths;

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @DecimalMin(value = "0.00")
    private BigDecimal monthlyRepayment;

    @DecimalMin(value = "0.00")
    private BigDecimal totalInterest;

    @DecimalMin(value = "0.00")
    private BigDecimal totalRepayable;

    @DecimalMin(value = "0.00")
    private BigDecimal outstandingBalance;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Column(columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "member_eligibility_status", length = 20)
    private String memberEligibilityStatus;

    @Column(name = "member_eligibility_errors", columnDefinition = "TEXT")
    private String memberEligibilityErrors;

    @Column(name = "member_eligibility_warnings", columnDefinition = "TEXT")
    private String memberEligibilityWarnings;

    @Column(name = "guarantor1_eligibility_status", length = 20)
    private String guarantor1EligibilityStatus;

    @Column(name = "guarantor1_eligibility_errors", columnDefinition = "TEXT")
    private String guarantor1EligibilityErrors;

    @Column(name = "guarantor2_eligibility_status", length = 20)
    private String guarantor2EligibilityStatus;

    @Column(name = "guarantor2_eligibility_errors", columnDefinition = "TEXT")
    private String guarantor2EligibilityErrors;

    @Column(name = "guarantor3_eligibility_status", length = 20)
    private String guarantor3EligibilityStatus;

    @Column(name = "guarantor3_eligibility_errors", columnDefinition = "TEXT")
    private String guarantor3EligibilityErrors;

    private LocalDateTime applicationDate;
    private LocalDateTime approvalDate;
    private LocalDateTime disbursementDate;

    @ManyToOne
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @ManyToOne
    @JoinColumn(name = "disbursed_by")
    private User disbursedBy;

    public enum Status {
        PENDING, 
        PENDING_GUARANTOR_APPROVAL, 
        PENDING_LOAN_OFFICER_REVIEW, 
        PENDING_CREDIT_COMMITTEE, 
        PENDING_TREASURER, 
        APPROVED, 
        REJECTED, 
        DISBURSED, 
        REPAID, 
        DEFAULTED
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLoanNumber() { return loanNumber; }
    public void setLoanNumber(String loanNumber) { this.loanNumber = loanNumber; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public LoanProduct getLoanProduct() { return loanProduct; }
    public void setLoanProduct(LoanProduct loanProduct) { this.loanProduct = loanProduct; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }

    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public BigDecimal getMonthlyRepayment() { return monthlyRepayment; }
    public void setMonthlyRepayment(BigDecimal monthlyRepayment) { this.monthlyRepayment = monthlyRepayment; }

    public BigDecimal getTotalInterest() { return totalInterest; }
    public void setTotalInterest(BigDecimal totalInterest) { this.totalInterest = totalInterest; }

    public BigDecimal getTotalRepayable() { return totalRepayable; }
    public void setTotalRepayable(BigDecimal totalRepayable) { this.totalRepayable = totalRepayable; }

    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }

    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public String getMemberEligibilityStatus() { return memberEligibilityStatus; }
    public void setMemberEligibilityStatus(String memberEligibilityStatus) { this.memberEligibilityStatus = memberEligibilityStatus; }
    public String getMemberEligibilityErrors() { return memberEligibilityErrors; }
    public void setMemberEligibilityErrors(String memberEligibilityErrors) { this.memberEligibilityErrors = memberEligibilityErrors; }
    public String getMemberEligibilityWarnings() { return memberEligibilityWarnings; }
    public void setMemberEligibilityWarnings(String memberEligibilityWarnings) { this.memberEligibilityWarnings = memberEligibilityWarnings; }

    public String getGuarantor1EligibilityStatus() { return guarantor1EligibilityStatus; }
    public void setGuarantor1EligibilityStatus(String guarantor1EligibilityStatus) { this.guarantor1EligibilityStatus = guarantor1EligibilityStatus; }
    public String getGuarantor1EligibilityErrors() { return guarantor1EligibilityErrors; }
    public void setGuarantor1EligibilityErrors(String guarantor1EligibilityErrors) { this.guarantor1EligibilityErrors = guarantor1EligibilityErrors; }

    public String getGuarantor2EligibilityStatus() { return guarantor2EligibilityStatus; }
    public void setGuarantor2EligibilityStatus(String guarantor2EligibilityStatus) { this.guarantor2EligibilityStatus = guarantor2EligibilityStatus; }
    public String getGuarantor2EligibilityErrors() { return guarantor2EligibilityErrors; }
    public void setGuarantor2EligibilityErrors(String guarantor2EligibilityErrors) { this.guarantor2EligibilityErrors = guarantor2EligibilityErrors; }

    public String getGuarantor3EligibilityStatus() { return guarantor3EligibilityStatus; }
    public void setGuarantor3EligibilityStatus(String guarantor3EligibilityStatus) { this.guarantor3EligibilityStatus = guarantor3EligibilityStatus; }
    public String getGuarantor3EligibilityErrors() { return guarantor3EligibilityErrors; }
    public void setGuarantor3EligibilityErrors(String guarantor3EligibilityErrors) { this.guarantor3EligibilityErrors = guarantor3EligibilityErrors; }

    public LocalDateTime getApplicationDate() { return applicationDate; }
    public void setApplicationDate(LocalDateTime applicationDate) { this.applicationDate = applicationDate; }

    public LocalDateTime getApprovalDate() { return approvalDate; }
    public void setApprovalDate(LocalDateTime approvalDate) { this.approvalDate = approvalDate; }

    public LocalDateTime getDisbursementDate() { return disbursementDate; }
    public void setDisbursementDate(LocalDateTime disbursementDate) { this.disbursementDate = disbursementDate; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User createdBy) { this.createdBy = createdBy; }

    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }

    public User getDisbursedBy() { return disbursedBy; }
    public void setDisbursedBy(User disbursedBy) { this.disbursedBy = disbursedBy; }

    /**
     * Calculate loan repayment details based on amount, interest rate, and term
     * Uses simple interest formula: Interest = Principal × (Rate/100) × (Term/12)
     */
    public void calculateRepaymentDetails() {
        if (this.amount == null || this.interestRate == null || this.termMonths == null) {
            return;
        }

        // Simple interest calculation: Interest = Principal × Rate × Time
        // Rate is annual, so we convert to decimal and multiply by time in years
        BigDecimal rate = this.interestRate.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
        BigDecimal timeInYears = new BigDecimal(this.termMonths).divide(new BigDecimal("12"), 4, java.math.RoundingMode.HALF_UP);
        this.totalInterest = this.amount.multiply(rate).multiply(timeInYears).setScale(2, java.math.RoundingMode.HALF_UP);

        // Calculate total repayable: amount + totalInterest
        this.totalRepayable = this.amount.add(this.totalInterest);

        // Calculate monthly repayment: totalRepayable / termMonths
        this.monthlyRepayment = this.totalRepayable.divide(
            new BigDecimal(this.termMonths),
            2,
            java.math.RoundingMode.HALF_UP
        );
    }
}