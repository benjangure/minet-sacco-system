package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LoanEligibilityReportDTO {

    private Long memberId;
    private String memberNumber;
    private String memberName;
    private String memberStatus;
    private LocalDate dateJoined;
    private Integer monthsAsMember;

    // Savings Status
    private BigDecimal savingsBalance;
    private BigDecimal frozenAmount;
    private BigDecimal availableSavings;

    // Eligibility Calculation
    private BigDecimal grossEligibility;
    private BigDecimal outstandingLoanBalance;
    private BigDecimal remainingEligibility;
    private Integer monthsContributed;

    // Eligibility Status
    private String eligibilityStatus;  // ELIGIBLE or NOT_ELIGIBLE
    private String eligibilityReason;  // Reason if not eligible

    public LoanEligibilityReportDTO() {}

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getMemberNumber() { return memberNumber; }
    public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getMemberStatus() { return memberStatus; }
    public void setMemberStatus(String memberStatus) { this.memberStatus = memberStatus; }

    public LocalDate getDateJoined() { return dateJoined; }
    public void setDateJoined(LocalDate dateJoined) { this.dateJoined = dateJoined; }

    public Integer getMonthsAsMember() { return monthsAsMember; }
    public void setMonthsAsMember(Integer monthsAsMember) { this.monthsAsMember = monthsAsMember; }

    public BigDecimal getSavingsBalance() { return savingsBalance; }
    public void setSavingsBalance(BigDecimal savingsBalance) { this.savingsBalance = savingsBalance; }

    public BigDecimal getFrozenAmount() { return frozenAmount; }
    public void setFrozenAmount(BigDecimal frozenAmount) { this.frozenAmount = frozenAmount; }

    public BigDecimal getAvailableSavings() { return availableSavings; }
    public void setAvailableSavings(BigDecimal availableSavings) { this.availableSavings = availableSavings; }

    public BigDecimal getGrossEligibility() { return grossEligibility; }
    public void setGrossEligibility(BigDecimal grossEligibility) { this.grossEligibility = grossEligibility; }

    public BigDecimal getOutstandingLoanBalance() { return outstandingLoanBalance; }
    public void setOutstandingLoanBalance(BigDecimal outstandingLoanBalance) { this.outstandingLoanBalance = outstandingLoanBalance; }

    public BigDecimal getRemainingEligibility() { return remainingEligibility; }
    public void setRemainingEligibility(BigDecimal remainingEligibility) { this.remainingEligibility = remainingEligibility; }

    public Integer getMonthsContributed() { return monthsContributed; }
    public void setMonthsContributed(Integer monthsContributed) { this.monthsContributed = monthsContributed; }

    public String getEligibilityStatus() { return eligibilityStatus; }
    public void setEligibilityStatus(String eligibilityStatus) { this.eligibilityStatus = eligibilityStatus; }

    public String getEligibilityReason() { return eligibilityReason; }
    public void setEligibilityReason(String eligibilityReason) { this.eligibilityReason = eligibilityReason; }
}
