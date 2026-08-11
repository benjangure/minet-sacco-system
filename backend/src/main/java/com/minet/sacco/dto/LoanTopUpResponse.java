package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO for loan top-up response
 */
public class LoanTopUpResponse {

    private Long loanId;
    private String loanNumber;
    private BigDecimal topupAmount;
    private BigDecimal outstandingBefore;
    private BigDecimal outstandingAfter;
    private BigDecimal principalAlreadyPaid;
    private BigDecimal totalTopupAmount;
    private Integer topupCount;
    private BigDecimal newMonthlyPayment;
    private BigDecimal newTotalRepayable;
    private BigDecimal newInterest;
    private LocalDateTime topupDate;
    private String status;

    // Constructors
    public LoanTopUpResponse() {}

    // Getters and Setters
    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public String getLoanNumber() {
        return loanNumber;
    }

    public void setLoanNumber(String loanNumber) {
        this.loanNumber = loanNumber;
    }

    public BigDecimal getTopupAmount() {
        return topupAmount;
    }

    public void setTopupAmount(BigDecimal topupAmount) {
        this.topupAmount = topupAmount;
    }

    public BigDecimal getOutstandingBefore() {
        return outstandingBefore;
    }

    public void setOutstandingBefore(BigDecimal outstandingBefore) {
        this.outstandingBefore = outstandingBefore;
    }

    public BigDecimal getOutstandingAfter() {
        return outstandingAfter;
    }

    public void setOutstandingAfter(BigDecimal outstandingAfter) {
        this.outstandingAfter = outstandingAfter;
    }

    public BigDecimal getPrincipalAlreadyPaid() {
        return principalAlreadyPaid;
    }

    public void setPrincipalAlreadyPaid(BigDecimal principalAlreadyPaid) {
        this.principalAlreadyPaid = principalAlreadyPaid;
    }

    public BigDecimal getTotalTopupAmount() {
        return totalTopupAmount;
    }

    public void setTotalTopupAmount(BigDecimal totalTopupAmount) {
        this.totalTopupAmount = totalTopupAmount;
    }

    public Integer getTopupCount() {
        return topupCount;
    }

    public void setTopupCount(Integer topupCount) {
        this.topupCount = topupCount;
    }

    public BigDecimal getNewMonthlyPayment() {
        return newMonthlyPayment;
    }

    public void setNewMonthlyPayment(BigDecimal newMonthlyPayment) {
        this.newMonthlyPayment = newMonthlyPayment;
    }

    public BigDecimal getNewTotalRepayable() {
        return newTotalRepayable;
    }

    public void setNewTotalRepayable(BigDecimal newTotalRepayable) {
        this.newTotalRepayable = newTotalRepayable;
    }

    public BigDecimal getNewInterest() {
        return newInterest;
    }

    public void setNewInterest(BigDecimal newInterest) {
        this.newInterest = newInterest;
    }

    public LocalDateTime getTopupDate() {
        return topupDate;
    }

    public void setTopupDate(LocalDateTime topupDate) {
        this.topupDate = topupDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
