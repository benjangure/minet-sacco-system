package com.minet.sacco.dto;

import java.math.BigDecimal;

public class LoanRepaymentDTO {
    private Long loanId;
    private BigDecimal amount;
    private String paymentMethod; // CASH, MPESA, BANK_TRANSFER
    private String description;

    public LoanRepaymentDTO() {}

    public LoanRepaymentDTO(Long loanId, BigDecimal amount, String paymentMethod, String description) {
        this.loanId = loanId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.description = description;
    }

    // Getters and Setters
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
