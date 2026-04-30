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
     * 
     * Applies Rule 3, 4, and 5:
     * - Rule 3: Frozen savings reduce eligibility BEFORE multiplier
     * - Rule 4: Full self-guarantee doesn't subtract outstanding again
     * - Rule 5: Partial self-guarantee subtracts only external outstanding
     */
    public EligibilityResult calculateCurrentEligibility(Member member) {
        log.debug("Calculating eligibility for member: {}", member.getId());

        // Get member's account balance (actual savings, never changes due to loans)
        Optional<Account> account = accountRepository
                .findByMemberIdAndAccountType(member.getId(), Account.AccountType.SAVINGS);
        BigDecimal accountBalance = account.isPresent() ? account.get().getBalance() : BigDecimal.ZERO;
        log.debug("Account Balance: {}", accountBalance);

        // Get member's true savings (already deducts ONLY self-guarantee frozen)
        BigDecimal trueSavings = getTrueSavings(member);
        log.debug("True savings: {}", trueSavings);

        // Get ONLY self-guarantee frozen (for display purposes)
        BigDecimal selfGuaranteeFrozen = getSelfGuaranteeFrozenAmount(member);
        log.debug("Self-Guarantee Frozen: {}", selfGuaranteeFrozen);

        // Get TOTAL frozen (self-guarantee + guarantor pledges) for display
        BigDecimal totalFrozen = getTotalFrozenSavings(member);
        log.debug("Total Frozen (Self + Guarantor Pledges): {}", totalFrozen);

        // Available savings = true savings (already has self-guarantee frozen deducted)
        BigDecimal availableSavings = trueSavings;
        log.debug("Available savings: {}", availableSavings);

        // Calculate gross eligibility (Rule 3: frozen deducted before multiplier)
        BigDecimal grossEligibility = availableSavings.multiply(MULTIPLIER);
        log.debug("Gross eligibility: {}", grossEligibility);

        // Get all active loans
        List<Loan> activeLoans = loanRepository.findByMemberIdAndStatus(member.getId(), Loan.Status.DISBURSED);
        List<Loan> activeLoans2 = loanRepository.findByMemberIdAndStatus(member.getId(), Loan.Status.REPAID);
        activeLoans.addAll(activeLoans2);

        // CRITICAL: Only deduct EXTERNAL guarantee portion of outstanding (Rule 5)
        // Self-guaranteed portion is already frozen, so don't deduct it again (Rule 4)
        BigDecimal externalGuaranteeOutstanding = calculateUnguaranteedOutstanding(activeLoans);
        log.debug("External guarantee outstanding: {}", externalGuaranteeOutstanding);

        // Calculate remaining eligibility
        BigDecimal remainingEligibility = grossEligibility.subtract(externalGuaranteeOutstanding);
        log.debug("Remaining eligibility: {}", remainingEligibility);

        EligibilityResult result = new EligibilityResult(
            trueSavings,
            totalFrozen,  // FIXED: Now passing totalFrozen (self-guarantee + guarantor pledges)
            availableSavings,
            grossEligibility,
            externalGuaranteeOutstanding,
            remainingEligibility
        );
        result.setMemberId(member.getId());
        result.setAccountBalance(accountBalance);  // Set the actual account balance

        // Calculate self-guaranteed loan details
        // Use ORIGINAL self-guarantee amount (guaranteeAmount), not frozen amount (pledgeAmount)
        // This shows the member what they originally committed to guarantee
        BigDecimal totalSelfGuaranteed = BigDecimal.ZERO;
        BigDecimal totalSelfGuaranteedInterest = BigDecimal.ZERO;
        
        for (Loan loan : activeLoans) {
            BigDecimal selfGuaranteeAmount = getOriginalSelfGuaranteeAmount(loan);
            if (selfGuaranteeAmount.compareTo(BigDecimal.ZERO) > 0) {
                totalSelfGuaranteed = totalSelfGuaranteed.add(selfGuaranteeAmount);
                // Use actual accrued interest from loan, not calculated interest
                // This accounts for the actual term and time elapsed
                if (loan.getTotalInterest() != null) {
                    totalSelfGuaranteedInterest = totalSelfGuaranteedInterest.add(loan.getTotalInterest());
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
        log.debug("=== CALCULATING HYPOTHETICAL ELIGIBILITY ===");
        log.debug("Member ID: {}, Loan Amount: {}, Self-Guarantee Amount: {}", member.getId(), loanAmount, selfGuaranteeAmount);

        // Get member's account balance (actual savings, never changes due to loans)
        Optional<Account> account = accountRepository
                .findByMemberIdAndAccountType(member.getId(), Account.AccountType.SAVINGS);
        BigDecimal accountBalance = account.isPresent() ? account.get().getBalance() : BigDecimal.ZERO;
        log.debug("Account Balance: {}", accountBalance);

        // Get member's true savings
        BigDecimal trueSavings = getTrueSavings(member);
        log.debug("True Savings (balance - frozen): {}", trueSavings);

        // Get current frozen
        BigDecimal currentFrozen = getTotalFrozenSavings(member);
        log.debug("Current Frozen Savings: {}", currentFrozen);

        // Add the new self-guarantee to frozen (Kenyan SACCO: use self-guarantee amount only)
        BigDecimal newFrozen = currentFrozen.add(selfGuaranteeAmount);
        log.debug("New Frozen after this loan: {} + {} = {}", currentFrozen, selfGuaranteeAmount, newFrozen);

        // Calculate available savings after new freeze
        // trueSavings already has currentFrozen deducted, so we subtract the NEW self-guarantee amount
        BigDecimal availableSavings = trueSavings.subtract(selfGuaranteeAmount);
        if (availableSavings.compareTo(BigDecimal.ZERO) < 0) {
            availableSavings = BigDecimal.ZERO;
        }
        log.debug("Available Savings: {} - {} = {}", trueSavings, selfGuaranteeAmount, availableSavings);
        log.debug("FIXED: Subtracting new selfGuaranteeAmount ({}) from trueSavings", selfGuaranteeAmount);

        // Calculate gross eligibility
        BigDecimal grossEligibility = availableSavings.multiply(MULTIPLIER);
        log.debug("Gross Eligibility: {} × {} = {}", availableSavings, MULTIPLIER, grossEligibility);

        // Get all active loans plus this new one
        List<Loan> activeLoans = loanRepository.findByMemberIdAndStatus(member.getId(), Loan.Status.DISBURSED);
        List<Loan> activeLoans2 = loanRepository.findByMemberIdAndStatus(member.getId(), Loan.Status.REPAID);
        activeLoans.addAll(activeLoans2);

        // Calculate unguaranteed outstanding (external guarantees only)
        BigDecimal unguaranteedOutstanding = calculateUnguaranteedOutstanding(activeLoans);

        // Add the external portion of this new loan
        BigDecimal externalGuaranteeAmount = loanAmount.subtract(selfGuaranteeAmount);
        log.debug("External Guarantee Amount: {} - {} = {}", loanAmount, selfGuaranteeAmount, externalGuaranteeAmount);
        
        unguaranteedOutstanding = unguaranteedOutstanding.add(externalGuaranteeAmount);
        log.debug("Unguaranteed Outstanding after new loan: {} + {} = {}", unguaranteedOutstanding.subtract(externalGuaranteeAmount), externalGuaranteeAmount, unguaranteedOutstanding);

        // Calculate remaining eligibility
        BigDecimal remainingEligibility = grossEligibility.subtract(unguaranteedOutstanding);
        log.debug("Remaining Eligibility: {} - {} = {}", grossEligibility, unguaranteedOutstanding, remainingEligibility);
        log.debug("=== END HYPOTHETICAL ELIGIBILITY CALCULATION ===");

        EligibilityResult result = new EligibilityResult(
            trueSavings,
            newFrozen,
            availableSavings,
            grossEligibility,
            unguaranteedOutstanding,
            remainingEligibility
        );
        result.setMemberId(member.getId());
        result.setAccountBalance(accountBalance);  // Set the actual account balance
        result.setSelfGuaranteedAmount(selfGuaranteeAmount);
        return result;
    }

    /**
     * Get member's true savings (reduced ONLY by self-guarantee frozen amounts)
     * 
     * CRITICAL RULE 3: Frozen savings reduce eligibility BEFORE multiplier
     * 
     * For eligibility calculation:
     * - Subtract frozen amounts from member's OWN self-guaranteed loans (proportional to outstanding principal)
     * - ALSO subtract frozen pledges from being a guarantor on OTHER people's loans
     * - Both types of frozen amounts represent committed savings that cannot be used for new loans
     * 
     * Three scenarios:
     * 1. Full self-guarantee: Frozen = full loan amount (initially), reduces proportionally with repayment
     * 2. Full external guarantors: Frozen = 0 (no self-guarantee)
     * 3. Partial self-guarantee: Frozen = only the self-guarantee portion, reduces proportionally
     */
    private BigDecimal getTrueSavings(Member member) {
        log.debug("=== CALCULATING TRUE SAVINGS FOR MEMBER {} ===", member.getId());
        Optional<Account> account = accountRepository
                .findByMemberIdAndAccountType(member.getId(), Account.AccountType.SAVINGS);
        
        BigDecimal balance = account.isPresent() ? account.get().getBalance() : BigDecimal.ZERO;
        log.debug("Account Balance: {}", balance);
        
        // Calculate frozen savings from self-guaranteed loans where THIS MEMBER is the BORROWER
        List<Loan> memberLoans = loanRepository.findByMemberId(member.getId());
        BigDecimal frozenSelfGuarantee = BigDecimal.ZERO;
        
        for (Loan loan : memberLoans) {
            // Only count DISBURSED and REPAID loans (active guarantees)
            if (loan.getStatus() != Loan.Status.DISBURSED && loan.getStatus() != Loan.Status.REPAID) {
                continue;
            }
            
            // Get self-guarantee guarantor for THIS LOAN where member is the borrower
            List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());
            for (Guarantor guarantor : guarantors) {
                // Only count if: member is self-guarantor AND member is the loan borrower
                if (guarantor.isSelfGuarantee() && guarantor.getMember().getId().equals(member.getId()) && 
                    loan.getMember().getId().equals(member.getId())) {
                    
                    // Get original pledge amount (set at disbursement)
                    BigDecimal originalPledge = guarantor.getPledgeAmount();
                    if (originalPledge == null || originalPledge.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    
                    BigDecimal outstandingBalance = loan.getOutstandingBalance();
                    if (outstandingBalance == null || outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    
                    BigDecimal originalPrincipal = loan.getOriginalPrincipal() != null ?
                            loan.getOriginalPrincipal() : loan.getAmount();
                    BigDecimal totalRepayable = loan.getTotalRepayable();

                    // outstandingBalance starts at totalRepayable and decreases with each repayment.
                    // Amount repaid = totalRepayable - outstandingBalance
                    // Outstanding principal = originalPrincipal - amountRepaid
                    BigDecimal outstandingPrincipal;
                    if (totalRepayable != null && totalRepayable.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal amountRepaid = totalRepayable.subtract(outstandingBalance);
                        outstandingPrincipal = originalPrincipal.subtract(amountRepaid);
                    } else {
                        outstandingPrincipal = originalPrincipal;
                    }
                    if (outstandingPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                        outstandingPrincipal = BigDecimal.ZERO;
                    }
                    
                    // Frozen = Original Pledge × (Outstanding Principal / Original Principal)
                    BigDecimal proportionalFrozen = originalPrincipal.compareTo(BigDecimal.ZERO) > 0 ?
                            originalPledge.multiply(outstandingPrincipal)
                                    .divide(originalPrincipal, 2, java.math.RoundingMode.HALF_UP) :
                            BigDecimal.ZERO;
                    
                    frozenSelfGuarantee = frozenSelfGuarantee.add(proportionalFrozen);
                    log.debug("Loan {}: OriginalPledge={}, TotalRepayable={}, OutstandingBalance={}, OutstandingPrincipal={}, Frozen={}",
                            loan.getId(), originalPledge, totalRepayable, outstandingBalance, outstandingPrincipal, proportionalFrozen);
                }
            }
        }
        log.debug("Total Frozen from Self-Guarantees: {}", frozenSelfGuarantee);
        
        // CRITICAL: ALSO include frozen pledges from being a guarantor on OTHER PEOPLE'S loans
        // These pledges represent committed savings that reduce the member's available capital
        BigDecimal frozenPledges = guarantorRepository.sumActivePledgesByMemberId(member.getId());
        if (frozenPledges == null) {
            frozenPledges = BigDecimal.ZERO;
        }
        log.debug("Frozen from Guarantor Pledges (other loans): {}", frozenPledges);
        
        // Total frozen = self-guarantee frozen + guarantor pledges frozen (Rule 2)
        BigDecimal totalFrozen = frozenSelfGuarantee.add(frozenPledges);
        log.debug("Total Frozen (Self + Guarantor Pledges): {}", totalFrozen);
        
        // True savings = balance - total frozen (Rule 3)
        BigDecimal trueSavings = balance.subtract(totalFrozen);
        log.debug("True Savings: {} - {} = {}", balance, totalFrozen, trueSavings);
        log.debug("=== END TRUE SAVINGS CALCULATION ===");
        
        return trueSavings.compareTo(BigDecimal.ZERO) > 0 ? trueSavings : BigDecimal.ZERO;
    }

    /**
     * Get ONLY self-guarantee frozen amount (for display in eligibility breakdown)
     * Does NOT include frozen pledges from being a guarantor on other loans
     * 
     * CRITICAL: Uses proportional calculation based on outstanding PRINCIPAL
     * As loan is repaid, frozen amount reduces proportionally
     * Formula: Frozen = Original Pledge × (Outstanding Principal / Original Principal)
     */
    private BigDecimal getSelfGuaranteeFrozenAmount(Member member) {
        List<Loan> memberLoans = loanRepository.findByMemberId(member.getId());
        BigDecimal frozenSelfGuaranteeOnly = BigDecimal.ZERO;
        
        for (Loan loan : memberLoans) {
            if (loan.getStatus() != Loan.Status.DISBURSED && loan.getStatus() != Loan.Status.REPAID) {
                continue;
            }
            
            List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());
            for (Guarantor guarantor : guarantors) {
                if (guarantor.isSelfGuarantee() && guarantor.getMember().getId().equals(member.getId())) {
                    BigDecimal originalPledge = guarantor.getPledgeAmount();
                    if (originalPledge == null || originalPledge.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    
                    BigDecimal outstandingBalance = loan.getOutstandingBalance();
                    if (outstandingBalance == null || outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    
                    BigDecimal originalPrincipal = loan.getOriginalPrincipal() != null ?
                            loan.getOriginalPrincipal() : loan.getAmount();
                    BigDecimal totalRepayable = loan.getTotalRepayable();

                    // outstandingBalance starts at totalRepayable and decreases with each repayment.
                    // Amount repaid = totalRepayable - outstandingBalance
                    // Outstanding principal = originalPrincipal - amountRepaid
                    BigDecimal outstandingPrincipal;
                    if (totalRepayable != null && totalRepayable.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal amountRepaid = totalRepayable.subtract(outstandingBalance);
                        outstandingPrincipal = originalPrincipal.subtract(amountRepaid);
                    } else {
                        outstandingPrincipal = originalPrincipal;
                    }
                    if (outstandingPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                        outstandingPrincipal = BigDecimal.ZERO;
                    }
                    
                    // Frozen = Original Pledge × (Outstanding Principal / Original Principal)
                    // Interest is NOT frozen — only the principal portion is frozen
                    BigDecimal proportionalFrozen = originalPrincipal.compareTo(BigDecimal.ZERO) > 0 ?
                            originalPledge.multiply(outstandingPrincipal)
                                    .divide(originalPrincipal, 2, java.math.RoundingMode.HALF_UP) :
                            BigDecimal.ZERO;
                    
                    frozenSelfGuaranteeOnly = frozenSelfGuaranteeOnly.add(proportionalFrozen);
                    
                    log.debug("Loan {}: OriginalPledge={}, TotalRepayable={}, OutstandingBalance={}, OutstandingPrincipal={}, Frozen={}",
                            loan.getId(), originalPledge, totalRepayable, outstandingBalance, outstandingPrincipal, proportionalFrozen);
                }
            }
        }
        
        return frozenSelfGuaranteeOnly;
    }

    /**
     * Get total frozen amount (both self-guarantee frozen savings and guarantor pledges)
     * 
     * CRITICAL: Calculates frozen amounts dynamically from guarantor table instead of using cached frozenSavings
     * Uses proportional calculation based on outstanding PRINCIPAL (not total outstanding)
     * This ensures accuracy even if the account.frozenSavings field becomes out of sync
     */
    private BigDecimal getTotalFrozenSavings(Member member) {
        // Calculate frozen savings dynamically from self-guaranteed loans
        List<Loan> selfGuaranteedLoans = loanRepository.findByMemberId(member.getId());
        BigDecimal frozenSelfGuarantee = BigDecimal.ZERO;
        
        for (Loan loan : selfGuaranteedLoans) {
            // Only count DISBURSED and REPAID loans (active guarantees)
            if (loan.getStatus() != Loan.Status.DISBURSED && loan.getStatus() != Loan.Status.REPAID) {
                continue;
            }
            
            // Get self-guarantee guarantor for this loan
            List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());
            for (Guarantor guarantor : guarantors) {
                if (guarantor.isSelfGuarantee() && guarantor.getMember().getId().equals(member.getId())) {
                    BigDecimal originalPledge = guarantor.getPledgeAmount();
                    if (originalPledge == null || originalPledge.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    
                    BigDecimal outstandingBalance = loan.getOutstandingBalance();
                    if (outstandingBalance == null || outstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }
                    
                    BigDecimal originalPrincipal = loan.getOriginalPrincipal() != null ?
                            loan.getOriginalPrincipal() : loan.getAmount();
                    BigDecimal totalRepayable = loan.getTotalRepayable();

                    // outstandingBalance starts at totalRepayable and decreases with each repayment.
                    // Amount repaid = totalRepayable - outstandingBalance
                    // Outstanding principal = originalPrincipal - amountRepaid
                    BigDecimal outstandingPrincipal;
                    if (totalRepayable != null && totalRepayable.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal amountRepaid = totalRepayable.subtract(outstandingBalance);
                        outstandingPrincipal = originalPrincipal.subtract(amountRepaid);
                    } else {
                        outstandingPrincipal = originalPrincipal;
                    }
                    if (outstandingPrincipal.compareTo(BigDecimal.ZERO) < 0) {
                        outstandingPrincipal = BigDecimal.ZERO;
                    }
                    
                    // Frozen = Original Pledge × (Outstanding Principal / Original Principal)
                    BigDecimal proportionalFrozen = originalPrincipal.compareTo(BigDecimal.ZERO) > 0 ?
                            originalPledge.multiply(outstandingPrincipal)
                                    .divide(originalPrincipal, 2, java.math.RoundingMode.HALF_UP) :
                            BigDecimal.ZERO;
                    
                    frozenSelfGuarantee = frozenSelfGuarantee.add(proportionalFrozen);
                }
            }
        }
        
        // Get frozen pledges from being a guarantor on other loans
        BigDecimal frozenPledges = guarantorRepository.sumActivePledgesByMemberId(member.getId());
        if (frozenPledges == null) {
            frozenPledges = BigDecimal.ZERO;
        }
        
        // Total frozen = self-guarantee frozen + guarantor pledges frozen
        return frozenSelfGuarantee.add(frozenPledges);
    }

    /**
     * Calculate outstanding balance NOT covered by self-guarantees
     * This is the portion covered by external guarantors
     * 
     * CRITICAL: Uses proportional self-guarantee coverage based on OUTSTANDING balance
     * As the loan is repaid, the unguaranteed outstanding reduces proportionally
     * 
     * Formula:
     * - selfGuaranteeRatio = originalSelfGuaranteeAmount / originalPrincipal
     * - selfGuaranteeCoveredNow = currentOutstandingBalance × selfGuaranteeRatio
     * - unguaranteedOutstanding = currentOutstandingBalance - selfGuaranteeCoveredNow
     */
    private BigDecimal calculateUnguaranteedOutstanding(List<Loan> loans) {
        BigDecimal total = BigDecimal.ZERO;

        for (Loan loan : loans) {
            if (loan.getOutstandingBalance() == null || loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Get ORIGINAL self-guarantee amount (not frozen pledge)
            BigDecimal originalSelfGuaranteeAmount = getOriginalSelfGuaranteeAmount(loan);

            // Get the original principal (not including interest)
            BigDecimal originalPrincipal = loan.getOriginalPrincipal() != null ? 
                    loan.getOriginalPrincipal() : loan.getAmount();

            // Get current outstanding balance
            BigDecimal outstandingPrincipal = loan.getOutstandingBalance();

            // Calculate the self-guarantee ratio (what proportion of original loan was self-guaranteed)
            BigDecimal selfGuaranteeRatio = originalPrincipal.compareTo(BigDecimal.ZERO) > 0 ?
                    originalSelfGuaranteeAmount.divide(originalPrincipal, 4, java.math.RoundingMode.HALF_UP) :
                    BigDecimal.ZERO;

            // Calculate how much of the CURRENT outstanding is covered by self-guarantee (proportionally)
            BigDecimal selfGuaranteeCoveredNow = outstandingPrincipal
                    .multiply(selfGuaranteeRatio)
                    .setScale(2, java.math.RoundingMode.HALF_UP);

            // Calculate the portion NOT covered by self-guarantee (covered by external guarantors)
            BigDecimal principalNotCovered = outstandingPrincipal.subtract(selfGuaranteeCoveredNow);

            // Only add if positive (there is external guarantee portion)
            if (principalNotCovered.compareTo(BigDecimal.ZERO) > 0) {
                total = total.add(principalNotCovered);
                log.debug("Loan {}: Outstanding={}, SelfGuaranteeRatio={}, SelfGuaranteeCovered={}, Unguaranteed={}",
                        loan.getId(), outstandingPrincipal, selfGuaranteeRatio, selfGuaranteeCoveredNow, principalNotCovered);
            }
        }

        return total;
    }

    /**
     * Get the ORIGINAL self-guarantee amount for a specific loan
     * Returns the guaranteeAmount (original amount pledged), not the frozen pledgeAmount
     * This amount never changes throughout the loan lifecycle
     */
    private BigDecimal getOriginalSelfGuaranteeAmount(Loan loan) {
        List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());

        BigDecimal selfGuaranteeTotal = BigDecimal.ZERO;
        for (Guarantor guarantor : guarantors) {
            if (guarantor.isSelfGuarantee()) {
                // Use guaranteeAmount (original), not pledgeAmount (frozen)
                BigDecimal amount = guarantor.getGuaranteeAmount();
                if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                    selfGuaranteeTotal = selfGuaranteeTotal.add(amount);
                }
            }
        }

        return selfGuaranteeTotal;
    }

    /**
     * Get the self-guarantee amount for a specific loan
     * Returns the FROZEN amount (pledgeAmount), not the original guarantee amount
     * Used for repayment impact calculations
     */
    private BigDecimal getSelfGuaranteeAmount(Loan loan) {
        List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());

        BigDecimal selfGuaranteeTotal = BigDecimal.ZERO;
        for (Guarantor guarantor : guarantors) {
            if (guarantor.isSelfGuarantee()) {
                // Use pledgeAmount (frozen amount), not guaranteeAmount
                BigDecimal amount = guarantor.getPledgeAmount();
                if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
                    selfGuaranteeTotal = selfGuaranteeTotal.add(amount);
                }
            }
        }

        return selfGuaranteeTotal;
    }

    /**
     * Get repayment impact on eligibility for a specific loan
     * 
     * CRITICAL FIX: Uses originalPrincipal (not totalRepayable) as denominator.
     * totalRepayable includes interest, which makes the denominator larger than principal.
     * This causes frozen amounts to release too slowly.
     * 
     * Correct formula: repaymentRatio = outstandingBalance / originalPrincipal
     * This ensures frozen amounts release proportionally with principal repayment.
     */
    public RepaymentImpactResult getRepaymentImpact(Loan loan) {
        log.debug("Calculating repayment impact for loan: {}", loan.getId());

        // Get self-guarantee amount
        BigDecimal originalFrozen = getSelfGuaranteeAmount(loan);

        // Calculate current frozen (proportional to outstanding)
        BigDecimal currentFrozen = BigDecimal.ZERO;
        
        // Get original principal (not including interest)
        BigDecimal originalPrincipal = loan.getOriginalPrincipal() != null ? 
                loan.getOriginalPrincipal() : loan.getAmount();
        
        // FIXED: Use originalPrincipal, not totalRepayable
        if (originalPrincipal != null && originalPrincipal.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal repaymentRatio = loan.getOutstandingBalance().divide(
                    originalPrincipal, 4, java.math.RoundingMode.HALF_UP);
            currentFrozen = originalFrozen.multiply(repaymentRatio).setScale(2, java.math.RoundingMode.HALF_UP);
            log.debug("Repayment ratio: {} / {} = {}", loan.getOutstandingBalance(), originalPrincipal, repaymentRatio);
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
        private BigDecimal accountBalance;  // Actual account balance (never changes due to loans)
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
            // Account balance will be set separately
            this.accountBalance = BigDecimal.ZERO;
        }

        // Getters
        public Long getMemberId() { return memberId; }
        public BigDecimal getAccountBalance() { return accountBalance; }
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
        public void setAccountBalance(BigDecimal balance) { this.accountBalance = balance; }
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
