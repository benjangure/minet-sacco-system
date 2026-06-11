package com.minet.sacco.dto;

import java.math.BigDecimal;

public class LoanRepaymentDTO {
    private Long loanId;
    private BigDecimal amount;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private String paymentMethod; // CASH, MPESA, BANK_TRANSFER
    private String description;

    public LoanRepaymentDTO() {}

    public LoanRepaymentDTO(Long loanId, BigDecimal amount, BigDecimal principalAmount, BigDecimal interestAmount, String paymentMethod, String description) {
        this.loanId = loanId;
        this.amount = amount;
        this.principalAmount = principalAmount;
        this.interestAmount = interestAmount;
        this.paymentMethod = paymentMethod;
        this.description = description;
    }

    // Getters and Setters
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }

    public BigDecimal getInterestAmount() { return interestAmount; }
    public void setInterestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
