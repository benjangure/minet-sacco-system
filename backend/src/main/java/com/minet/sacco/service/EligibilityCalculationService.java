package com.minet.sacco.service;

import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Calculates member eligibility based on savings and frozen amounts from self-guarantees.
 * 
 * Key principle: Frozen savings and outstanding balance are two sides of the same coin
 * for self-guaranteed loans. They should not both be subtracted.
 * 
 * Three distinct formulas:
 * 1. Normal loan: (True Savings × 3) - Outstanding
 * 2. Full self-guarantee: (True Savings - Frozen) × 3
 * 3. Partial self-guarantee: (True Savings - Frozen) × 3 - Unguaranteed Outstanding
 */
@Service
public class EligibilityCalculationService {

    private static final Logger log = LoggerFactory.getLogger(EligibilityCalculationService.class);
    private static final BigDecimal MULTIPLIER = BigDecimal.valueOf(3);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    /**
     * Calculate current eligibility for a member
     */
    public EligibilityResult calculateCurrentEligibility(Member member) {
        log.debug("Calculating eligibility for member: {}", member.getId());

        // Get member's true savings
        BigDecimal trueSavings = getTrueSavings(member);
        log.debug("True savings: {}", trueSavings);

        // Get total frozen from all self-guarantee loans
        BigDecimal totalFrozen = getTotalFrozenSavings(member);
        log.debug("Total frozen: {}", totalFrozen);

        // Calculate available savings
        BigDecimal availableSavings = trueSavings.subtract(totalFrozen);
        log.debug("Available savings: {}", availableSavings);

        // Calculate gross eligibility
        BigDecimal grossEligibility = availableSavings.multiply(MULTIPLIER);
        log.debug("Gross eligibility: {}", grossEligibility);

        // Get all active loans
        List<Loan> activeLoans = loanRepository.findByMemberIdAndStatus(member.getId(), Loan.Status.DISBURSED);
        List<Loan> activeLoans2 = loanRepository.findByMemberIdAndStatus(member.getId(), Loan.Status.REPAID);
        activeLoans.addAll(activeLoans2);

        // Calculate unguaranteed outstanding (external guarantees only)
        BigDecimal unguaranteedOutstanding = calculateUnguaranteedOutstanding(activeLoans);
        log.debug("Unguaranteed outstanding: {}", unguaranteedOutstanding);

        // Calculate remaining eligibility
        BigDecimal remainingEligibility = grossEligibility.subtract(unguaranteedOutstanding);
        log.debug("Remaining eligibility: {}", remainingEligibility);

        EligibilityResult result = new EligibilityResult(
            trueSavings,
            totalFrozen,
            availableSavings,
            grossEligibility,
            unguaranteedOutstanding,
            remainingEligibility
        );
        result.setMemberId(member.getId());

        // Calculate self-guaranteed loan details
        BigDecimal totalSelfGuaranteed = BigDecimal.ZERO;
        BigDecimal totalSelfGuaranteedInterest = BigDecimal.ZERO;
        
        for (Loan loan : activeLoans) {
            BigDecimal selfGuaranteeAmount = getSelfGuaranteeAmount(loan);
            if (selfGuaranteeAmount.compareTo(BigDecimal.ZERO) > 0) {
                totalSelfGuaranteed = totalSelfGuaranteed.add(selfGuaranteeAmount);
                // Calculate interest on self-guaranteed portion
                if (loan.getInterestRate() != null) {
                    BigDecimal interestOnSelfGuarantee = selfGuaranteeAmount
                        .multiply(loan.getInterestRate())
                        .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
                    totalSelfGuaranteedInterest = totalSelfGuaranteedInterest.add(interestOnSelfGuarantee);
                }
            }
        }
        
        result.setSelfGuaranteedAmount(totalSelfGuaranteed);
        result.setSelfGuaranteedInterest(totalSelfGuaranteedInterest);
        
        return result;
    }

    /**
     * Calculate hypothetical eligibility if a new loan is taken with specific guarantee structure
     */
    public EligibilityResult calculateHypotheticalEligibility(Member member, BigDecimal loanAmount, BigDecimal selfGuaranteeAmount) {
        log.debug("Calculating hypothetical eligibility - Loan: {}, Self-guarantee: {}", loanAmount, selfGuaranteeAmount);

        // Get member's true savings
        BigDecimal trueSavings = getTrueSavings(member);

        // Get current frozen
        BigDecimal currentFrozen = getTotalFrozenSavings(member);

        // Add the new self-guarantee to frozen
        BigDecimal newFrozen = currentFrozen.add(selfGuaranteeAmount);
        log.debug("New frozen after this loan: {}", newFrozen);

        // Calculate available savings after new freeze
        BigDecimal availableSavings = trueSavings.subtract(newFrozen);

        // Calculate gross eligibility
        BigDecimal grossEligibility = availableSavings.multiply(MULTIPLIER);

        // Get all active loans plus this new one
        List<Loan> activeLoans = loanRepository.findByMemberIdAndStatus(member.getId(), Loan.Status.DISBURSED);
        List<Loan> activeLoans2 = loanRepository.findByMemberIdAndStatus(member.getId(), Loan.Status.REPAID);
        activeLoans.addAll(activeLoans2);

        // Calculate unguaranteed outstanding (external guarantees only)
        BigDecimal unguaranteedOutstanding = calculateUnguaranteedOutstanding(activeLoans);

        // Add the external portion of this new loan
        BigDecimal externalGuaranteeAmount = loanAmount.subtract(selfGuaranteeAmount);
        unguaranteedOutstanding = unguaranteedOutstanding.add(externalGuaranteeAmount);
        log.debug("Unguaranteed outstanding after new loan: {}", unguaranteedOutstanding);

        // Calculate remaining eligibility
        BigDecimal remainingEligibility = grossEligibility.subtract(unguaranteedOutstanding);

        EligibilityResult result = new EligibilityResult(
            trueSavings,
            newFrozen,
            availableSavings,
            grossEligibility,
            unguaranteedOutstanding,
            remainingEligibility
        );
        result.setMemberId(member.getId());
        return result;
    }

    /**
     * Get member's true savings (not reduced by frozen amounts)
     */
    private BigDecimal getTrueSavings(Member member) {
        Optional<Account> savingsAccount = accountRepository
                .findByMemberIdAndAccountType(member.getId(), Account.AccountType.SAVINGS);
        
        if (savingsAccount.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        Account account = savingsAccount.get();
        BigDecimal balance = account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO;
        BigDecimal frozen = account.getFrozenSavings() != null ? account.getFrozenSavings() : BigDecimal.ZERO;
        
        // True savings = balance - frozen savings
        return balance.subtract(frozen);
    }

    /**
     * Get total frozen savings from all self-guarantee loans
     */
    private BigDecimal getTotalFrozenSavings(Member member) {
        Optional<Account> savingsAccount = accountRepository
                .findByMemberIdAndAccountType(member.getId(), Account.AccountType.SAVINGS);
        if (savingsAccount.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal frozen = savingsAccount.get().getFrozenSavings();
        return frozen != null ? frozen : BigDecimal.ZERO;
    }

    /**
     * Calculate outstanding balance NOT covered by self-guarantees
     * This is the portion covered by external guarantors
     */
    private BigDecimal calculateUnguaranteedOutstanding(List<Loan> loans) {
        BigDecimal total = BigDecimal.ZERO;

        for (Loan loan : loans) {
            if (loan.getOutstandingBalance() == null || loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Get self-guarantee amount for this loan
            BigDecimal selfGuaranteeAmount = getSelfGuaranteeAmount(loan);

            // Calculate the portion NOT covered by self-guarantee
            BigDecimal outstandingNotCovered = loan.getOutstandingBalance().subtract(selfGuaranteeAmount);

            // Only add if positive (there is external guarantee portion)
            if (outstandingNotCovered.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(outstandingNotCovered);
            }
        }

        return total;
    }

    /**
     * Get the self-guarantee amount for a specific loan
     */
    private BigDecimal getSelfGuaranteeAmount(Loan loan) {
        List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());

        BigDecimal selfGuaranteeTotal = BigDecimal.ZERO;
        for (Guarantor guarantor : guarantors) {
            if (guarantor.isSelfGuarantee()) {
                BigDecimal amount = guarantor.getGuaranteeAmount();
                if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                    selfGuaranteeTotal = selfGuaranteeTotal.add(amount);
                }
            }
        }

        return selfGuaranteeTotal;
    }

    /**
     * Get repayment impact on eligibility for a specific loan
     */
    public RepaymentImpactResult getRepaymentImpact(Loan loan) {
        log.debug("Calculating repayment impact for loan: {}", loan.getId());

        // Get self-guarantee amount
        BigDecimal originalFrozen = getSelfGuaranteeAmount(loan);

        // Calculate current frozen (proportional to outstanding)
        BigDecimal currentFrozen = BigDecimal.ZERO;
        if (loan.getTotalRepayable() != null && loan.getTotalRepayable().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal repaymentRatio = loan.getOutstandingBalance().divide(
                    loan.getTotalRepayable(), 4, java.math.RoundingMode.HALF_UP);
            currentFrozen = originalFrozen.multiply(repaymentRatio).setScale(2, java.math.RoundingMode.HALF_UP);
        }

        // Calculate released amount
        BigDecimal released = originalFrozen.subtract(currentFrozen);

        // Calculate eligibility recovered
        BigDecimal eligibilityRecovered = released.multiply(MULTIPLIER);

        // Get member's true savings
        BigDecimal trueSavings = getTrueSavings(loan.getMember());

        // Calculate projected eligibility at full repayment
        BigDecimal projectedEligibility = trueSavings.multiply(MULTIPLIER);

        return new RepaymentImpactResult(
            originalFrozen,
            currentFrozen,
            released,
            eligibilityRecovered,
            projectedEligibility
        );
    }

    /**
     * DTO for eligibility calculation result
     */
    public static class EligibilityResult {
        private Long memberId;
        private BigDecimal trueSavings;
        private BigDecimal totalFrozen;
        private BigDecimal availableSavings;
        private BigDecimal grossEligibility;
        private BigDecimal unguaranteedOutstanding;
        private BigDecimal remainingEligibility;
        private BigDecimal selfGuaranteedAmount;
        private BigDecimal selfGuaranteedInterest;

        public EligibilityResult(BigDecimal trueSavings, BigDecimal totalFrozen, BigDecimal availableSavings,
                               BigDecimal grossEligibility, BigDecimal unguaranteedOutstanding,
                               BigDecimal remainingEligibility) {
            this.trueSavings = trueSavings;
            this.totalFrozen = totalFrozen;
            this.availableSavings = availableSavings;
            this.grossEligibility = grossEligibility;
            this.unguaranteedOutstanding = unguaranteedOutstanding;
            this.remainingEligibility = remainingEligibility;
            this.selfGuaranteedAmount = BigDecimal.ZERO;
            this.selfGuaranteedInterest = BigDecimal.ZERO;
        }

        // Getters
        public Long getMemberId() { return memberId; }
        public BigDecimal getTrueSavings() { return trueSavings; }
        public BigDecimal getTotalFrozen() { return totalFrozen; }
        public BigDecimal getAvailableSavings() { return availableSavings; }
        public BigDecimal getGrossEligibility() { return grossEligibility; }
        public BigDecimal getUnguaranteedOutstanding() { return unguaranteedOutstanding; }
        public BigDecimal getRemainingEligibility() { return remainingEligibility; }
        public BigDecimal getSelfGuaranteedAmount() { return selfGuaranteedAmount; }
        public BigDecimal getSelfGuaranteedInterest() { return selfGuaranteedInterest; }

        // Setters
        public void setMemberId(Long memberId) { this.memberId = memberId; }
        public void setSelfGuaranteedAmount(BigDecimal amount) { this.selfGuaranteedAmount = amount; }
        public void setSelfGuaranteedInterest(BigDecimal interest) { this.selfGuaranteedInterest = interest; }
    }

    /**
     * DTO for repayment impact result
     */
    public static class RepaymentImpactResult {
        private BigDecimal originalFrozen;
        private BigDecimal currentFrozen;
        private BigDecimal released;
        private BigDecimal eligibilityRecovered;
        private BigDecimal projectedEligibilityAtFullRepayment;

        public RepaymentImpactResult(BigDecimal originalFrozen, BigDecimal currentFrozen, BigDecimal released,
                                   BigDecimal eligibilityRecovered, BigDecimal projectedEligibilityAtFullRepayment) {
            this.originalFrozen = originalFrozen;
            this.currentFrozen = currentFrozen;
            this.released = released;
            this.eligibilityRecovered = eligibilityRecovered;
            this.projectedEligibilityAtFullRepayment = projectedEligibilityAtFullRepayment;
        }

        // Getters
        public BigDecimal getOriginalFrozen() { return originalFrozen; }
        public BigDecimal getCurrentFrozen() { return currentFrozen; }
        public BigDecimal getReleased() { return released; }
        public BigDecimal getEligibilityRecovered() { return eligibilityRecovered; }
        public BigDecimal getProjectedEligibilityAtFullRepayment() { return projectedEligibilityAtFullRepayment; }
    }
}
