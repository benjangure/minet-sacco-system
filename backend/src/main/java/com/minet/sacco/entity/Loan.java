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
    @Column(name = "original_principal")
    private BigDecimal originalPrincipal;

    @DecimalMin(value = "0.00")
    @Column(name = "original_amount")
    private BigDecimal originalAmount;  // Store original loan amount for reduction tracking

    @Column(name = "rejection_stage")
    private String rejectionStage;  // Track which stage rejected (GUARANTOR, LOAN_OFFICER, etc.)

    @DecimalMin(value = "0.00")
    private BigDecimal outstandingBalance;

    @Column(columnDefinition = "TEXT")
    private String purpose;
    private String rejectionReason;

    @Column(name = "member_eligibility_status", length = 20)
    private String memberEligibilityStatus;

    @Column(name = "member_eligibility_errors", columnDefinition = "TEXT")
    private String memberEligibilityErrors;

    @Column(name = "member_eligibility_warnings", columnDefinition = "TEXT")
    private String memberEligibilityWarnings;

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

    @Column(name = "migration_status")
    private String migrationStatus = "ACTIVE"; // ACTIVE or MIGRATED

    public enum Status {
        PENDING, 
        PENDING_GUARANTOR_APPROVAL, 
        PENDING_GUARANTOR_REPLACEMENT,
        PENDING_GUARANTOR_REASSIGNMENT,
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

    public BigDecimal getOriginalPrincipal() { return originalPrincipal; }
    public void setOriginalPrincipal(BigDecimal originalPrincipal) { this.originalPrincipal = originalPrincipal; }

    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }

    public String getRejectionStage() { return rejectionStage; }
    public void setRejectionStage(String rejectionStage) { this.rejectionStage = rejectionStage; }

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

    public String getMigrationStatus() { return migrationStatus; }
    public void setMigrationStatus(String migrationStatus) { this.migrationStatus = migrationStatus; }

    /**
     * Calculate loan repayment details based on amount, interest rate, and term
     * Uses simple interest formula: Interest = Principal × (Rate/100) × (Term/12)
     */
    public void calculateRepaymentDetails() {
        if (this.amount == null || this.interestRate == null || this.termMonths == null) {
            return;
        }

        // Set original principal - this never changes and is used for proportional calculations
        this.originalPrincipal = this.amount;

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