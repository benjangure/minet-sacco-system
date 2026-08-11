package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO showing the impact analysis of a member exit
 */
public class MemberExitImpactResponse {

    private Long memberId;
    private String memberName;
    private String employeeId;

    private int totalLoansAsGuarantor;
    private BigDecimal totalGuaranteeAmount;

    private List<LoanGuaranteeInfo> loansAsGuarantor = new ArrayList<>();
    private boolean allLoansHaveNok;
    private int loansWithoutNok;

    public static class LoanGuaranteeInfo {
        private Long loanId;
        private String loanNumber;
        private String borrowerName;
        private BigDecimal guaranteeAmount;
        private boolean hasNok;
        private String nokName;
        private Long nokMemberId;

        // Constructors
        public LoanGuaranteeInfo() {}

        public LoanGuaranteeInfo(Long loanId, String loanNumber, String borrowerName, 
                                BigDecimal guaranteeAmount, boolean hasNok, String nokName, Long nokMemberId) {
            this.loanId = loanId;
            this.loanNumber = loanNumber;
            this.borrowerName = borrowerName;
            this.guaranteeAmount = guaranteeAmount;
            this.hasNok = hasNok;
            this.nokName = nokName;
            this.nokMemberId = nokMemberId;
        }

        // Getters and Setters
        public Long getLoanId() { return loanId; }
        public void setLoanId(Long loanId) { this.loanId = loanId; }

        public String getLoanNumber() { return loanNumber; }
        public void setLoanNumber(String loanNumber) { this.loanNumber = loanNumber; }

        public String getBorrowerName() { return borrowerName; }
        public void setBorrowerName(String borrowerName) { this.borrowerName = borrowerName; }

        public BigDecimal getGuaranteeAmount() { return guaranteeAmount; }
        public void setGuaranteeAmount(BigDecimal guaranteeAmount) { this.guaranteeAmount = guaranteeAmount; }

        public boolean isHasNok() { return hasNok; }
        public void setHasNok(boolean hasNok) { this.hasNok = hasNok; }

        public String getNokName() { return nokName; }
        public void setNokName(String nokName) { this.nokName = nokName; }

        public Long getNokMemberId() { return nokMemberId; }
        public void setNokMemberId(Long nokMemberId) { this.nokMemberId = nokMemberId; }
    }

    // Constructors
    public MemberExitImpactResponse() {}

    // Getters and Setters
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public int getTotalLoansAsGuarantor() { return totalLoansAsGuarantor; }
    public void setTotalLoansAsGuarantor(int totalLoansAsGuarantor) { this.totalLoansAsGuarantor = totalLoansAsGuarantor; }

    public BigDecimal getTotalGuaranteeAmount() { return totalGuaranteeAmount; }
    public void setTotalGuaranteeAmount(BigDecimal totalGuaranteeAmount) { this.totalGuaranteeAmount = totalGuaranteeAmount; }

    public List<LoanGuaranteeInfo> getLoansAsGuarantor() { return loansAsGuarantor; }
    public void setLoansAsGuarantor(List<LoanGuaranteeInfo> loansAsGuarantor) { this.loansAsGuarantor = loansAsGuarantor; }

    public boolean isAllLoansHaveNok() { return allLoansHaveNok; }
    public void setAllLoansHaveNok(boolean allLoansHaveNok) { this.allLoansHaveNok = allLoansHaveNok; }

    public int getLoansWithoutNok() { return loansWithoutNok; }
    public void setLoansWithoutNok(int loansWithoutNok) { this.loansWithoutNok = loansWithoutNok; }
}
