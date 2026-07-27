package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class WithdrawalMonitoringDetailDTO {

    private List<WithdrawalTransaction> transactions;
    private WithdrawalSummary summary;

    public WithdrawalMonitoringDetailDTO() {}

    public WithdrawalMonitoringDetailDTO(List<WithdrawalTransaction> transactions, WithdrawalSummary summary) {
        this.transactions = transactions;
        this.summary = summary;
    }

    public List<WithdrawalTransaction> getTransactions() { return transactions; }
    public void setTransactions(List<WithdrawalTransaction> transactions) { this.transactions = transactions; }

    public WithdrawalSummary getSummary() { return summary; }
    public void setSummary(WithdrawalSummary summary) { this.summary = summary; }

    // Withdrawal Transaction Detail
    public static class WithdrawalTransaction {
        private Long transactionId;
        private String memberNumber;
        private String memberName;
        private String accountType;
        private BigDecimal withdrawalAmount;
        private LocalDateTime transactionDate;
        private String withdrawalMethod;  // M_PESA, MANUAL_CASH, BANK_TRANSFER
        private String processedBy;
        private String transactionStatus;  // COMPLETED, PENDING, FAILED
        private BigDecimal accountBalanceBefore;
        private BigDecimal accountBalanceAfter;
        private BigDecimal remainingBalance;

        public WithdrawalTransaction() {}

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

    // Withdrawal Summary
    public static class WithdrawalSummary {
        private Integer totalWithdrawals;
        private BigDecimal totalAmountWithdrawn;
        private Integer mPesaWithdrawals;
        private BigDecimal mPesaAmount;
        private Integer manualCashWithdrawals;
        private BigDecimal manualCashAmount;
        private Integer bankTransferWithdrawals;
        private BigDecimal bankTransferAmount;

        public WithdrawalSummary() {}

        public Integer getTotalWithdrawals() { return totalWithdrawals; }
        public void setTotalWithdrawals(Integer totalWithdrawals) { this.totalWithdrawals = totalWithdrawals; }

        public BigDecimal getTotalAmountWithdrawn() { return totalAmountWithdrawn; }
        public void setTotalAmountWithdrawn(BigDecimal totalAmountWithdrawn) { this.totalAmountWithdrawn = totalAmountWithdrawn; }

        public Integer getmPesaWithdrawals() { return mPesaWithdrawals; }
        public void setmPesaWithdrawals(Integer mPesaWithdrawals) { this.mPesaWithdrawals = mPesaWithdrawals; }

        public BigDecimal getmPesaAmount() { return mPesaAmount; }
        public void setmPesaAmount(BigDecimal mPesaAmount) { this.mPesaAmount = mPesaAmount; }

        public Integer getManualCashWithdrawals() { return manualCashWithdrawals; }
        public void setManualCashWithdrawals(Integer manualCashWithdrawals) { this.manualCashWithdrawals = manualCashWithdrawals; }

        public BigDecimal getManualCashAmount() { return manualCashAmount; }
        public void setManualCashAmount(BigDecimal manualCashAmount) { this.manualCashAmount = manualCashAmount; }

        public Integer getBankTransferWithdrawals() { return bankTransferWithdrawals; }
        public void setBankTransferWithdrawals(Integer bankTransferWithdrawals) { this.bankTransferWithdrawals = bankTransferWithdrawals; }

        public BigDecimal getBankTransferAmount() { return bankTransferAmount; }
        public void setBankTransferAmount(BigDecimal bankTransferAmount) { this.bankTransferAmount = bankTransferAmount; }
    }
}
