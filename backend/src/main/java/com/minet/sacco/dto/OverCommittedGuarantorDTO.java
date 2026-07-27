package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for Over-Committed Guarantor Report
 * Identifies guarantors where frozen pledges exceed available savings
 * This is a risk indicator that the guarantor may not be able to cover their pledged amounts
 */
public class OverCommittedGuarantorDTO {

    private List<OverCommittedGuarantorDetail> overCommittedGuarantors;
    private BigDecimal totalAtRisk;  // Sum of amount over-committed
    private int countOverCommitted; // Number of guarantors over-committed
    private BigDecimal systemRiskExposure; // Total of all over-committed amounts

    public OverCommittedGuarantorDTO() {}

    public OverCommittedGuarantorDTO(List<OverCommittedGuarantorDetail> overCommittedGuarantors) {
        this.overCommittedGuarantors = overCommittedGuarantors;
        this.countOverCommitted = overCommittedGuarantors.size();
        this.totalAtRisk = overCommittedGuarantors.stream()
                .map(OverCommittedGuarantorDetail::getAmountOverCommitted)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.systemRiskExposure = totalAtRisk;
    }

    public static class OverCommittedGuarantorDetail {
        private Long memberId;
        private String memberNumber;
        private String memberName;
        private String memberStatus;
        private BigDecimal totalSavings;           // Total savings balance
        private BigDecimal frozenSelfGuarantee;    // Frozen from own self-guaranteed loans
        private BigDecimal frozenPledges;          // Frozen pledges as guarantor on OTHER loans
        private BigDecimal totalFrozen;            // Total frozen (self + guarantor pledges)
        private BigDecimal availableSavings;       // Savings NOT frozen for self-guarantees
        private BigDecimal amountOverCommitted;    // How much frozen pledges EXCEED available savings
        private int numberOfLoansGuaranteeing;     // Count of active loans they're guaranteeing
        private List<RiskyGuaranteeDetail> riskyGuarantees;  // Breakdown by loan

        // Getters and Setters
        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }

        public String getMemberNumber() { return memberNumber; }
        public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }

        public String getMemberName() { return memberName; }
        public void setMemberName(String memberName) { this.memberName = memberName; }

        public String getMemberStatus() { return memberStatus; }
        public void setMemberStatus(String memberStatus) { this.memberStatus = memberStatus; }

        public BigDecimal getTotalSavings() { return totalSavings; }
        public void setTotalSavings(BigDecimal totalSavings) { this.totalSavings = totalSavings; }

        public BigDecimal getFrozenSelfGuarantee() { return frozenSelfGuarantee; }
        public void setFrozenSelfGuarantee(BigDecimal frozenSelfGuarantee) { this.frozenSelfGuarantee = frozenSelfGuarantee; }

        public BigDecimal getFrozenPledges() { return frozenPledges; }
        public void setFrozenPledges(BigDecimal frozenPledges) { this.frozenPledges = frozenPledges; }

        public BigDecimal getTotalFrozen() { return totalFrozen; }
        public void setTotalFrozen(BigDecimal totalFrozen) { this.totalFrozen = totalFrozen; }

        public BigDecimal getAvailableSavings() { return availableSavings; }
        public void setAvailableSavings(BigDecimal availableSavings) { this.availableSavings = availableSavings; }

        public BigDecimal getAmountOverCommitted() { return amountOverCommitted; }
        public void setAmountOverCommitted(BigDecimal amountOverCommitted) { this.amountOverCommitted = amountOverCommitted; }

        public int getNumberOfLoansGuaranteeing() { return numberOfLoansGuaranteeing; }
        public void setNumberOfLoansGuaranteeing(int numberOfLoansGuaranteeing) { this.numberOfLoansGuaranteeing = numberOfLoansGuaranteeing; }

        public List<RiskyGuaranteeDetail> getRiskyGuarantees() { return riskyGuarantees; }
        public void setRiskyGuarantees(List<RiskyGuaranteeDetail> riskyGuarantees) { this.riskyGuarantees = riskyGuarantees; }
    }

    public static class RiskyGuaranteeDetail {
        private Long loanId;
        private String loanNumber;
        private String borrowerName;
        private BigDecimal loanAmount;
        private BigDecimal outstandingBalance;
        private BigDecimal guarantorPledgeAmount;
        private BigDecimal currentFrozenPledge;
        private String guarantorStatus;

        // Getters and Setters
        public Long getLoanId() { return loanId; }
        public void setLoanId(Long loanId) { this.loanId = loanId; }

        public String getLoanNumber() { return loanNumber; }
        public void setLoanNumber(String loanNumber) { this.loanNumber = loanNumber; }

        public String getBorrowerName() { return borrowerName; }
        public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }

        public BigDecimal getLoanAmount() { return loanAmount; }
        public void setLoanAmount(BigDecimal loanAmount) { this.loanAmount = loanAmount; }

        public BigDecimal getOutstandingBalance() { return outstandingBalance; }
        public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }

        public BigDecimal getGuarantorPledgeAmount() { return guarantorPledgeAmount; }
        public void setGuarantorPledgeAmount(BigDecimal guarantorPledgeAmount) { this.guarantorPledgeAmount = guarantorPledgeAmount; }

        public BigDecimal getCurrentFrozenPledge() { return currentFrozenPledge; }
        public void setCurrentFrozenPledge(BigDecimal currentFrozenPledge) { this.currentFrozenPledge = currentFrozenPledge; }

        public String getGuarantorStatus() { return guarantorStatus; }
        public void setGuarantorStatus(String guarantorStatus) { this.guarantorStatus = guarantorStatus; }
    }

    // Getters and Setters for OverCommittedGuarantorDTO
    public List<OverCommittedGuarantorDetail> getOverCommittedGuarantors() { return overCommittedGuarantors; }
    public void setOverCommittedGuarantors(List<OverCommittedGuarantorDetail> overCommittedGuarantors) { this.overCommittedGuarantors = overCommittedGuarantors; }

    public BigDecimal getTotalAtRisk() { return totalAtRisk; }
    public void setTotalAtRisk(BigDecimal totalAtRisk) { this.totalAtRisk = totalAtRisk; }

    public int getCountOverCommitted() { return countOverCommitted; }
    public void setCountOverCommitted(int countOverCommitted) { this.countOverCommitted = countOverCommitted; }

    public BigDecimal getSystemRiskExposure() { return systemRiskExposure; }
    public void setSystemRiskExposure(BigDecimal systemRiskExposure) { this.systemRiskExposure = systemRiskExposure; }
}
