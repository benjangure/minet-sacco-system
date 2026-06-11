package com.minet.sacco.service;

import com.minet.sacco.entity.Account;
import com.minet.sacco.entity.Loan;
import com.minet.sacco.entity.LoanRepayment;
import com.minet.sacco.entity.Transaction;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.AccountRepository;
import com.minet.sacco.repository.LoanRepository;
import com.minet.sacco.repository.LoanRepaymentRepository;
import com.minet.sacco.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoanRepaymentService {

    @Autowired
    private LoanRepaymentRepository loanRepaymentRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private GuarantorTrackingService guarantorTrackingService;

    @Autowired
    private AuditService auditService;

    /**
     * Record a loan repayment and update outstanding balance
     */
    @Transactional
    public LoanRepayment recordRepayment(Long loanId, BigDecimal amount, BigDecimal principalAmount, BigDecimal interestAmount,
                                         LoanRepayment.PaymentMethod paymentMethod, String referenceNumber, LocalDateTime paymentDate, User recordedBy) {
        
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.Status.DISBURSED && loan.getStatus() != Loan.Status.REPAID) {
            throw new RuntimeException("Can only record repayments for DISBURSED loans");
        }

        BigDecimal principal = principalAmount != null ? principalAmount.setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal interest = interestAmount != null ? interestAmount.setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

        if ((amount == null || amount.compareTo(BigDecimal.ZERO) == 0)
                && (principal.compareTo(BigDecimal.ZERO) > 0 || interest.compareTo(BigDecimal.ZERO) > 0)) {
            amount = principal.add(interest);
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Repayment amount must be greater than zero");
        }

        if (principal.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Principal amount cannot be negative");
        }

        if (interest.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Interest amount cannot be negative");
        }

        if (principal.add(interest).compareTo(amount) != 0) {
            throw new RuntimeException("Repayment total must equal principal plus interest");
        }

        if (amount.compareTo(loan.getOutstandingBalance()) > 0) {
            throw new RuntimeException("Repayment amount cannot exceed outstanding balance of KES " + loan.getOutstandingBalance());
        }

        // Create repayment record
        LoanRepayment repayment = new LoanRepayment();
        repayment.setLoan(loan);
        repayment.setAmount(amount);
        repayment.setPrincipalAmount(principal);
        repayment.setInterestAmount(interest);
        repayment.setPaymentMethod(paymentMethod);
        repayment.setReferenceNumber(referenceNumber);
        repayment.setPaymentDate(paymentDate);
        repayment.setRecordedBy(recordedBy);
        repayment.setCreatedAt(LocalDateTime.now());
        repayment.setUpdatedAt(LocalDateTime.now());

        LoanRepayment savedRepayment = loanRepaymentRepository.save(repayment);

        // Update loan outstanding balance
        BigDecimal newOutstandingBalance = loan.getOutstandingBalance().subtract(amount);
        loan.setOutstandingBalance(newOutstandingBalance);

        if (interest.compareTo(BigDecimal.ZERO) > 0 && loan.getInterestRemaining() != null) {
            BigDecimal newInterestRemaining = loan.getInterestRemaining().subtract(interest);
            if (newInterestRemaining.compareTo(BigDecimal.ZERO) < 0) {
                newInterestRemaining = BigDecimal.ZERO;
            }
            loan.setInterestRemaining(newInterestRemaining);
        }

        // If fully repaid, update status
        if (newOutstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(Loan.Status.REPAID);
        }

        loanRepository.save(loan);

        // Preserve final amount for use in lambda
        final BigDecimal repaymentAmount = amount;
        final BigDecimal interestForLambda = interest;

        // Create transaction record so repayment appears in Member Transaction History
        accountRepository.findByMemberIdAndAccountType(loan.getMember().getId(), Account.AccountType.SAVINGS)
                .ifPresent(account -> {
                    // Create LOAN_REPAYMENT transaction for the full amount
                    Transaction transaction = new Transaction();
                    transaction.setAccount(account);
                    transaction.setTransactionType(Transaction.TransactionType.LOAN_REPAYMENT);
                    transaction.setAmount(repaymentAmount);
                    transaction.setDescription("Loan repayment - Loan #" + loan.getLoanNumber() +
                            " - Method: " + paymentMethod +
                            (referenceNumber != null && !referenceNumber.isEmpty() ? " - Ref: " + referenceNumber : ""));
                    transaction.setTransactionDate(paymentDate != null ? paymentDate : LocalDateTime.now());
                    transaction.setCreatedBy(recordedBy);
                    transactionRepository.save(transaction);

                    // Create separate INTEREST transaction if interest was paid
                    if (interestForLambda.compareTo(BigDecimal.ZERO) > 0) {
                        Transaction interestTransaction = new Transaction();
                        interestTransaction.setAccount(account);
                        interestTransaction.setTransactionType(Transaction.TransactionType.INTEREST);
                        interestTransaction.setAmount(interestForLambda);
                        interestTransaction.setDescription("Interest income - Loan #" + loan.getLoanNumber() +
                                (referenceNumber != null && !referenceNumber.isEmpty() ? " - Ref: " + referenceNumber : ""));
                        interestTransaction.setTransactionDate(paymentDate != null ? paymentDate : LocalDateTime.now());
                        interestTransaction.setCreatedBy(recordedBy);
                        transactionRepository.save(interestTransaction);
                    }
                });

        // Update guarantor pledge tracking (reduce pledge for self-guarantors)
        guarantorTrackingService.trackPledgeReduction(loan, amount);

        // Log audit event
        String auditDetails = "Loan #" + loan.getLoanNumber() + " - Member: " + loan.getMember().getFirstName() + " " + 
                            loan.getMember().getLastName() + " - Repayment: KES " + amount + " - Method: " + paymentMethod;
        auditService.logAction(recordedBy, "REPAY", "LOAN", loanId, auditDetails, 
                             "Loan repayment recorded. Outstanding balance: KES " + newOutstandingBalance, "SUCCESS");

        return savedRepayment;
    }

    /**
     * Get all repayments for a loan
     */
    public List<LoanRepayment> getRepaymentHistory(Long loanId) {
        return loanRepaymentRepository.findByLoanIdOrderByPaymentDateDesc(loanId);
    }

    /**
     * Get total repaid amount for a loan
     */
    public BigDecimal getTotalRepaidAmount(Long loanId) {
        return loanRepaymentRepository.getTotalRepaidAmount(loanId);
    }

    /**
     * Get repayments for a loan within date range
     */
    public List<LoanRepayment> getRepaymentsByDateRange(Long loanId, LocalDateTime startDate, LocalDateTime endDate) {
        return loanRepaymentRepository.findByLoanIdAndDateRange(loanId, startDate, endDate);
    }

    /**
     * Calculate amortization schedule for a loan
     */
    public LoanAmortizationSchedule calculateAmortizationSchedule(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        BigDecimal totalRepaid = getTotalRepaidAmount(loanId);
        BigDecimal outstandingBalance = loan.getOutstandingBalance();
        BigDecimal monthlyPayment = loan.getMonthlyRepayment();
        
        // Calculate remaining months based on multiple factors
        int remainingMonths = 0;
        
        if (loan.getDisbursementDate() != null) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime disbursementDate = loan.getDisbursementDate();
            
            // Calculate actual months elapsed with partial month consideration
            long totalMonthsElapsed = java.time.temporal.ChronoUnit.MONTHS.between(disbursementDate, now);
            int daysInMonth = disbursementDate.plusMonths(1).getDayOfMonth() - disbursementDate.getDayOfMonth();
            long totalDaysElapsed = java.time.temporal.ChronoUnit.DAYS.between(disbursementDate, now);
            int daysElapsed = (int) (totalDaysElapsed % 30);
            double partialMonth = daysElapsed / (double)daysInMonth;
            double monthsElapsed = totalMonthsElapsed + partialMonth;
            
            // Calculate expected remaining based on time
            int timeBasedRemaining = Math.max(0, (int)(loan.getTermMonths() - monthsElapsed));
            
            // Calculate remaining based on actual outstanding balance
            if (outstandingBalance.compareTo(BigDecimal.ZERO) > 0 && monthlyPayment.compareTo(BigDecimal.ZERO) > 0) {
                int paymentBasedRemaining = (int) Math.ceil(outstandingBalance.divide(monthlyPayment, 0, java.math.RoundingMode.UP).doubleValue());
                remainingMonths = Math.min(timeBasedRemaining, paymentBasedRemaining);
            } else if (outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                remainingMonths = 0; // Loan fully paid
            } else {
                remainingMonths = timeBasedRemaining; // Use time-based if payment calculation fails
            }
            
            // Apply loan status adjustments
            if ("DEFAULTED".equals(loan.getStatus())) {
                // For defaulted loans, show all remaining months as due
                remainingMonths = timeBasedRemaining;
            } else if ("COMPLETED".equals(loan.getStatus())) {
                remainingMonths = 0;
            } else if ("APPROVED".equals(loan.getStatus()) && loan.getDisbursementDate() == null) {
                remainingMonths = loan.getTermMonths(); // Not yet disbursed
            }
        } else {
            // No disbursement date - use total term
            remainingMonths = loan.getTermMonths();
        }

        return new LoanAmortizationSchedule(
            loan.getId(),
            loan.getAmount(),
            loan.getTotalRepayable(),
            totalRepaid,
            outstandingBalance,
            monthlyPayment,
            remainingMonths,
            loan.getTermMonths()
        );
    }

    /**
     * DTO for amortization schedule
     */
    public static class LoanAmortizationSchedule {
        public Long loanId;
        public BigDecimal principal;
        public BigDecimal totalRepayable;
        public BigDecimal totalRepaid;
        public BigDecimal outstandingBalance;
        public BigDecimal monthlyPayment;
        public int remainingMonths;
        public int totalMonths;

        public LoanAmortizationSchedule(Long loanId, BigDecimal principal, BigDecimal totalRepayable, 
                                       BigDecimal totalRepaid, BigDecimal outstandingBalance, 
                                       BigDecimal monthlyPayment, int remainingMonths, int totalMonths) {
            this.loanId = loanId;
            this.principal = principal;
            this.totalRepayable = totalRepayable;
            this.totalRepaid = totalRepaid;
            this.outstandingBalance = outstandingBalance;
            this.monthlyPayment = monthlyPayment;
            this.remainingMonths = remainingMonths;
            this.totalMonths = totalMonths;
        }

        // Getters
        public Long getLoanId() { return loanId; }
        public BigDecimal getPrincipal() { return principal; }
        public BigDecimal getTotalRepayable() { return totalRepayable; }
        public BigDecimal getTotalRepaid() { return totalRepaid; }
        public BigDecimal getOutstandingBalance() { return outstandingBalance; }
        public BigDecimal getMonthlyPayment() { return monthlyPayment; }
        public int getRemainingMonths() { return remainingMonths; }
        public int getTotalMonths() { return totalMonths; }
    }
}
