package com.minet.sacco.service;

import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for tracking guarantor pledge changes and default debits
 * Handles proportional calculations and audit trail creation
 */
@Service
public class GuarantorTrackingService {

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Track pledge reduction on repayment
     * Calculates new frozen pledge based on outstanding balance
     * Also unfreezes proportional savings for self-guarantors and member
     */
    @Transactional
    public void trackPledgeReduction(Loan loan, BigDecimal repaymentAmount) {
        List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());
        
        if (guarantors.isEmpty()) {
            return;
        }

        BigDecimal outstandingBalance = loan.getOutstandingBalance();
        BigDecimal originalLoanAmount = loan.getAmount();
        BigDecimal originalPrincipal = loan.getOriginalPrincipal() != null ? loan.getOriginalPrincipal() : originalLoanAmount;
        BigDecimal totalRepayable = loan.getTotalRepayable();

        // Derive outstanding principal from outstanding balance.
        // outstandingBalance starts at totalRepayable and decreases with each repayment.
        // Amount repaid = totalRepayable - outstandingBalance
        // Outstanding principal = originalPrincipal - amountRepaid
        BigDecimal outstandingPrincipal;
        if (totalRepayable != null && totalRepayable.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal amountRepaid = totalRepayable.subtract(outstandingBalance);
            outstandingPrincipal = originalPrincipal.subtract(amountRepaid);
        } else {
            outstandingPrincipal = outstandingBalance;
        }
        if (outstandingPrincipal.compareTo(BigDecimal.ZERO) < 0) {
            outstandingPrincipal = BigDecimal.ZERO;
        }

        // Check if member has a self-guarantee
        Guarantor memberSelfGuarantee = guarantors.stream()
                .filter(g -> g.isSelfGuarantee() && g.getMember().getId().equals(loan.getMember().getId()))
                .findFirst()
                .orElse(null);

        // If member has self-guarantee, unfreeze proportional savings based on outstanding principal
        if (memberSelfGuarantee != null) {
            BigDecimal pledgeBefore = memberSelfGuarantee.getPledgeAmount();
            if (pledgeBefore != null && pledgeBefore.compareTo(BigDecimal.ZERO) > 0) {
                // Kenyan SACCO formula: Remaining Frozen = Original Pledge × (Outstanding Principal / Original Principal)
                BigDecimal newFrozenPledge = originalPrincipal.compareTo(BigDecimal.ZERO) > 0 ?
                        pledgeBefore.multiply(outstandingPrincipal)
                                .divide(originalPrincipal, 2, java.math.RoundingMode.HALF_UP) :
                        BigDecimal.ZERO;

                BigDecimal pledgeReduction = pledgeBefore.subtract(newFrozenPledge);

                // Unfreeze only when repayment reduces frozen amount
                if (pledgeReduction.compareTo(BigDecimal.ZERO) > 0) {
                    unfreezeProportionalSavings(memberSelfGuarantee, pledgeReduction);
                }
            }
        }

        // Process external guarantors
        for (Guarantor guarantor : guarantors) {
            if (guarantor.getStatus() != Guarantor.Status.ACTIVE) {
                continue;
            }

            // Skip member's self-guarantee (already processed above)
            if (guarantor.isSelfGuarantee() && guarantor.getMember().getId().equals(loan.getMember().getId())) {
                continue;
            }

            BigDecimal pledgeBefore = guarantor.getPledgeAmount();
            if (pledgeBefore == null || pledgeBefore.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            // Kenyan SACCO formula: Remaining Frozen = Original Pledge × (Outstanding Principal / Original Principal)
            BigDecimal newFrozenPledge = originalPrincipal.compareTo(BigDecimal.ZERO) > 0 ?
                    pledgeBefore.multiply(outstandingPrincipal)
                            .divide(originalPrincipal, 2, java.math.RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            BigDecimal pledgeReduction = pledgeBefore.subtract(newFrozenPledge);
            
            // Update guarantor's frozen pledge
            guarantor.setPledgeAmount(newFrozenPledge);
            guarantorRepository.save(guarantor);

            // Unfreeze proportional savings for ALL guarantors (self and external)
            if (pledgeReduction.compareTo(BigDecimal.ZERO) > 0) {
                unfreezeProportionalSavings(guarantor, pledgeReduction);
            }

            // Create tracking record
            createRepaymentTrackingRecord(guarantor, loan, repaymentAmount, pledgeBefore, newFrozenPledge);

            // Notify guarantor of pledge reduction
            notifyGuarantorOfPledgeReduction(guarantor, loan, pledgeBefore, newFrozenPledge);
        }
    }

    /**
     * Create repayment tracking record for audit trail
     */
    @Transactional
    private void createRepaymentTrackingRecord(Guarantor guarantor, Loan loan, 
                                               BigDecimal repaymentAmount,
                                               BigDecimal frozenBefore, BigDecimal frozenAfter) {
        auditService.logAction(
                null,
                "GUARANTOR_PLEDGE_REDUCED",
                "Guarantor",
                guarantor.getId(),
                null,
                String.format("Pledge reduced from KES %,.2f to KES %,.2f due to repayment of KES %,.2f",
                        frozenBefore, frozenAfter, repaymentAmount),
                "SUCCESS"
        );
    }

    /**
     * Handle default debit for all guarantors
     * Debits guarantor accounts proportionally based on their guarantee ratio
     */
    @Transactional
    public void handleDefaultDebit(Loan loan, BigDecimal defaultAmount, User createdBy) {
        List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());
        
        if (guarantors.isEmpty() || defaultAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal totalLoanAmount = loan.getAmount();

        for (Guarantor guarantor : guarantors) {
            if (guarantor.getStatus() != Guarantor.Status.ACTIVE) {
                continue;
            }

            BigDecimal guarantorPledge = guarantor.getPledgeAmount();
            if (guarantorPledge == null || guarantorPledge.compareTo(BigDecimal.ZERO) == 0) {
                continue;
            }

            // Calculate guarantor's share of default: default_amount × (pledge / total_loan)
            BigDecimal guarantorShare = defaultAmount
                    .multiply(guarantorPledge)
                    .divide(totalLoanAmount, 2, java.math.RoundingMode.HALF_UP);

            // Debit guarantor's savings account
            debitGuarantorAccount(guarantor, guarantorShare, loan, createdBy);

            // Create default tracking record
            createDefaultTrackingRecord(guarantor, loan, defaultAmount, guarantorPledge, guarantorShare);

            // Notify guarantor of default debit
            notifyGuarantorOfDefaultDebit(guarantor, loan, guarantorShare);
        }
    }

    /**
     * Debit guarantor's savings account for default share
     */
    @Transactional
    private void debitGuarantorAccount(Guarantor guarantor, BigDecimal debitAmount, 
                                       Loan loan, User createdBy) {
        Account savingsAccount = accountRepository
                .findByMemberIdAndAccountType(guarantor.getMember().getId(), Account.AccountType.SAVINGS)
                .orElse(null);

        if (savingsAccount == null) {
            throw new RuntimeException("Guarantor savings account not found");
        }

        if (savingsAccount.getBalance().compareTo(debitAmount) < 0) {
            throw new RuntimeException("Insufficient balance for default debit");
        }

        savingsAccount.setBalance(savingsAccount.getBalance().subtract(debitAmount));
        savingsAccount.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(savingsAccount);

        Transaction transaction = new Transaction();
        transaction.setAccount(savingsAccount);
        transaction.setTransactionType(Transaction.TransactionType.LOAN_DEFAULT_DEBIT);
        transaction.setAmount(debitAmount);
        transaction.setDescription("Default debit for loan " + loan.getLoanNumber() + 
                                  " - Guarantor share of default");
        transaction.setCreatedBy(createdBy);
        transaction.setTransactionDate(LocalDateTime.now());
        transactionRepository.save(transaction);
    }

    /**
     * Create default tracking record for audit trail
     */
    @Transactional
    private void createDefaultTrackingRecord(Guarantor guarantor, Loan loan,
                                            BigDecimal defaultAmount, BigDecimal guarantorPledge,
                                            BigDecimal debitAmount) {
        auditService.logAction(
                null,
                "GUARANTOR_DEFAULT_DEBIT",
                "Guarantor",
                guarantor.getId(),
                null,
                String.format("Default debit of KES %,.2f applied (%.1f%% of KES %,.2f default)",
                        debitAmount, 
                        guarantorPledge.divide(loan.getAmount(), 4, java.math.RoundingMode.HALF_UP)
                                .multiply(new BigDecimal("100")),
                        defaultAmount),
                "SUCCESS"
        );
    }

    /**
     * Notify guarantor of pledge reduction
     */
    private void notifyGuarantorOfPledgeReduction(Guarantor guarantor, Loan loan,
                                                  BigDecimal pledgeBefore, BigDecimal pledgeAfter) {
        try {
            User guarantorUser = userRepository.findByMemberId(guarantor.getMember().getId())
                    .orElse(null);
            
            if (guarantorUser == null) {
                return;
            }

            String message = String.format(
                    "Hi %s, %s has made a repayment on their loan. Your frozen guarantee has been reduced from KES %,.2f to KES %,.2f. Your savings are being progressively unfrozen as the loan is repaid.",
                    guarantor.getMember().getFirstName(),
                    loan.getMember().getFirstName() + " " + loan.getMember().getLastName(),
                    pledgeBefore,
                    pledgeAfter
            );

            notificationService.notifyUser(
                    guarantorUser.getId(),
                    message,
                    "GUARANTOR_PLEDGE_REDUCED",
                    loan.getId(),
                    loan.getMember().getId(),
                    "GUARANTOR"
            );
        } catch (Exception e) {
            System.err.println("Error notifying guarantor of pledge reduction: " + e.getMessage());
        }
    }

    /**
     * Notify guarantor of default debit
     */
    private void notifyGuarantorOfDefaultDebit(Guarantor guarantor, Loan loan, BigDecimal debitAmount) {
        try {
            User guarantorUser = userRepository.findByMemberId(guarantor.getMember().getId())
                    .orElse(null);
            
            if (guarantorUser == null) {
                return;
            }

            String message = String.format(
                    "Alert: %s has defaulted on their loan. Your account has been debited KES %,.2f (your proportional share of the default). Please contact the office for more information.",
                    loan.getMember().getFirstName() + " " + loan.getMember().getLastName(),
                    debitAmount
            );

            notificationService.notifyUser(
                    guarantorUser.getId(),
                    message,
                    "GUARANTOR_DEFAULT_DEBIT",
                    loan.getId(),
                    loan.getMember().getId(),
                    "GUARANTOR"
            );
        } catch (Exception e) {
            System.err.println("Error notifying guarantor of default debit: " + e.getMessage());
        }
    }

    /**
     * Release all guarantor pledges when loan is fully repaid
     */
    @Transactional
    public void releaseAllPledges(Loan loan) {
        List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());

        for (Guarantor guarantor : guarantors) {
            if (guarantor.getStatus() == Guarantor.Status.ACTIVE) {
                guarantor.setStatus(Guarantor.Status.RELEASED);
                BigDecimal frozenAmount = guarantor.getPledgeAmount();
                guarantor.setPledgeAmount(BigDecimal.ZERO);
                guarantorRepository.save(guarantor);

                if (frozenAmount != null && frozenAmount.compareTo(BigDecimal.ZERO) > 0) {
                    unfreezeProportionalSavings(guarantor, frozenAmount);
                }

                notifyGuarantorOfFullRelease(guarantor, loan);
            }
        }
    }

    /**
     * Notify guarantor when pledge is fully released
     */
    private void notifyGuarantorOfFullRelease(Guarantor guarantor, Loan loan) {
        try {
            User guarantorUser = userRepository.findByMemberId(guarantor.getMember().getId())
                    .orElse(null);
            
            if (guarantorUser == null) {
                return;
            }

            String message = String.format(
                    "Excellent news %s! %s has fully repaid their loan. Your guarantee has been completely released and all your frozen savings (KES %,.2f) are now unfrozen and available for withdrawal.",
                    guarantor.getMember().getFirstName(),
                    loan.getMember().getFirstName() + " " + loan.getMember().getLastName(),
                    guarantor.getPledgeAmount() != null ? guarantor.getPledgeAmount() : BigDecimal.ZERO
            );

            notificationService.notifyUser(
                    guarantorUser.getId(),
                    message,
                    "GUARANTOR_PLEDGE_RELEASED",
                    loan.getId(),
                    loan.getMember().getId(),
                    "GUARANTOR"
            );
        } catch (Exception e) {
            System.err.println("Error notifying guarantor of pledge release: " + e.getMessage());
        }
    }

    /**
     * Unfreeze proportional savings for self-guarantors
     * Called when loan is repaid or fully released
     */
    @Transactional
    private void unfreezeProportionalSavings(Guarantor guarantor, BigDecimal unfreezeAmount) {
        try {
            Account savingsAccount = accountRepository
                    .findByMemberIdAndAccountType(guarantor.getMember().getId(), Account.AccountType.SAVINGS)
                    .orElse(null);
            
            if (savingsAccount == null) {
                return;
            }
            
            BigDecimal currentFrozen = savingsAccount.getFrozenSavings() != null ? 
                    savingsAccount.getFrozenSavings() : BigDecimal.ZERO;
            
            BigDecimal newFrozen = currentFrozen.subtract(unfreezeAmount);
            if (newFrozen.compareTo(BigDecimal.ZERO) < 0) {
                newFrozen = BigDecimal.ZERO;
            }
            
            savingsAccount.setFrozenSavings(newFrozen);
            savingsAccount.setUpdatedAt(LocalDateTime.now());
            accountRepository.save(savingsAccount);
        } catch (Exception e) {
            System.err.println("Error unfreezing savings: " + e.getMessage());
        }
    }
}
