package com.minet.sacco.dto;

import java.math.BigDecimal;

public class ExpenseDTO {
    private OperatingExpensesDTO operatingExpenses;
    private LoanLossProvisionsDTO loanLossProvisions;
    private BigDecimal otherExpenses;
    private BigDecimal totalExpenses;

    // Constructors
    public ExpenseDTO() {
    }

    public ExpenseDTO(OperatingExpensesDTO operatingExpenses, LoanLossProvisionsDTO loanLossProvisions,
                      BigDecimal otherExpenses, BigDecimal totalExpenses) {
        this.operatingExpenses = operatingExpenses;
        this.loanLossProvisions = loanLossProvisions;
        this.otherExpenses = otherExpenses;
        this.totalExpenses = totalExpenses;
    }

    // Getters and Setters
    public OperatingExpensesDTO getOperatingExpenses() {
        return operatingExpenses;
    }

    public void setOperatingExpenses(OperatingExpensesDTO operatingExpenses) {
        this.operatingExpenses = operatingExpenses;
    }

    public LoanLossProvisionsDTO getLoanLossProvisions() {
        return loanLossProvisions;
    }

    public void setLoanLossProvisions(LoanLossProvisionsDTO loanLossProvisions) {
        this.loanLossProvisions = loanLossProvisions;
    }

    public BigDecimal getOtherExpenses() {
        return otherExpenses;
    }

    public void setOtherExpenses(BigDecimal otherExpenses) {
        this.otherExpenses = otherExpenses;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    // Inner class for Operating Expenses
    public static class OperatingExpensesDTO {
        private BigDecimal salaries;
        private BigDecimal rent;
        private BigDecimal utilities;
        private BigDecimal other;
        private BigDecimal total;

        public OperatingExpensesDTO() {
        }

        public OperatingExpensesDTO(BigDecimal salaries, BigDecimal rent, BigDecimal utilities,
                                    BigDecimal other, BigDecimal total) {
            this.salaries = salaries;
            this.rent = rent;
            this.utilities = utilities;
            this.other = other;
            this.total = total;
        }

        public BigDecimal getSalaries() {
            return salaries;
        }

        public void setSalaries(BigDecimal salaries) {
            this.salaries = salaries;
        }

        public BigDecimal getRent() {
            return rent;
        }

        public void setRent(BigDecimal rent) {
            this.rent = rent;
        }

        public BigDecimal getUtilities() {
            return utilities;
        }

        public void setUtilities(BigDecimal utilities) {
            this.utilities = utilities;
        }

        public BigDecimal getOther() {
            return other;
        }

        public void setOther(BigDecimal other) {
            this.other = other;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public void setTotal(BigDecimal total) {
            this.total = total;
        }
    }

    // Inner class for Loan Loss Provisions
    public static class LoanLossProvisionsDTO {
        private BigDecimal doubtfulDebts;
        private BigDecimal writeOffs;
        private BigDecimal total;

        public LoanLossProvisionsDTO() {
        }

        public LoanLossProvisionsDTO(BigDecimal doubtfulDebts, BigDecimal writeOffs, BigDecimal total) {
            this.doubtfulDebts = doubtfulDebts;
            this.writeOffs = writeOffs;
            this.total = total;
        }

        public BigDecimal getDoubtfulDebts() {
            return doubtfulDebts;
        }

        public void setDoubtfulDebts(BigDecimal doubtfulDebts) {
            this.doubtfulDebts = doubtfulDebts;
        }

        public BigDecimal getWriteOffs() {
            return writeOffs;
        }

        public void setWriteOffs(BigDecimal writeOffs) {
            this.writeOffs = writeOffs;
        }

        public BigDecimal getTotal() {
            return total;
        }

        public void setTotal(BigDecimal total) {
            this.total = total;
        }
    }
}
