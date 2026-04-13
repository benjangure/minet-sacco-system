package com.minet.sacco.service;

import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Consolidated loan disbursement service used by both individual and bulk workflows
 */
@Service
public class LoanDisbursementService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private LoanNumberGenerationService loanNumberGenerationService;

    @Autowired
    private AuditService auditService;

    /**
     * Disburse a loan with all required operations
     * - Verify and recalculate loan calculations if needed
     * - Generate loan number
     * - Credit member account
     * - Create transaction
     * - Update guarantor status to ACTIVE
     */
    @Transactional
    public Loan disburseLoan(Loan loan, User disbursedBy) {
        // Refresh loan from database to get latest status
        Loan freshLoan = loanRepository.findById(loan.getId())
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        
        if (freshLoan.getStatus() != Loan.Status.APPROVED) {
            throw new RuntimeException("Loan must be APPROVED before disbursement. Current status: " + freshLoan.getStatus());
        }
        
        loan = freshLoan;

        // Verify and recalculate loan calculations if they're missing or zero
        if (loan.getMonthlyRepayment() == null || loan.getMonthlyRepayment().compareTo(BigDecimal.ZERO) == 0 ||
            loan.getTotalInterest() == null || loan.getTotalInterest().compareTo(BigDecimal.ZERO) == 0 ||
            loan.getTotalRepayable() == null || loan.getTotalRepayable().compareTo(BigDecimal.ZERO) == 0) {
            
            // Recalculate from amount, interest rate, and term
            if (loan.getAmount() != null && loan.getInterestRate() != null && loan.getTermMonths() != null) {
                BigDecimal principal = loan.getAmount();
                BigDecimal annualRate = loan.getInterestRate();
                Integer termMonths = loan.getTermMonths();
                
                // Simple interest calculation: Interest = Principal * Rate * Time
                BigDecimal rate = annualRate.divide(BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP);
                BigDecimal timeInYears = BigDecimal.valueOf(termMonths).divide(BigDecimal.valueOf(12), 4, java.math.RoundingMode.HALF_UP);
                BigDecimal totalInterest = principal.multiply(rate).multiply(timeInYears).setScale(2, java.math.RoundingMode.HALF_UP);
                BigDecimal totalRepayable = principal.add(totalInterest);
                BigDecimal monthlyRepayment = totalRepayable.divide(BigDecimal.valueOf(termMonths), 2, java.math.RoundingMode.HALF_UP);
                
                loan.setTotalInterest(totalInterest);
                loan.setTotalRepayable(totalRepayable);
                loan.setMonthlyRepayment(monthlyRepayment);
                loan.setOutstandingBalance(totalRepayable);
            }
        }

        // Generate and assign loan number
        String loanNumber = loanNumberGenerationService.generateLoanNumber(loan);
        loan.setLoanNumber(loanNumber);

        // Update loan status
        loan.setStatus(Loan.Status.DISBURSED);
        loan.setDisbursementDate(LocalDateTime.now());
        Loan updatedLoan = loanRepository.save(loan);

        // Credit member's account
        Account account = accountRepository.findByMemberIdAndAccountType(
                updatedLoan.getMember().getId(), Account.AccountType.SAVINGS)
                .orElseGet(() -> {
                    Account newAccount = new Account();
                    newAccount.setMember(updatedLoan.getMember());
                    newAccount.setAccountType(Account.AccountType.SAVINGS);
                    newAccount.setBalance(BigDecimal.ZERO);
                    newAccount.setCreatedAt(LocalDateTime.now());
                    return accountRepository.save(newAccount);
                });

        account.setBalance(account.getBalance().add(updatedLoan.getAmount()));
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setAccount(account);
        transaction.setTransactionType(Transaction.TransactionType.LOAN_DISBURSEMENT);
        transaction.setAmount(updatedLoan.getAmount());
        transaction.setDescription("Loan disbursement - Loan Number: " + loanNumber);
        transaction.setCreatedBy(disbursedBy);
        transactionRepository.save(transaction);

        // Update guarantor status to ACTIVE
        updateGuarantorStatusToActive(updatedLoan);

        // Freeze self-guarantor savings
        freezeSelfGuarantorSavings(updatedLoan);

        // Log audit event
        String loanDetails = "Loan #" + updatedLoan.getLoanNumber() + " - Member: " + updatedLoan.getMember().getFirstName() + " " + 
                            updatedLoan.getMember().getLastName() + " - Amount: KES " + updatedLoan.getAmount();
        auditService.logAction(disbursedBy, "DISBURSE", "LOAN", updatedLoan.getId(), loanDetails, "Loan disbursed to member account", "SUCCESS");

        return updatedLoan;
    }

    /**
     * Update all guarantors for a loan to ACTIVE status and freeze their pledge amount
     */
    @Transactional
    public void updateGuarantorStatusToActive(Loan loan) {
        java.util.List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());
        for (Guarantor guarantor : guarantors) {
            guarantor.setStatus(Guarantor.Status.ACTIVE);
            // Freeze the guarantor's pledge amount equal to their guarantee amount
            // If guarantee_amount is not set, use the loan amount (for backward compatibility)
            BigDecimal pledgeAmount = guarantor.getGuaranteeAmount() != null && guarantor.getGuaranteeAmount().compareTo(BigDecimal.ZERO) > 0
                    ? guarantor.getGuaranteeAmount()
                    : loan.getAmount();
            guarantor.setPledgeAmount(pledgeAmount);
            guarantorRepository.save(guarantor);
        }
    }

    /**
     * Update guarantor status based on eligibility
     */
    @Transactional
    public void updateGuarantorStatus(Loan loan, boolean eligible) {
        java.util.List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());
        for (Guarantor guarantor : guarantors) {
            if (eligible) {
                guarantor.setStatus(Guarantor.Status.ACCEPTED);
            } else {
                guarantor.setStatus(Guarantor.Status.REJECTED);
            }
            guarantorRepository.save(guarantor);
        }
    }

    /**
     * Freeze savings for self-guarantors
     * When a loan is disbursed, self-guarantor savings are frozen equal to their guarantee amount
     */
    @Transactional
    public void freezeSelfGuarantorSavings(Loan loan) {
        java.util.List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());
        
        for (Guarantor guarantor : guarantors) {
            // Only freeze for self-guarantors
            if (!guarantor.isSelfGuarantee()) {
                continue;
            }
            
            // Get self-guarantor's savings account
            Account savingsAccount = accountRepository
                    .findByMemberIdAndAccountType(guarantor.getMember().getId(), Account.AccountType.SAVINGS)
                    .orElse(null);
            
            if (savingsAccount == null) {
                continue;
            }
            
            // Freeze the guarantee amount
            BigDecimal freezeAmount = guarantor.getGuaranteeAmount() != null && 
                                     guarantor.getGuaranteeAmount().compareTo(BigDecimal.ZERO) > 0
                    ? guarantor.getGuaranteeAmount()
                    : loan.getAmount();
            
            // Add to frozen savings
            BigDecimal currentFrozen = savingsAccount.getFrozenSavings() != null ? 
                    savingsAccount.getFrozenSavings() : BigDecimal.ZERO;
            savingsAccount.setFrozenSavings(currentFrozen.add(freezeAmount));
            savingsAccount.setUpdatedAt(LocalDateTime.now());
            accountRepository.save(savingsAccount);
        }
    }
}
