package com.minet.sacco.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_transaction_items")
public class BulkTransactionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    @JsonIgnoreProperties({"items", "memberItems", "loanItems", "disbursementItems", "uploadedBy", "approvedBy"})
    private BulkBatch batch;

    @Column(name = "row_number", nullable = false)
    private Integer rowNumber;

    @Column(name = "member_number", length = 50)
    private String memberNumber;

    @ManyToOne
    @JoinColumn(name = "member_id")
    @JsonIgnoreProperties({"loans", "accounts", "guarantors"})
    private Member member;

    @Column(name = "savings_amount", precision = 15, scale = 2)
    private BigDecimal savingsAmount = BigDecimal.ZERO;

    @Column(name = "shares_amount", precision = 15, scale = 2)
    private BigDecimal sharesAmount = BigDecimal.ZERO;

    @Column(name = "loan_repayment_amount", precision = 15, scale = 2)
    private BigDecimal loanRepaymentAmount = BigDecimal.ZERO;

    @Column(name = "loan_number", length = 50)
    private String loanNumber;

    @ManyToOne
    @JoinColumn(name = "loan_id")
    @JsonIgnoreProperties({"member", "loanProduct", "repayments", "guarantors", "createdBy", "approvedBy", "disbursedBy"})
    private Loan loan;

    @Column(name = "benevolent_fund_amount", precision = 15, scale = 2)
    private BigDecimal benevolentFundAmount = BigDecimal.ZERO;

    @Column(name = "development_fund_amount", precision = 15, scale = 2)
    private BigDecimal developmentFundAmount = BigDecimal.ZERO;

    @Column(name = "school_fees_amount", precision = 15, scale = 2)
    private BigDecimal schoolFeesAmount = BigDecimal.ZERO;

    @Column(name = "holiday_fund_amount", precision = 15, scale = 2)
    private BigDecimal holidayFundAmount = BigDecimal.ZERO;

    @Column(name = "emergency_fund_amount", precision = 15, scale = 2)
    private BigDecimal emergencyFundAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @ManyToOne
    @JoinColumn(name = "savings_transaction_id")
    @JsonIgnoreProperties({"account", "createdBy"})
    private Transaction savingsTransaction;

    @ManyToOne
    @JoinColumn(name = "shares_transaction_id")
    @JsonIgnoreProperties({"account", "createdBy"})
    private Transaction sharesTransaction;

    @ManyToOne
    @JoinColumn(name = "loan_repayment_id")
    @JsonIgnoreProperties({"loan", "createdBy"})
    private LoanRepayment loanRepayment;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BulkBatch getBatch() { return batch; }
    public void setBatch(BulkBatch batch) { this.batch = batch; }

    public Integer getRowNumber() { return rowNumber; }
    public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }

    public String getMemberNumber() { return memberNumber; }
    public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public BigDecimal getSavingsAmount() { return savingsAmount; }
    public void setSavingsAmount(BigDecimal savingsAmount) { this.savingsAmount = savingsAmount; }

    public BigDecimal getSharesAmount() { return sharesAmount; }
    public void setSharesAmount(BigDecimal sharesAmount) { this.sharesAmount = sharesAmount; }

    public BigDecimal getLoanRepaymentAmount() { return loanRepaymentAmount; }
    public void setLoanRepaymentAmount(BigDecimal loanRepaymentAmount) { this.loanRepaymentAmount = loanRepaymentAmount; }

    public String getLoanNumber() { return loanNumber; }
    public void setLoanNumber(String loanNumber) { this.loanNumber = loanNumber; }

    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public Transaction getSavingsTransaction() { return savingsTransaction; }
    public void setSavingsTransaction(Transaction savingsTransaction) { this.savingsTransaction = savingsTransaction; }

    public Transaction getSharesTransaction() { return sharesTransaction; }
    public void setSharesTransaction(Transaction sharesTransaction) { this.sharesTransaction = sharesTransaction; }

    public LoanRepayment getLoanRepayment() { return loanRepayment; }
    public void setLoanRepayment(LoanRepayment loanRepayment) { this.loanRepayment = loanRepayment; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }

    public BigDecimal getBenevolentFundAmount() { return benevolentFundAmount; }
    public void setBenevolentFundAmount(BigDecimal benevolentFundAmount) { this.benevolentFundAmount = benevolentFundAmount; }

    public BigDecimal getDevelopmentFundAmount() { return developmentFundAmount; }
    public void setDevelopmentFundAmount(BigDecimal developmentFundAmount) { this.developmentFundAmount = developmentFundAmount; }


    public BigDecimal getSchoolFeesAmount() { return schoolFeesAmount; }
    public void setSchoolFeesAmount(BigDecimal schoolFeesAmount) { this.schoolFeesAmount = schoolFeesAmount; }

    public BigDecimal getHolidayFundAmount() { return holidayFundAmount; }
    public void setHolidayFundAmount(BigDecimal holidayFundAmount) { this.holidayFundAmount = holidayFundAmount; }

    public BigDecimal getEmergencyFundAmount() { return emergencyFundAmount; }
    public void setEmergencyFundAmount(BigDecimal emergencyFundAmount) { this.emergencyFundAmount = emergencyFundAmount; }
}
