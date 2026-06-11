package com.minet.sacco.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request to set interest and approve loan at treasurer stage
 * Treasurer can override interest rate before final approval
 */
public class TreasurerLoanApprovalRequest {
    
    @NotNull(message = "Loan ID is required")
    private Long loanId;
    
    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.00", message = "Interest rate must be >= 0")
    private BigDecimal interestRate;
    
    @NotNull(message = "Action is required")
    private Boolean approved;  // true = approve, false = reject
    
    private String notes;  // Optional rejection notes

    public TreasurerLoanApprovalRequest() {}

    public TreasurerLoanApprovalRequest(Long loanId, BigDecimal interestRate, Boolean approved) {
        this.loanId = loanId;
        this.interestRate = interestRate;
        this.approved = approved;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public Boolean getApproved() {
        return approved;
    }

    public void setApproved(Boolean approved) {
        this.approved = approved;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
