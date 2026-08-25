package com.minet.sacco.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for a member's full contribution / transaction history report.
 * Covers all account types or a filtered subset, optionally within a date range.
 */
public class MemberContributionsReportDTO {

    // ── Member info ──────────────────────────────────────────────────────────
    private Long memberId;
    private String memberNumber;
    private String memberName;
    private String email;
    private String phone;

    // ── Report parameters ────────────────────────────────────────────────────
    private LocalDate startDate;
    private LocalDate endDate;
    private String accountTypeFilter; // null = all types
    private LocalDateTime generatedAt;

    // ── Summary per account type ─────────────────────────────────────────────
    private List<AccountSummary> accountSummaries;

    // ── Individual transactions ──────────────────────────────────────────────
    private List<ContributionEntry> entries;

    // ── Totals across all filtered accounts ─────────────────────────────────
    private BigDecimal totalDeposited  = BigDecimal.ZERO;
    private BigDecimal totalWithdrawn  = BigDecimal.ZERO;
    private BigDecimal netContribution = BigDecimal.ZERO;

    // ── Inner classes ────────────────────────────────────────────────────────

    public static class AccountSummary {
        private String accountType;
        private BigDecimal currentBalance;
        private BigDecimal totalDeposited;
        private BigDecimal totalWithdrawn;
        private int transactionCount;

        public String getAccountType()           { return accountType; }
        public void setAccountType(String v)     { this.accountType = v; }
        public BigDecimal getCurrentBalance()    { return currentBalance; }
        public void setCurrentBalance(BigDecimal v) { this.currentBalance = v; }
        public BigDecimal getTotalDeposited()    { return totalDeposited; }
        public void setTotalDeposited(BigDecimal v) { this.totalDeposited = v; }
        public BigDecimal getTotalWithdrawn()    { return totalWithdrawn; }
        public void setTotalWithdrawn(BigDecimal v) { this.totalWithdrawn = v; }
        public int getTransactionCount()         { return transactionCount; }
        public void setTransactionCount(int v)   { this.transactionCount = v; }
    }

    public static class ContributionEntry {
        private Long transactionId;
        private LocalDateTime transactionDate;
        private String transactionType;
        private String accountType;
        private BigDecimal amount;
        private String description;
        private String processedBy; // username of createdBy

        public Long getTransactionId()           { return transactionId; }
        public void setTransactionId(Long v)     { this.transactionId = v; }
        public LocalDateTime getTransactionDate() { return transactionDate; }
        public void setTransactionDate(LocalDateTime v) { this.transactionDate = v; }
        public String getTransactionType()       { return transactionType; }
        public void setTransactionType(String v) { this.transactionType = v; }
        public String getAccountType()           { return accountType; }
        public void setAccountType(String v)     { this.accountType = v; }
        public BigDecimal getAmount()            { return amount; }
        public void setAmount(BigDecimal v)      { this.amount = v; }
        public String getDescription()           { return description; }
        public void setDescription(String v)     { this.description = v; }
        public String getProcessedBy()           { return processedBy; }
        public void setProcessedBy(String v)     { this.processedBy = v; }
    }

    // ── Getters / setters ─────────────────────────────────────────────────────

    public Long getMemberId()                          { return memberId; }
    public void setMemberId(Long v)                    { this.memberId = v; }
    public String getMemberNumber()                    { return memberNumber; }
    public void setMemberNumber(String v)              { this.memberNumber = v; }
    public String getMemberName()                      { return memberName; }
    public void setMemberName(String v)                { this.memberName = v; }
    public String getEmail()                           { return email; }
    public void setEmail(String v)                     { this.email = v; }
    public String getPhone()                           { return phone; }
    public void setPhone(String v)                     { this.phone = v; }
    public LocalDate getStartDate()                    { return startDate; }
    public void setStartDate(LocalDate v)              { this.startDate = v; }
    public LocalDate getEndDate()                      { return endDate; }
    public void setEndDate(LocalDate v)                { this.endDate = v; }
    public String getAccountTypeFilter()               { return accountTypeFilter; }
    public void setAccountTypeFilter(String v)         { this.accountTypeFilter = v; }
    public LocalDateTime getGeneratedAt()              { return generatedAt; }
    public void setGeneratedAt(LocalDateTime v)        { this.generatedAt = v; }
    public List<AccountSummary> getAccountSummaries()  { return accountSummaries; }
    public void setAccountSummaries(List<AccountSummary> v) { this.accountSummaries = v; }
    public List<ContributionEntry> getEntries()        { return entries; }
    public void setEntries(List<ContributionEntry> v)  { this.entries = v; }
    public BigDecimal getTotalDeposited()              { return totalDeposited; }
    public void setTotalDeposited(BigDecimal v)        { this.totalDeposited = v; }
    public BigDecimal getTotalWithdrawn()              { return totalWithdrawn; }
    public void setTotalWithdrawn(BigDecimal v)        { this.totalWithdrawn = v; }
    public BigDecimal getNetContribution()             { return netContribution; }
    public void setNetContribution(BigDecimal v)       { this.netContribution = v; }
}
