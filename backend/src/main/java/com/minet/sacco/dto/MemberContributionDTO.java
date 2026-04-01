package com.minet.sacco.dto;

import java.math.BigDecimal;

public class MemberContributionDTO {
    private Long accountId;
    private BigDecimal amount;
    private String paymentMethod; // CASH, MPESA, BANK_TRANSFER
    private String description;

    public MemberContributionDTO() {}

    public MemberContributionDTO(Long accountId, BigDecimal amount, String paymentMethod, String description) {
        this.accountId = accountId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.description = description;
    }

    // Getters and Setters
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
