package com.minet.sacco.dto;

import java.math.BigDecimal;

public class RevenueDTO {
    private InterestIncomeDTO interestIncome;
    private FeesAndChargesDTO feesAndCharges;
    private BigDecimal otherIncome;
    private BigDecimal totalRevenue;

    // Constructors
    public RevenueDTO() {
    }

    public RevenueDTO(InterestIncomeDTO interestIncome, FeesAndChargesDTO feesAndCharges,
                      BigDecimal otherIncome, BigDecimal totalRevenue) {
        this.interestIncome = interestIncome;
        this.feesAndCharges = feesAndCharges;
        this.otherIncome = otherIncome;
        this.totalRevenue = totalRevenue;
    }

    // Getters and Setters
    public InterestIncomeDTO getInterestIncome() {
        return interestIncome;
    }

    public void setInterestIncome(InterestIncomeDTO interestIncome) {
        this.interestIncome = interestIncome;
    }

    public FeesAndChargesDTO getFeesAndCharges() {
        return feesAndCharges;
    }

    public void setFeesAndCharges(FeesAndChargesDTO feesAndCharges) {
        this.feesAndCharges = feesAndCharges;
    }

    public BigDecimal getOtherIncome() {
        return otherIncome;
    }

    public void setOtherIncome(BigDecimal otherIncome) {
        this.otherIncome = otherIncome;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    // Inner class for Interest Income
    public static class InterestIncomeDTO {
        private BigDecimal fromLoans;
        private BigDecimal fromSavings;
        private BigDecimal total;

        public InterestIncomeDTO() {
        }

        public InterestIncomeDTO(BigDecimal fromLoans, BigDecimal fromSavings, BigDecimal total) {
            this.fromLoans = fromLoans;
            this.fromSavings = fromSavings;
            this.total = total;
        }

        public BigDecimal getFromLoans() {
            return fromLoans;
        }

        public void setFromLoans(BigDecimal fromLoans) {
            this.fromLoans = fromLoans;
        }

        public BigDecimal getFromSavings() {
            return fromSavings;
        }

        public void setFromSavings(BigDecimal fromSavings) {
            this.fromSavings = fromSavings;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public void setTotal(BigDecimal total) {
            this.total = total;
        }
    }

    // Inner class for Fees and Charges
    public static class FeesAndChargesDTO {
        private BigDecimal loanProcessingFees;
        private BigDecimal accountMaintenanceFees;
        private BigDecimal otherFees;
        private BigDecimal total;

        public FeesAndChargesDTO() {
        }

        public FeesAndChargesDTO(BigDecimal loanProcessingFees, BigDecimal accountMaintenanceFees,
                                 BigDecimal otherFees, BigDecimal total) {
            this.loanProcessingFees = loanProcessingFees;
            this.accountMaintenanceFees = accountMaintenanceFees;
            this.otherFees = otherFees;
            this.total = total;
        }

        public BigDecimal getLoanProcessingFees() {
            return loanProcessingFees;
        }

        public void setLoanProcessingFees(BigDecimal loanProcessingFees) {
            this.loanProcessingFees = loanProcessingFees;
        }

        public BigDecimal getAccountMaintenanceFees() {
            return accountMaintenanceFees;
        }

        public void setAccountMaintenanceFees(BigDecimal accountMaintenanceFees) {
            this.accountMaintenanceFees = accountMaintenanceFees;
        }

        public BigDecimal getOtherFees() {
            return otherFees;
        }

        public void setOtherFees(BigDecimal otherFees) {
            this.otherFees = otherFees;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public void setTotal(BigDecimal total) {
            this.total = total;
        }
    }
}
