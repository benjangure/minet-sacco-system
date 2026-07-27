package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class ExitedMemberLoanDTO {
    private List<ExitedMemberLoanDetail> exitedMembersWithLoans;

    public ExitedMemberLoanDTO() {}

    public ExitedMemberLoanDTO(List<ExitedMemberLoanDetail> exitedMembersWithLoans) {
        this.exitedMembersWithLoans = exitedMembersWithLoans;
    }

    public List<ExitedMemberLoanDetail> getExitedMembersWithLoans() {
        return exitedMembersWithLoans;
    }

    public void setExitedMembersWithLoans(List<ExitedMemberLoanDetail> exitedMembersWithLoans) {
        this.exitedMembersWithLoans = exitedMembersWithLoans;
    }

    public static class ExitedMemberLoanDetail {
        private Long memberId;
        private String memberNumber;
        private String memberName;
        private LocalDate exitDate;
        private String exitReason;
        private Long loanId;
        private String loanNumber;
        private BigDecimal outstandingBalance;
        private BigDecimal originalAmount;
        private LocalDate disbursementDate;

        public ExitedMemberLoanDetail() {}

        public ExitedMemberLoanDetail(Long memberId, String memberNumber, String memberName,
                LocalDate exitDate, String exitReason, Long loanId, String loanNumber,
                BigDecimal outstandingBalance, BigDecimal originalAmount, LocalDate disbursementDate) {
            this.memberId = memberId;
            this.memberNumber = memberNumber;
            this.memberName = memberName;
            this.exitDate = exitDate;
            this.exitReason = exitReason;
            this.loanId = loanId;
            this.loanNumber = loanNumber;
            this.outstandingBalance = outstandingBalance;
            this.originalAmount = originalAmount;
            this.disbursementDate = disbursementDate;
        }

        public Long getMemberId() {
            return memberId;
        }

        public void setMemberId(Long memberId) {
            this.memberId = memberId;
        }

        public String getMemberNumber() {
            return memberNumber;
        }

        public void setMemberNumber(String memberNumber) {
            this.memberNumber = memberNumber;
        }

        public String getMemberName() {
            return memberName;
        }

        public void setMemberName(String memberName) {
            this.memberName = memberName;
        }

        public LocalDate getExitDate() {
            return exitDate;
        }

        public void setExitDate(LocalDate exitDate) {
            this.exitDate = exitDate;
        }

        public String getExitReason() {
            return exitReason;
        }

        public void setExitReason(String exitReason) {
            this.exitReason = exitReason;
        }

        public Long getLoanId() {
            return loanId;
        }

        public void setLoanId(Long loanId) {
            this.loanId = loanId;
        }

        public String getLoanNumber() {
            return loanNumber;
        }

        public void setLoanNumber(String loanNumber) {
            this.loanNumber = loanNumber;
        }

        public BigDecimal getOutstandingBalance() {
            return outstandingBalance;
        }

        public void setOutstandingBalance(BigDecimal outstandingBalance) {
            this.outstandingBalance = outstandingBalance;
        }

        public BigDecimal getOriginalAmount() {
            return originalAmount;
        }

        public void setOriginalAmount(BigDecimal originalAmount) {
            this.originalAmount = originalAmount;
        }

        public LocalDate getDisbursementDate() {
            return disbursementDate;
        }

        public void setDisbursementDate(LocalDate disbursementDate) {
            this.disbursementDate = disbursementDate;
        }
    }
}
