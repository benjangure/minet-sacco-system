package com.minet.sacco.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_loan_items")
public class BulkLoanItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    @JsonIgnoreProperties({"items", "memberItems", "loanItems", "disbursementItems", "uploadedBy", "approvedBy"})
    private BulkBatch batch;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    private String memberNumber;
    private String loanProductName;
    private BigDecimal amount;
    private Integer termMonths;
    private String purpose;
    private String guarantor1;
    private String guarantor2;
    private String guarantor3;

    @ManyToOne
    @JoinColumn(name = "member_id")
    @JsonIgnoreProperties({"loans", "accounts", "guarantors"})
    private Member member;

    @ManyToOne
    @JoinColumn(name = "loan_id")
    @JsonIgnoreProperties({"member", "loanProduct", "repayments", "guarantors", "createdBy", "approvedBy", "disbursedBy"})
    private Loan loan;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // NEW: Calculation fields
    @Column(name = "total_interest")
    private BigDecimal totalInterest;

    @Column(name = "total_repayable")
    private BigDecimal totalRepayable;

    @Column(name = "monthly_repayment")
    private BigDecimal monthlyRepayment;

    // NEW: Eligibility status fields
    @Column(name = "guarantor1_eligibility_status", length = 20)
    private String guarantor1EligibilityStatus;

    @Column(name = "guarantor2_eligibility_status", length = 20)
    private String guarantor2EligibilityStatus;

    @Column(name = "guarantor3_eligibility_status", length = 20)
    private String guarantor3EligibilityStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BulkBatch getBatch() { return batch; }
    public void setBatch(BulkBatch batch) { this.batch = batch; }
    public Integer getRowNumber() { return rowNumber; }
    public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }
    public String getMemberNumber() { return memberNumber; }
    public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }
    public String getLoanProductName() { return loanProductName; }
    public void setLoanProductName(String loanProductName) { this.loanProductName = loanProductName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getTermMonths() { return termMonths; }
    public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getGuarantor1() { return guarantor1; }
    public void setGuarantor1(String guarantor1) { this.guarantor1 = guarantor1; }
    public String getGuarantor2() { return guarantor2; }
    public void setGuarantor2(String guarantor2) { this.guarantor2 = guarantor2; }
    public String getGuarantor3() { return guarantor3; }
    public void setGuarantor3(String guarantor3) { this.guarantor3 = guarantor3; }
    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }
    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    // NEW: Getters and setters for calculation fields
    public BigDecimal getTotalInterest() { return totalInterest; }
    public void setTotalInterest(BigDecimal totalInterest) { this.totalInterest = totalInterest; }
    public BigDecimal getTotalRepayable() { return totalRepayable; }
    public void setTotalRepayable(BigDecimal totalRepayable) { this.totalRepayable = totalRepayable; }
    public BigDecimal getMonthlyRepayment() { return monthlyRepayment; }
    public void setMonthlyRepayment(BigDecimal monthlyRepayment) { this.monthlyRepayment = monthlyRepayment; }

    // NEW: Getters and setters for eligibility status fields
    public String getGuarantor1EligibilityStatus() { return guarantor1EligibilityStatus; }
    public void setGuarantor1EligibilityStatus(String guarantor1EligibilityStatus) { this.guarantor1EligibilityStatus = guarantor1EligibilityStatus; }
    public String getGuarantor2EligibilityStatus() { return guarantor2EligibilityStatus; }
    public void setGuarantor2EligibilityStatus(String guarantor2EligibilityStatus) { this.guarantor2EligibilityStatus = guarantor2EligibilityStatus; }
    public String getGuarantor3EligibilityStatus() { return guarantor3EligibilityStatus; }
    public void setGuarantor3EligibilityStatus(String guarantor3EligibilityStatus) { this.guarantor3EligibilityStatus = guarantor3EligibilityStatus; }

}
