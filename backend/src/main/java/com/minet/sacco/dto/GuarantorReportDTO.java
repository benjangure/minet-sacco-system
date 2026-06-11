package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.util.List;

public class GuarantorReportDTO {

    // For Single Member View
    private MemberGuarantorDetail memberDetail;

    // For All Members Summary View
    private List<MemberGuarantorSummary> memberSummaries;

    // Constructor for single member
    public GuarantorReportDTO(MemberGuarantorDetail memberDetail) {
        this.memberDetail = memberDetail;
    }

    // Constructor for all members
    public GuarantorReportDTO(List<MemberGuarantorSummary> memberSummaries) {
        this.memberSummaries = memberSummaries;
    }

    public MemberGuarantorDetail getMemberDetail() {
        return memberDetail;
    }

    public void setMemberDetail(MemberGuarantorDetail memberDetail) {
        this.memberDetail = memberDetail;
    }

    public List<MemberGuarantorSummary> getMemberSummaries() {
        return memberSummaries;
    }

    public void setMemberSummaries(List<MemberGuarantorSummary> memberSummaries) {
        this.memberSummaries = memberSummaries;
    }

    // Single Member Detail View
    public static class MemberGuarantorDetail {
        private Long memberId;
        private String memberNumber;
        private String memberName;
        private String memberStatus;

        // Guarantor Capacity
        private BigDecimal totalSavings;
        private BigDecimal frozenSelfGuaranteeAmount;
        private BigDecimal availableSavings;

        // Active Guarantor Pledges (as guarantor for others)
        private Integer numberOfLoansGuaranteeing;
        private BigDecimal totalPledgeAmount;
        private BigDecimal availableGuarantorshipCapacity;

        // Loans They Are Guaranteeing
        private List<GuarantorLoansDetail> loansGuaranteeing;

        public MemberGuarantorDetail() {}

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

        public BigDecimal getFrozenSelfGuaranteeAmount() { return frozenSelfGuaranteeAmount; }
        public void setFrozenSelfGuaranteeAmount(BigDecimal frozenSelfGuaranteeAmount) { this.frozenSelfGuaranteeAmount = frozenSelfGuaranteeAmount; }

        public BigDecimal getAvailableSavings() { return availableSavings; }
        public void setAvailableSavings(BigDecimal availableSavings) { this.availableSavings = availableSavings; }

        public Integer getNumberOfLoansGuaranteeing() { return numberOfLoansGuaranteeing; }
        public void setNumberOfLoansGuaranteeing(Integer numberOfLoansGuaranteeing) { this.numberOfLoansGuaranteeing = numberOfLoansGuaranteeing; }

        public BigDecimal getTotalPledgeAmount() { return totalPledgeAmount; }
        public void setTotalPledgeAmount(BigDecimal totalPledgeAmount) { this.totalPledgeAmount = totalPledgeAmount; }

        public BigDecimal getAvailableGuarantorshipCapacity() { return availableGuarantorshipCapacity; }
        public void setAvailableGuarantorshipCapacity(BigDecimal availableGuarantorshipCapacity) { this.availableGuarantorshipCapacity = availableGuarantorshipCapacity; }

        public List<GuarantorLoansDetail> getLoansGuaranteeing() { return loansGuaranteeing; }
        public void setLoansGuaranteeing(List<GuarantorLoansDetail> loansGuaranteeing) { this.loansGuaranteeing = loansGuaranteeing; }
    }

    // Details of loans the member is guaranteeing
    public static class GuarantorLoansDetail {
        private Long loanId;
        private String loanNumber;
        private String borrowerName;
        private BigDecimal loanAmount;
        private BigDecimal outstandingBalance;
        private BigDecimal repaymentProgress;  // Percentage
        private BigDecimal guarantorPledgeAmount;
        private String status;  // ACTIVE, RELEASED, DEFAULTED

        public GuarantorLoansDetail() {}

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

        public BigDecimal getRepaymentProgress() { return repaymentProgress; }
        public void setRepaymentProgress(BigDecimal repaymentProgress) { this.repaymentProgress = repaymentProgress; }

        public BigDecimal getGuarantorPledgeAmount() { return guarantorPledgeAmount; }
        public void setGuarantorPledgeAmount(BigDecimal guarantorPledgeAmount) { this.guarantorPledgeAmount = guarantorPledgeAmount; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    // All Members Summary View (for Treasurer's quick reference)
    public static class MemberGuarantorSummary {
        private Long memberId;
        private String memberNumber;
        private String memberName;
        private String memberStatus;
        private BigDecimal availableSavings;
        private BigDecimal availableGuarantorshipCapacity;
        private Integer numberOfLoansGuaranteeing;

        public MemberGuarantorSummary() {}

        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }

        public String getMemberNumber() { return memberNumber; }
        public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }

        public String getMemberName() { return memberName; }
        public void setMemberName(String memberName) { this.memberName = memberName; }

        public String getMemberStatus() { return memberStatus; }
        public void setMemberStatus(String memberStatus) { this.memberStatus = memberStatus; }

        public BigDecimal getAvailableSavings() { return availableSavings; }
        public void setAvailableSavings(BigDecimal availableSavings) { this.availableSavings = availableSavings; }

        public BigDecimal getAvailableGuarantorshipCapacity() { return availableGuarantorshipCapacity; }
        public void setAvailableGuarantorshipCapacity(BigDecimal availableGuarantorshipCapacity) { this.availableGuarantorshipCapacity = availableGuarantorshipCapacity; }

        public Integer getNumberOfLoansGuaranteeing() { return numberOfLoansGuaranteeing; }
        public void setNumberOfLoansGuaranteeing(Integer numberOfLoansGuaranteeing) { this.numberOfLoansGuaranteeing = numberOfLoansGuaranteeing; }
    }
}
