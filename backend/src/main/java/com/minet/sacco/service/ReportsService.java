package com.minet.sacco.service;

import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportsService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private LoanRepaymentRepository loanRepaymentRepository;

    /**
     * Generate Cashbook - Daily transaction log with filters
     */
    public CashbookReport generateCashbook(LocalDate startDate, LocalDate endDate, 
                                          String memberNumber, String transactionType, String accountType) {
        CashbookReport report = new CashbookReport();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setGeneratedAt(LocalDateTime.now());

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Transaction> transactions = transactionRepository.findAll().stream()
                .filter(t -> t.getTransactionDate() != null && 
                           !t.getTransactionDate().isBefore(startDateTime) && 
                           !t.getTransactionDate().isAfter(endDateTime))
                .filter(t -> memberNumber == null || memberNumber.isEmpty() || 
                           t.getAccount().getMember().getMemberNumber().equals(memberNumber))
                .filter(t -> transactionType == null || transactionType.isEmpty() || 
                           t.getTransactionType().toString().equals(transactionType))
                .filter(t -> accountType == null || accountType.isEmpty() || 
                           t.getAccount().getAccountType().toString().equals(accountType))
                .sorted(Comparator.comparing(Transaction::getTransactionDate))
                .collect(Collectors.toList());

        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        BigDecimal totalRepayments = BigDecimal.ZERO;

        List<CashbookEntry> entries = new ArrayList<>();
        for (Transaction transaction : transactions) {
            CashbookEntry entry = new CashbookEntry();
            entry.setDate(transaction.getTransactionDate().toLocalDate());
            entry.setTransactionType(transaction.getTransactionType().toString());
            entry.setMemberNumber(transaction.getAccount().getMember().getMemberNumber());
            entry.setMemberName(transaction.getAccount().getMember().getFirstName() + " " + 
                               transaction.getAccount().getMember().getLastName());
            entry.setAccountType(transaction.getAccount().getAccountType().toString());
            entry.setAmount(transaction.getAmount());
            entry.setDescription(transaction.getDescription());

            entries.add(entry);

            // Calculate totals
            switch (transaction.getTransactionType()) {
                case DEPOSIT, LOAN_DISBURSEMENT -> totalDeposits = totalDeposits.add(transaction.getAmount());
                case WITHDRAWAL -> totalWithdrawals = totalWithdrawals.add(transaction.getAmount());
                case LOAN_REPAYMENT -> totalRepayments = totalRepayments.add(transaction.getAmount());
                default -> {}
            }
        }

        report.setEntries(entries);
        report.setTotalDeposits(totalDeposits);
        report.setTotalWithdrawals(totalWithdrawals);
        report.setTotalRepayments(totalRepayments);
        report.setNetCash(totalDeposits.add(totalRepayments).subtract(totalWithdrawals));

        return report;
    }

    /**
     * Generate Trial Balance - All accounts with debit/credit balances
     */
    public TrialBalanceReport generateTrialBalance(String memberNumber, String accountType) {
        TrialBalanceReport report = new TrialBalanceReport();
        report.setGeneratedAt(LocalDateTime.now());

        List<Account> accounts = accountRepository.findAll().stream()
                .filter(a -> memberNumber == null || memberNumber.isEmpty() || 
                           a.getMember().getMemberNumber().equals(memberNumber))
                .filter(a -> accountType == null || accountType.isEmpty() || 
                           a.getAccountType().toString().equals(accountType))
                .toList();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        List<TrialBalanceEntry> entries = new ArrayList<>();
        for (Account account : accounts) {
            TrialBalanceEntry entry = new TrialBalanceEntry();
            entry.setMemberNumber(account.getMember().getMemberNumber());
            entry.setMemberName(account.getMember().getFirstName() + " " + 
                               account.getMember().getLastName());
            entry.setAccountType(account.getAccountType().toString());
            entry.setBalance(account.getBalance());

            // In SACCO: Member accounts are liabilities (credits)
            entry.setDebit(BigDecimal.ZERO);
            entry.setCredit(account.getBalance());

            entries.add(entry);
            totalCredits = totalCredits.add(account.getBalance());
        }

        // Add loan accounts (assets - debits)
        List<Loan> loans = loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.Status.DISBURSED || l.getStatus() == Loan.Status.APPROVED)
                .collect(Collectors.toList());

        for (Loan loan : loans) {
            TrialBalanceEntry entry = new TrialBalanceEntry();
            entry.setMemberNumber(loan.getMember().getMemberNumber());
            entry.setMemberName(loan.getMember().getFirstName() + " " + 
                               loan.getMember().getLastName());
            entry.setAccountType("LOAN");
            entry.setBalance(loan.getOutstandingBalance());
            entry.setDebit(loan.getOutstandingBalance());
            entry.setCredit(BigDecimal.ZERO);

            entries.add(entry);
            totalDebits = totalDebits.add(loan.getOutstandingBalance());
        }

        report.setEntries(entries);
        report.setTotalDebits(totalDebits);
        report.setTotalCredits(totalCredits);
        report.setIsBalanced(totalDebits.compareTo(totalCredits) == 0);

        return report;
    }

    /**
     * Generate Balance Sheet - Assets, Liabilities, Equity
     */
    public BalanceSheetReport generateBalanceSheet() {
        BalanceSheetReport report = new BalanceSheetReport();
        report.setGeneratedAt(LocalDateTime.now());

        // ASSETS: Loans outstanding
        List<Loan> activeLoans = loanRepository.findAll().stream()
                .filter(l -> l.getStatus() == Loan.Status.DISBURSED || l.getStatus() == Loan.Status.APPROVED)
                .collect(Collectors.toList());

        BigDecimal totalLoansOutstanding = activeLoans.stream()
                .map(Loan::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        report.setTotalAssets(totalLoansOutstanding);

        // LIABILITIES: Member savings and shares
        List<Account> accounts = accountRepository.findAll();
        BigDecimal totalSavings = BigDecimal.ZERO;
        BigDecimal totalShares = BigDecimal.ZERO;

        for (Account account : accounts) {
            if (account.getAccountType() == Account.AccountType.SAVINGS) {
                totalSavings = totalSavings.add(account.getBalance());
            } else if (account.getAccountType() == Account.AccountType.SHARES) {
                totalShares = totalShares.add(account.getBalance());
            }
        }

        BigDecimal totalLiabilities = totalSavings.add(totalShares);
        report.setTotalSavings(totalSavings);
        report.setTotalShares(totalShares);
        report.setTotalLiabilities(totalLiabilities);

        // EQUITY: Assets - Liabilities
        BigDecimal equity = totalLoansOutstanding.subtract(totalLiabilities);
        report.setEquity(equity);

        return report;
    }

    /**
     * Generate Member Statement - Individual account history
     */
    public MemberStatementReport generateMemberStatement(Long memberId, LocalDate startDate, LocalDate endDate) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        MemberStatementReport report = new MemberStatementReport();
        report.setMemberId(memberId);
        report.setMemberNumber(member.getMemberNumber());
        report.setMemberName(member.getFirstName() + " " + member.getLastName());
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setGeneratedAt(LocalDateTime.now());

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        // Get all accounts for member
        List<Account> memberAccounts = accountRepository.findByMemberId(memberId);

        List<MemberStatementEntry> entries = new ArrayList<>();
        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;

        for (Account account : memberAccounts) {
            List<Transaction> transactions = transactionRepository.findAll().stream()
                    .filter(t -> t.getAccount().getId().equals(account.getId()) &&
                               t.getTransactionDate() != null &&
                               !t.getTransactionDate().isBefore(startDateTime) &&
                               !t.getTransactionDate().isAfter(endDateTime))
                    .sorted(Comparator.comparing(Transaction::getTransactionDate))
                    .collect(Collectors.toList());

            for (Transaction transaction : transactions) {
                MemberStatementEntry entry = new MemberStatementEntry();
                entry.setDate(transaction.getTransactionDate().toLocalDate());
                entry.setAccountType(account.getAccountType().toString());
                entry.setTransactionType(transaction.getTransactionType().toString());
                entry.setAmount(transaction.getAmount());
                entry.setDescription(transaction.getDescription());

                entries.add(entry);

                if (transaction.getTransactionType() == Transaction.TransactionType.DEPOSIT ||
                    transaction.getTransactionType() == Transaction.TransactionType.LOAN_DISBURSEMENT) {
                    totalDeposits = totalDeposits.add(transaction.getAmount());
                } else if (transaction.getTransactionType() == Transaction.TransactionType.WITHDRAWAL) {
                    totalWithdrawals = totalWithdrawals.add(transaction.getAmount());
                }
            }
        }

        report.setEntries(entries);
        report.setTotalDeposits(totalDeposits);
        report.setTotalWithdrawals(totalWithdrawals);

        // Current balances
        Map<String, BigDecimal> currentBalances = new HashMap<>();
        for (Account account : memberAccounts) {
            currentBalances.put(account.getAccountType().toString(), account.getBalance());
        }
        report.setCurrentBalances(currentBalances);

        return report;
    }

    /**
     * Generate Loan Register - All loans with status and repayment schedule
     */
    public LoanRegisterReport generateLoanRegister(String memberNumber, String loanStatus, String loanProduct) {
        LoanRegisterReport report = new LoanRegisterReport();
        report.setGeneratedAt(LocalDateTime.now());

        List<Loan> loans = loanRepository.findAll().stream()
                .filter(l -> memberNumber == null || memberNumber.isEmpty() || 
                           l.getMember().getMemberNumber().equals(memberNumber))
                .filter(l -> loanStatus == null || loanStatus.isEmpty() || 
                           l.getStatus().toString().equals(loanStatus))
                .filter(l -> loanProduct == null || loanProduct.isEmpty() || 
                           l.getLoanProduct().getName().equals(loanProduct))
                .toList();
        List<LoanRegisterEntry> entries = new ArrayList<>();

        BigDecimal totalLoansIssued = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        BigDecimal totalRepaid = BigDecimal.ZERO;

        for (Loan loan : loans) {
            LoanRegisterEntry entry = new LoanRegisterEntry();
            entry.setLoanNumber(loan.getLoanNumber());
            entry.setMemberNumber(loan.getMember().getMemberNumber());
            entry.setMemberName(loan.getMember().getFirstName() + " " + loan.getMember().getLastName());
            entry.setLoanProduct(loan.getLoanProduct().getName());
            entry.setAmount(loan.getAmount());
            entry.setInterestRate(loan.getInterestRate());
            entry.setTermMonths(loan.getTermMonths());
            entry.setMonthlyRepayment(loan.getMonthlyRepayment());
            entry.setStatus(loan.getStatus().toString());
            entry.setApplicationDate(loan.getApplicationDate());
            entry.setApprovalDate(loan.getApprovalDate());
            entry.setDisbursementDate(loan.getDisbursementDate());
            entry.setOutstandingBalance(loan.getOutstandingBalance());

            entries.add(entry);

            totalLoansIssued = totalLoansIssued.add(loan.getAmount());
            totalOutstanding = totalOutstanding.add(loan.getOutstandingBalance());

            // Calculate total repaid
            BigDecimal totalRepaidForLoan = loanRepaymentRepository.getTotalRepaidAmount(loan.getId());
            if (totalRepaidForLoan != null) {
                totalRepaid = totalRepaid.add(totalRepaidForLoan);
            }
        }

        report.setEntries(entries);
        report.setTotalLoansIssued(totalLoansIssued);
        report.setTotalOutstanding(totalOutstanding);
        report.setTotalRepaid(totalRepaid);

        return report;
    }

    // Report DTOs
    public static class CashbookReport {
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime generatedAt;
        private List<CashbookEntry> entries;
        private BigDecimal totalDeposits;
        private BigDecimal totalWithdrawals;
        private BigDecimal totalRepayments;
        private BigDecimal netCash;

        // Getters and Setters
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public List<CashbookEntry> getEntries() { return entries; }
        public void setEntries(List<CashbookEntry> entries) { this.entries = entries; }
        public BigDecimal getTotalDeposits() { return totalDeposits; }
        public void setTotalDeposits(BigDecimal totalDeposits) { this.totalDeposits = totalDeposits; }
        public BigDecimal getTotalWithdrawals() { return totalWithdrawals; }
        public void setTotalWithdrawals(BigDecimal totalWithdrawals) { this.totalWithdrawals = totalWithdrawals; }
        public BigDecimal getTotalRepayments() { return totalRepayments; }
        public void setTotalRepayments(BigDecimal totalRepayments) { this.totalRepayments = totalRepayments; }
        public BigDecimal getNetCash() { return netCash; }
        public void setNetCash(BigDecimal netCash) { this.netCash = netCash; }
    }

    public static class CashbookEntry {
        private LocalDate date;
        private String transactionType;
        private String memberNumber;
        private String memberName;
        private String accountType;
        private BigDecimal amount;
        private String description;

        // Getters and Setters
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public String getTransactionType() { return transactionType; }
        public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
        public String getMemberNumber() { return memberNumber; }
        public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }
        public String getMemberName() { return memberName; }
        public void setMemberName(String memberName) { this.memberName = memberName; }
        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class TrialBalanceReport {
        private LocalDateTime generatedAt;
        private List<TrialBalanceEntry> entries;
        private BigDecimal totalDebits;
        private BigDecimal totalCredits;
        private Boolean isBalanced;

        // Getters and Setters
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public List<TrialBalanceEntry> getEntries() { return entries; }
        public void setEntries(List<TrialBalanceEntry> entries) { this.entries = entries; }
        public BigDecimal getTotalDebits() { return totalDebits; }
        public void setTotalDebits(BigDecimal totalDebits) { this.totalDebits = totalDebits; }
        public BigDecimal getTotalCredits() { return totalCredits; }
        public void setTotalCredits(BigDecimal totalCredits) { this.totalCredits = totalCredits; }
        public Boolean getIsBalanced() { return isBalanced; }
        public void setIsBalanced(Boolean isBalanced) { this.isBalanced = isBalanced; }
    }

    public static class TrialBalanceEntry {
        private String memberNumber;
        private String memberName;
        private String accountType;
        private BigDecimal balance;
        private BigDecimal debit;
        private BigDecimal credit;

        // Getters and Setters
        public String getMemberNumber() { return memberNumber; }
        public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }
        public String getMemberName() { return memberName; }
        public void setMemberName(String memberName) { this.memberName = memberName; }
        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }
        public BigDecimal getBalance() { return balance; }
        public void setBalance(BigDecimal balance) { this.balance = balance; }
        public BigDecimal getDebit() { return debit; }
        public void setDebit(BigDecimal debit) { this.debit = debit; }
        public BigDecimal getCredit() { return credit; }
        public void setCredit(BigDecimal credit) { this.credit = credit; }
    }

    public static class BalanceSheetReport {
        private LocalDateTime generatedAt;
        private BigDecimal totalAssets;
        private BigDecimal totalSavings;
        private BigDecimal totalShares;
        private BigDecimal totalLiabilities;
        private BigDecimal equity;

        // Getters and Setters
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public BigDecimal getTotalAssets() { return totalAssets; }
        public void setTotalAssets(BigDecimal totalAssets) { this.totalAssets = totalAssets; }
        public BigDecimal getTotalSavings() { return totalSavings; }
        public void setTotalSavings(BigDecimal totalSavings) { this.totalSavings = totalSavings; }
        public BigDecimal getTotalShares() { return totalShares; }
        public void setTotalShares(BigDecimal totalShares) { this.totalShares = totalShares; }
        public BigDecimal getTotalLiabilities() { return totalLiabilities; }
        public void setTotalLiabilities(BigDecimal totalLiabilities) { this.totalLiabilities = totalLiabilities; }
        public BigDecimal getEquity() { return equity; }
        public void setEquity(BigDecimal equity) { this.equity = equity; }
    }

    public static class MemberStatementReport {
        private Long memberId;
        private String memberNumber;
        private String memberName;
        private LocalDate startDate;
        private LocalDate endDate;
        private LocalDateTime generatedAt;
        private List<MemberStatementEntry> entries;
        private BigDecimal totalDeposits;
        private BigDecimal totalWithdrawals;
        private Map<String, BigDecimal> currentBalances;

        // Getters and Setters
        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }
        public String getMemberNumber() { return memberNumber; }
        public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }
        public String getMemberName() { return memberName; }
        public void setMemberName(String memberName) { this.memberName = memberName; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public List<MemberStatementEntry> getEntries() { return entries; }
        public void setEntries(List<MemberStatementEntry> entries) { this.entries = entries; }
        public BigDecimal getTotalDeposits() { return totalDeposits; }
        public void setTotalDeposits(BigDecimal totalDeposits) { this.totalDeposits = totalDeposits; }
        public BigDecimal getTotalWithdrawals() { return totalWithdrawals; }
        public void setTotalWithdrawals(BigDecimal totalWithdrawals) { this.totalWithdrawals = totalWithdrawals; }
        public Map<String, BigDecimal> getCurrentBalances() { return currentBalances; }
        public void setCurrentBalances(Map<String, BigDecimal> currentBalances) { this.currentBalances = currentBalances; }
    }

    public static class MemberStatementEntry {
        private LocalDate date;
        private String accountType;
        private String transactionType;
        private BigDecimal amount;
        private String description;

        // Getters and Setters
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public String getAccountType() { return accountType; }
        public void setAccountType(String accountType) { this.accountType = accountType; }
        public String getTransactionType() { return transactionType; }
        public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class LoanRegisterReport {
        private LocalDateTime generatedAt;
        private List<LoanRegisterEntry> entries;
        private BigDecimal totalLoansIssued;
        private BigDecimal totalOutstanding;
        private BigDecimal totalRepaid;

        // Getters and Setters
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
        public List<LoanRegisterEntry> getEntries() { return entries; }
        public void setEntries(List<LoanRegisterEntry> entries) { this.entries = entries; }
        public BigDecimal getTotalLoansIssued() { return totalLoansIssued; }
        public void setTotalLoansIssued(BigDecimal totalLoansIssued) { this.totalLoansIssued = totalLoansIssued; }
        public BigDecimal getTotalOutstanding() { return totalOutstanding; }
        public void setTotalOutstanding(BigDecimal totalOutstanding) { this.totalOutstanding = totalOutstanding; }
        public BigDecimal getTotalRepaid() { return totalRepaid; }
        public void setTotalRepaid(BigDecimal totalRepaid) { this.totalRepaid = totalRepaid; }
    }

    public static class LoanRegisterEntry {
        private String loanNumber;
        private String memberNumber;
        private String memberName;
        private String loanProduct;
        private BigDecimal amount;
        private BigDecimal interestRate;
        private Integer termMonths;
        private BigDecimal monthlyRepayment;
        private String status;
        private LocalDateTime applicationDate;
        private LocalDateTime approvalDate;
        private LocalDateTime disbursementDate;
        private BigDecimal outstandingBalance;

        // Getters and Setters
        public String getLoanNumber() { return loanNumber; }
        public void setLoanNumber(String loanNumber) { this.loanNumber = loanNumber; }
        public String getMemberNumber() { return memberNumber; }
        public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }
        public String getMemberName() { return memberName; }
        public void setMemberName(String memberName) { this.memberName = memberName; }
        public String getLoanProduct() { return loanProduct; }
        public void setLoanProduct(String loanProduct) { this.loanProduct = loanProduct; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public BigDecimal getInterestRate() { return interestRate; }
        public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
        public Integer getTermMonths() { return termMonths; }
        public void setTermMonths(Integer termMonths) { this.termMonths = termMonths; }
        public BigDecimal getMonthlyRepayment() { return monthlyRepayment; }
        public void setMonthlyRepayment(BigDecimal monthlyRepayment) { this.monthlyRepayment = monthlyRepayment; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public LocalDateTime getApplicationDate() { return applicationDate; }
        public void setApplicationDate(LocalDateTime applicationDate) { this.applicationDate = applicationDate; }
        public LocalDateTime getApprovalDate() { return approvalDate; }
        public void setApprovalDate(LocalDateTime approvalDate) { this.approvalDate = approvalDate; }
        public LocalDateTime getDisbursementDate() { return disbursementDate; }
        public void setDisbursementDate(LocalDateTime disbursementDate) { this.disbursementDate = disbursementDate; }
        public BigDecimal getOutstandingBalance() { return outstandingBalance; }
        public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    }
}
