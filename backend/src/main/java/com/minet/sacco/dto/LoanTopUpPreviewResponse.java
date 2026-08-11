package com.minet.sacco.dto;

import java.math.BigDecimal;

/**
 * DTO for previewing loan top-up calculations before submission
 */
public class LoanTopUpPreviewResponse {

    private Long loanId;
    private String loanNumber;
    private BigDecimal currentOutstanding;
    private BigDecimal principalAlreadyPaid;
    private BigDecimal topupAmount;
    private BigDecimal newOutstanding;
    private BigDecimal currentInterest;
    private BigDecimal newInterest;
    private BigDecimal currentMonthlyPayment;
    private BigDecimal newMonthlyPayment;
    private BigDecimal currentTotalRepayable;
    private BigDecimal newTotalRepayable;
    private Integer termMonths;
    private BigDecimal interestRate;
    private EligibilityCheck eligibilityCheck;

    // Nested class for eligibility
    public static class EligibilityCheck {
        private boolean eligible;
        private BigDecimal maxTopupAllowed;
        private String message;

        public boolean isEligible() {
            return eligible;
        }

        public void setEligible(boolean eligible) {
            this.eligible = eligible;
        }

        public BigDecimal getMaxTopupAllowed() {
            return maxTopupAllowed;
        }

        public void setMaxTopupAllowed(BigDecimal maxTopupAllowed) {
            this.maxTopupAllowed = maxTopupAllowed;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

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

    public BigDecimal getCurrentOutstanding() {
        return currentOutstanding;
    }

    public void setCurrentOutstanding(BigDecimal currentOutstanding) {
        this.currentOutstanding = currentOutstanding;
    }

    public BigDecimal getPrincipalAlreadyPaid() {
        return principalAlreadyPaid;
    }

    public void setPrincipalAlreadyPaid(BigDecimal principalAlreadyPaid) {
        this.principalAlreadyPaid = principalAlreadyPaid;
    }

    public BigDecimal getTopupAmount() {
        return topupAmount;
    }

    public void setTopupAmount(BigDecimal topupAmount) {
        this.topupAmount = topupAmount;
    }

    public BigDecimal getNewOutstanding() {
        return newOutstanding;
    }

    public void setNewOutstanding(BigDecimal newOutstanding) {
        this.newOutstanding = newOutstanding;
    }

    public BigDecimal getCurrentInterest() {
        return currentInterest;
    }

    public void setCurrentInterest(BigDecimal currentInterest) {
        this.currentInterest = currentInterest;
    }

    public BigDecimal getNewInterest() {
        return newInterest;
    }

    public void setNewInterest(BigDecimal newInterest) {
        this.newInterest = newInterest;
    }

    public BigDecimal getCurrentMonthlyPayment() {
        return currentMonthlyPayment;
    }

    public void setCurrentMonthlyPayment(BigDecimal currentMonthlyPayment) {
        this.currentMonthlyPayment = currentMonthlyPayment;
    }

    public BigDecimal getNewMonthlyPayment() {
        return newMonthlyPayment;
    }

    public void setNewMonthlyPayment(BigDecimal newMonthlyPayment) {
        this.newMonthlyPayment = newMonthlyPayment;
    }

    public BigDecimal getCurrentTotalRepayable() {
        return currentTotalRepayable;
    }

    public void setCurrentTotalRepayable(BigDecimal currentTotalRepayable) {
        this.currentTotalRepayable = currentTotalRepayable;
    }

    public BigDecimal getNewTotalRepayable() {
        return newTotalRepayable;
    }

    public void setNewTotalRepayable(BigDecimal newTotalRepayable) {
        this.newTotalRepayable = newTotalRepayable;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(Integer termMonths) {
        this.termMonths = termMonths;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public EligibilityCheck getEligibilityCheck() {
        return eligibilityCheck;
    }

    public void setEligibilityCheck(EligibilityCheck eligibilityCheck) {
        this.eligibilityCheck = eligibilityCheck;
    }
}
