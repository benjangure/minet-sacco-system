package com.minet.sacco.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class LoanApprovalRequest {

    @NotNull
    private Long loanId;

    @NotNull
    private Boolean approved;

    @NotBlank
    private String comments;

    private BigDecimal interestRate; // Optional: Treasurer sets this during final approval

    // Getters and Setters
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
}
