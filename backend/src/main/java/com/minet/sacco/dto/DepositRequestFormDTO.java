package com.minet.sacco.dto;

public class DepositRequestFormDTO {
    private String accountId;
    private String claimedAmount;
    private String description;

    public DepositRequestFormDTO() {}

    public DepositRequestFormDTO(String accountId, String claimedAmount, String description) {
        this.accountId = accountId;
        this.claimedAmount = claimedAmount;
        this.description = description;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getClaimedAmount() {
        return claimedAmount;
    }

    public void setClaimedAmount(String claimedAmount) {
        this.claimedAmount = claimedAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
