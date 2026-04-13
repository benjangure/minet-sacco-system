package com.minet.sacco.service;

import com.minet.sacco.entity.Loan;
import com.minet.sacco.entity.LoanRepayment;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.LoanRepository;
import com.minet.sacco.repository.LoanRepaymentRepository;
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
    private GuarantorTrackingService guarantorTrackingService;

    @Autowired
    private AuditService auditService;

    /**
     * Record a loan repayment and update outstanding balance
     */
    @Transactional
    public LoanRepayment recordRepayment(Long loanId, BigDecimal amount, LoanRepayment.PaymentMethod paymentMethod, 
                                         String referenceNumber, LocalDateTime paymentDate, User recordedBy) {
        
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.Status.DISBURSED && loan.getStatus() != Loan.Status.REPAID) {
            throw new RuntimeException("Can only record repayments for DISBURSED loans");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Repayment amount must be greater than zero");
        }

        if (amount.compareTo(loan.getOutstandingBalance()) > 0) {
            throw new RuntimeException("Repayment amount cannot exceed outstanding balance of KES " + loan.getOutstandingBalance());
        }

        // Create repayment record
        LoanRepayment repayment = new LoanRepayment();
        repayment.setLoan(loan);
        repayment.setAmount(amount);
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

        // If fully repaid, update status
        if (newOutstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(Loan.Status.REPAID);
        }

        loanRepository.save(loan);

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
        
        // Calculate remaining months based on elapsed time since disbursement
        int remainingMonths = loan.getTermMonths(); // Default to total term
        
        if (loan.getDisbursementDate() != null) {
            // Calculate months elapsed since disbursement
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime disbursementDate = loan.getDisbursementDate();
            
            long monthsElapsed = java.time.temporal.ChronoUnit.MONTHS.between(disbursementDate, now);
            
            // Remaining months = Total months - Months elapsed
            remainingMonths = Math.max(0, (int)(loan.getTermMonths() - monthsElapsed));
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
