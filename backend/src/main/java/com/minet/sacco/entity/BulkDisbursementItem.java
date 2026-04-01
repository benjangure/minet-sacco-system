package com.minet.sacco.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bulk_disbursement_items")
public class BulkDisbursementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    @JsonIgnoreProperties({"items", "memberItems", "loanItems", "disbursementItems", "uploadedBy", "approvedBy"})
    private BulkBatch batch;

    private Integer rowNumber;

    private String loanNumber;

    @ManyToOne
    @JoinColumn(name = "loan_id")
    @JsonIgnoreProperties({"member", "loanProduct", "repayments", "guarantors", "createdBy", "approvedBy", "disbursedBy"})
    private Loan loan;

    private BigDecimal disbursementAmount;

    private String disbursementAccount; // SAVINGS, SHARES, etc.

    private String status; // PENDING, SUCCESS, FAILED

    private String errorMessage;

    private LocalDateTime processedAt;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BulkBatch getBatch() { return batch; }
    public void setBatch(BulkBatch batch) { this.batch = batch; }

    public Integer getRowNumber() { return rowNumber; }
    public void setRowNumber(Integer rowNumber) { this.rowNumber = rowNumber; }

    public String getLoanNumber() { return loanNumber; }
    public void setLoanNumber(String loanNumber) { this.loanNumber = loanNumber; }

    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }

    public BigDecimal getDisbursementAmount() { return disbursementAmount; }
    public void setDisbursementAmount(BigDecimal disbursementAmount) { this.disbursementAmount = disbursementAmount; }

    public String getDisbursementAccount() { return disbursementAccount; }
    public void setDisbursementAccount(String disbursementAccount) { this.disbursementAccount = disbursementAccount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
