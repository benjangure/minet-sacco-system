package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class WithdrawalMonitoringReportDTO {

    private List<WithdrawalTransaction> withdrawalTransactions;
    private SummaryTotals summaryTotals;

    public WithdrawalMonitoringReportDTO(List<WithdrawalTransaction> withdrawalTransactions, SummaryTotals summaryTotals) {
        this.withdrawalTransactions = withdrawalTransactions;
        this.summaryTotals = summaryTotals;
    }

    public List<WithdrawalTransaction> getWithdrawalTransactions() {
        return withdrawalTransactions;
    }

    public void setWithdrawalTransactions(List<WithdrawalTransaction> withdrawalTransactions) {
        this.withdrawalTransactions = withdrawalTransactions;
    }

    public SummaryTotals getSummaryTotals() {
        return summaryTotals;
    }

    public void setSummaryTotals(SummaryTotals summaryTotals) {
        this.summaryTotals = summaryTotals;
    }

    public static class WithdrawalTransaction {
        private Long transactionId;
        private String memberNumber;
        private String memberName;
        private String accountType;
        private BigDecimal withdrawalAmount;
        private LocalDateTime transactionDate;
        private String withdrawalMethod; // M_PESA, MANUAL_CASH, BANK_TRANSFER
        private String processedBy;
        private String transactionStatus; // COMPLETED, PENDING, FAILED
        private BigDecimal accountBalanceBefore;
        private BigDecimal accountBalanceAfter;
        private BigDecimal remainingBalance;

        public WithdrawalTransaction() {}

        public WithdrawalTransaction(Long transactionId, String memberNumber, String memberName, String accountType,
                                    BigDecimal withdrawalAmount, LocalDateTime transactionDate, String withdrawalMethod,
                                    String processedBy, String transactionStatus, BigDecimal accountBalanceBefore,
                                    BigDecimal accountBalanceAfter, BigDecimal remainingBalance) {
            this.transactionId = transactionId;
            this.memberNumber = memberNumber;
            this.memberName = memberName;
            this.accountType = accountType;
            this.withdrawalAmount = withdrawalAmount;
            this.transactionDate = transactionDate;
            this.withdrawalMethod = withdrawalMethod;
            this.processedBy = processedBy;
            this.transactionStatus = transactionStatus;
            this.accountBalanceBefore = accountBalanceBefore;
            this.accountBalanceAfter = accountBalanceAfter;
            this.remainingBalance = remainingBalance;
        }

        // Getters and Setters
        public Long getTransactionId() { return transactionId; }
        public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }

        public String getMemberNumber() { return memberNumber; }
        public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }

        public String getMemberName() { return memberName; }
        public void setMemberName(String memberName) { this.memberName = memberName; }

        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }

        public BigDecimal getWithdrawalAmount() { return withdrawalAmount; }
        public void setWithdrawalAmount(BigDecimal withdrawalAmount) { this.withdrawalAmount = withdrawalAmount; }

        public LocalDateTime getTransactionDate() { return transactionDate; }
        public void setTransactionDate(LocalDateTime transactionDate) { this.transactionDate = transactionDate; }

        public String getWithdrawalMethod() { return withdrawalMethod; }
        public void setWithdrawalMethod(String withdrawalMethod) { this.withdrawalMethod = withdrawalMethod; }

        public String getProcessedBy() { return processedBy; }
        public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }

        public String getTransactionStatus() { return transactionStatus; }
        public void setTransactionStatus(String transactionStatus) { this.transactionStatus = transactionStatus; }

        public BigDecimal getAccountBalanceBefore() { return accountBalanceBefore; }
        public void setAccountBalanceBefore(BigDecimal accountBalanceBefore) { this.accountBalanceBefore = accountBalanceBefore; }

        public BigDecimal getAccountBalanceAfter() { return accountBalanceAfter; }
        public void setAccountBalanceAfter(BigDecimal accountBalanceAfter) { this.accountBalanceAfter = accountBalanceAfter; }

        public BigDecimal getRemainingBalance() { return remainingBalance; }
        public void setRemainingBalance(BigDecimal remainingBalance) { this.remainingBalance = remainingBalance; }
    }

    public static class SummaryTotals {
        private Long totalWithdrawals;
        private BigDecimal totalAmountWithdrawn;
        private MethodSummary mpesaSummary;
        private MethodSummary manualCashSummary;
        private MethodSummary bankTransferSummary;

        public SummaryTotals() {}

        public SummaryTotals(Long totalWithdrawals, BigDecimal totalAmountWithdrawn, MethodSummary mpesaSummary,
                            MethodSummary manualCashSummary, MethodSummary bankTransferSummary) {
            this.totalWithdrawals = totalWithdrawals;
            this.totalAmountWithdrawn = totalAmountWithdrawn;
            this.mpesaSummary = mpesaSummary;
            this.manualCashSummary = manualCashSummary;
            this.bankTransferSummary = bankTransferSummary;
        }

        // Getters and Setters
        public Long getTotalWithdrawals() { return totalWithdrawals; }
        public void setTotalWithdrawals(Long totalWithdrawals) { this.totalWithdrawals = totalWithdrawals; }

        public BigDecimal getTotalAmountWithdrawn() { return totalAmountWithdrawn; }
        public void setTotalAmountWithdrawn(BigDecimal totalAmountWithdrawn) { this.totalAmountWithdrawn = totalAmountWithdrawn; }

        public MethodSummary getMpesaSummary() { return mpesaSummary; }
        public void setMpesaSummary(MethodSummary mpesaSummary) { this.mpesaSummary = mpesaSummary; }

        public MethodSummary getManualCashSummary() { return manualCashSummary; }
        public void setManualCashSummary(MethodSummary manualCashSummary) { this.manualCashSummary = manualCashSummary; }

        public MethodSummary getBankTransferSummary() { return bankTransferSummary; }
        public void setBankTransferSummary(MethodSummary bankTransferSummary) { this.bankTransferSummary = bankTransferSummary; }
    }

    public static class MethodSummary {
        private Long count;
        private BigDecimal amount;

        public MethodSummary() {}

        public MethodSummary(Long count, BigDecimal amount) {
            this.count = count;
            this.amount = amount;
        }

        public Long getCount() { return count; }
        public void setCount(Long count) { this.count = count; }

        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
    }
}
