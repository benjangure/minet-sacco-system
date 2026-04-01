package com.minet.sacco.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_eligibility_rules")
public class LoanEligibilityRules {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Member eligibility rules
    @Column(name = "min_member_savings", nullable = false)
    private BigDecimal minMemberSavings = new BigDecimal("10000"); // KES 10,000

    @Column(name = "min_member_shares", nullable = false)
    private BigDecimal minMemberShares = new BigDecimal("5000"); // KES 5,000 - DEPRECATED: Shares no longer count for eligibility

    @Column(name = "min_savings_to_loan_ratio", nullable = false)
    private BigDecimal minSavingsToLoanRatio = new BigDecimal("0.20"); // 20% of loan (SAVINGS ONLY - not shares)

    @Column(name = "max_outstanding_to_savings_ratio", nullable = false)
    private BigDecimal maxOutstandingToSavingsRatio = new BigDecimal("0.50"); // 50% of savings

    @Column(name = "max_active_loans", nullable = false)
    private Integer maxActiveLoans = 3;

    // Maximum loan amount as multiple of savings (Kenyan SACCO requirement: 3x savings)
    @Column(name = "max_loan_to_savings_multiplier", nullable = false)
    private BigDecimal maxLoanToSavingsMultiplier = new BigDecimal("3.0"); // 3x savings maximum

    // Guarantor eligibility rules
    @Column(name = "min_guarantor_savings", nullable = false)
    private BigDecimal minGuarantorSavings = new BigDecimal("10000"); // KES 10,000

    @Column(name = "min_guarantor_shares", nullable = false)
    private BigDecimal minGuarantorShares = new BigDecimal("5000"); // KES 5,000 - DEPRECATED: Shares no longer count for eligibility

    @Column(name = "min_guarantor_savings_to_loan_ratio", nullable = false)
    private BigDecimal minGuarantorSavingsToLoanRatio = new BigDecimal("0.50"); // 50% of loan (SAVINGS ONLY - not shares)

    @Column(name = "max_guarantor_outstanding_to_savings_ratio", nullable = false)
    private BigDecimal maxGuarantorOutstandingToSavingsRatio = new BigDecimal("0.50"); // 50% of savings

    @Column(name = "max_guarantor_commitments", nullable = false)
    private Integer maxGuarantorCommitments = 3;

    // Loan rules
    @Column(name = "allow_defaulters", nullable = false)
    private Boolean allowDefaulters = false;

    @Column(name = "allow_exited_members", nullable = false)
    private Boolean allowExitedMembers = false;

    // Global loan term limit (applies across all loan products)
    @Column(name = "max_loan_term_months", nullable = false)
    private Integer maxLoanTermMonths = 72; // 6 years — standard Kenyan SACCO maximum

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getMinMemberSavings() { return minMemberSavings; }
    public void setMinMemberSavings(BigDecimal minMemberSavings) { this.minMemberSavings = minMemberSavings; }

    public BigDecimal getMinMemberShares() { return minMemberShares; }
    public void setMinMemberShares(BigDecimal minMemberShares) { this.minMemberShares = minMemberShares; }

    public BigDecimal getMinSavingsToLoanRatio() { return minSavingsToLoanRatio; }
    public void setMinSavingsToLoanRatio(BigDecimal minSavingsToLoanRatio) { this.minSavingsToLoanRatio = minSavingsToLoanRatio; }

    public BigDecimal getMaxOutstandingToSavingsRatio() { return maxOutstandingToSavingsRatio; }
    public void setMaxOutstandingToSavingsRatio(BigDecimal maxOutstandingToSavingsRatio) { this.maxOutstandingToSavingsRatio = maxOutstandingToSavingsRatio; }

    public Integer getMaxActiveLoans() { return maxActiveLoans; }
    public void setMaxActiveLoans(Integer maxActiveLoans) { this.maxActiveLoans = maxActiveLoans; }

    public BigDecimal getMaxLoanToSavingsMultiplier() { return maxLoanToSavingsMultiplier; }
    public void setMaxLoanToSavingsMultiplier(BigDecimal maxLoanToSavingsMultiplier) { this.maxLoanToSavingsMultiplier = maxLoanToSavingsMultiplier; }

    public BigDecimal getMinGuarantorSavings() { return minGuarantorSavings; }
    public void setMinGuarantorSavings(BigDecimal minGuarantorSavings) { this.minGuarantorSavings = minGuarantorSavings; }

    public BigDecimal getMinGuarantorShares() { return minGuarantorShares; }
    public void setMinGuarantorShares(BigDecimal minGuarantorShares) { this.minGuarantorShares = minGuarantorShares; }

    public BigDecimal getMinGuarantorSavingsToLoanRatio() { return minGuarantorSavingsToLoanRatio; }
    public void setMinGuarantorSavingsToLoanRatio(BigDecimal minGuarantorSavingsToLoanRatio) { this.minGuarantorSavingsToLoanRatio = minGuarantorSavingsToLoanRatio; }

    public BigDecimal getMaxGuarantorOutstandingToSavingsRatio() { return maxGuarantorOutstandingToSavingsRatio; }
    public void setMaxGuarantorOutstandingToSavingsRatio(BigDecimal maxGuarantorOutstandingToSavingsRatio) { this.maxGuarantorOutstandingToSavingsRatio = maxGuarantorOutstandingToSavingsRatio; }

    public Integer getMaxGuarantorCommitments() { return maxGuarantorCommitments; }
    public void setMaxGuarantorCommitments(Integer maxGuarantorCommitments) { this.maxGuarantorCommitments = maxGuarantorCommitments; }

    public Boolean getAllowDefaulters() { return allowDefaulters; }
    public void setAllowDefaulters(Boolean allowDefaulters) { this.allowDefaulters = allowDefaulters; }

    public Boolean getAllowExitedMembers() { return allowExitedMembers; }
    public void setAllowExitedMembers(Boolean allowExitedMembers) { this.allowExitedMembers = allowExitedMembers; }

    public Integer getMaxLoanTermMonths() { return maxLoanTermMonths; }
    public void setMaxLoanTermMonths(Integer maxLoanTermMonths) { this.maxLoanTermMonths = maxLoanTermMonths; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
