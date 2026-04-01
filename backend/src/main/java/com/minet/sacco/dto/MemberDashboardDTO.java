package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.util.List;

public class MemberDashboardDTO {
    private String memberNumber;
    private String firstName;
    private String lastName;
    private BigDecimal savingsBalance;
    private BigDecimal sharesBalance;
    private BigDecimal totalBalance;
    private Integer activeLoans;
    private BigDecimal totalOutstanding;
    private Integer pendingApplications;
    private List<RecentTransactionDTO> recentTransactions;

    public MemberDashboardDTO() {}

    public MemberDashboardDTO(String memberNumber, String firstName, String lastName, 
                             BigDecimal savingsBalance, BigDecimal sharesBalance, 
                             BigDecimal totalBalance, Integer activeLoans, 
                             BigDecimal totalOutstanding, Integer pendingApplications) {
        this.memberNumber = memberNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.savingsBalance = savingsBalance;
        this.sharesBalance = sharesBalance;
        this.totalBalance = totalBalance;
        this.activeLoans = activeLoans;
        this.totalOutstanding = totalOutstanding;
        this.pendingApplications = pendingApplications;
    }

    // Getters and Setters
    public String getMemberNumber() { return memberNumber; }
    public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public BigDecimal getSavingsBalance() { return savingsBalance; }
    public void setSavingsBalance(BigDecimal savingsBalance) { this.savingsBalance = savingsBalance; }

    public BigDecimal getSharesBalance() { return sharesBalance; }
    public void setSharesBalance(BigDecimal sharesBalance) { this.sharesBalance = sharesBalance; }

    public BigDecimal getTotalBalance() { return totalBalance; }
    public void setTotalBalance(BigDecimal totalBalance) { this.totalBalance = totalBalance; }

    public Integer getActiveLoans() { return activeLoans; }
    public void setActiveLoans(Integer activeLoans) { this.activeLoans = activeLoans; }

    public BigDecimal getTotalOutstanding() { return totalOutstanding; }
    public void setTotalOutstanding(BigDecimal totalOutstanding) { this.totalOutstanding = totalOutstanding; }

    public Integer getPendingApplications() { return pendingApplications; }
    public void setPendingApplications(Integer pendingApplications) { this.pendingApplications = pendingApplications; }

    public List<RecentTransactionDTO> getRecentTransactions() { return recentTransactions; }
    public void setRecentTransactions(List<RecentTransactionDTO> recentTransactions) { this.recentTransactions = recentTransactions; }
}
