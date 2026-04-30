package com.minet.sacco.service;

import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Consolidated loan eligibility validation service used by both individual and bulk workflows
 */
@Service
public class LoanEligibilityValidator {

    private static final Logger log = LoggerFactory.getLogger(LoanEligibilityValidator.class);

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private LoanEligibilityRulesService loanEligibilityRulesService;

    @Autowired
    private SystemSettingsService systemSettingsService;

    @Autowired
    private MemberSuspensionService memberSuspensionService;

    public static class EligibilityResult {
        private boolean eligible;
        private List<String> errors;
        private List<String> warnings;
        private BigDecimal savingsBalance;
        private BigDecimal sharesBalance;
        private BigDecimal totalBalance;
        private long activeLoans;
        private long defaultedLoans;
        private BigDecimal totalOutstanding;
        private BigDecimal maxEligibleAmount;
        private BigDecimal netEligibleAmount;
        private BigDecimal baseSavings;
        private BigDecimal totalDisbursed;
        private BigDecimal trueSavings;
        private BigDecimal grossEligibility;

        public EligibilityResult() {
            this.errors = new ArrayList<>();
            this.warnings = new ArrayList<>();
            this.eligible = true;
        }

        // Getters and Setters
        public boolean isEligible() { return eligible; }
        public void setEligible(boolean eligible) { this.eligible = eligible; }

        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }

        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }

        public BigDecimal getSavingsBalance() { return savingsBalance; }
        public void setSavingsBalance(BigDecimal savingsBalance) { this.savingsBalance = savingsBalance; }

        public BigDecimal getSharesBalance() { return sharesBalance; }
        public void setSharesBalance(BigDecimal sharesBalance) { this.sharesBalance = sharesBalance; }

        public BigDecimal getTotalBalance() { return totalBalance; }
        public void setTotalBalance(BigDecimal totalBalance) { this.totalBalance = totalBalance; }

        public long getActiveLoans() { return activeLoans; }
        public void setActiveLoans(long activeLoans) { this.activeLoans = activeLoans; }

        public long getDefaultedLoans() { return defaultedLoans; }
        public void setDefaultedLoans(long defaultedLoans) { this.defaultedLoans = defaultedLoans; }

        public BigDecimal getTotalOutstanding() { return totalOutstanding; }
        public void setTotalOutstanding(BigDecimal totalOutstanding) { this.totalOutstanding = totalOutstanding; }

        public BigDecimal getMaxEligibleAmount() { return maxEligibleAmount; }
        public void setMaxEligibleAmount(BigDecimal maxEligibleAmount) { this.maxEligibleAmount = maxEligibleAmount; }

        public BigDecimal getNetEligibleAmount() { return netEligibleAmount; }
        public void setNetEligibleAmount(BigDecimal netEligibleAmount) { this.netEligibleAmount = netEligibleAmount; }

        public BigDecimal getBaseSavings() { return baseSavings; }
        public void setBaseSavings(BigDecimal baseSavings) { this.baseSavings = baseSavings; }

        public BigDecimal getTotalDisbursed() { return totalDisbursed; }
        public void setTotalDisbursed(BigDecimal totalDisbursed) { this.totalDisbursed = totalDisbursed; }

        public BigDecimal getTrueSavings() { return trueSavings; }
        public void setTrueSavings(BigDecimal trueSavings) { this.trueSavings = trueSavings; }

        public BigDecimal getGrossEligibility() { return grossEligibility; }
        public void setGrossEligibility(BigDecimal grossEligibility) { this.grossEligibility = grossEligibility; }
    }

    /**
     * Validate member eligibility for loan application
     * IMPORTANT: Only SAVINGS count for eligibility (not shares)
     * Uses formula: True Savings = Current Savings - Total Disbursed Loans
     * Frozen pledges from guarantorships reduce available savings
     */
    public EligibilityResult validateMemberEligibility(Member member, BigDecimal loanAmount) {
        log.debug("=== VALIDATING MEMBER ELIGIBILITY ===");
        log.debug("Member ID: {}, Loan Amount: {}", member.getId(), loanAmount);
        
        EligibilityResult result = new EligibilityResult();
        LoanEligibilityRules rules = loanEligibilityRulesService.getRules();
        
        // Validate rules are not null
        if (rules.getMinMemberSavings() == null) {
            rules.setMinMemberSavings(new BigDecimal("10000"));
        }
        if (rules.getMinSavingsToLoanRatio() == null) {
            rules.setMinSavingsToLoanRatio(new BigDecimal("0.20"));
        }
        if (rules.getMaxOutstandingToSavingsRatio() == null) {
            rules.setMaxOutstandingToSavingsRatio(new BigDecimal("0.50"));
        }
        if (rules.getMaxActiveLoans() == null) {
            rules.setMaxActiveLoans(3);
        }
        if (rules.getAllowDefaulters() == null) {
            rules.setAllowDefaulters(false);
        }

        // Get member's accounts from database
        Optional<Account> savingsAccount = accountRepository.findByMemberIdAndAccountType(
                member.getId(), Account.AccountType.SAVINGS);
        Optional<Account> sharesAccount = accountRepository.findByMemberIdAndAccountType(
                member.getId(), Account.AccountType.SHARES);

        BigDecimal savingsBalance = savingsAccount.map(Account::getBalance).orElse(BigDecimal.ZERO);
        BigDecimal sharesBalance = sharesAccount.map(Account::getBalance).orElse(BigDecimal.ZERO);
        log.debug("Savings Balance: {}, Shares Balance: {}", savingsBalance, sharesBalance);
        
        // For member loan eligibility, only SAVINGS count (not shares)
        // Shares are capital contributions and don't count toward loan capacity
        BigDecimal totalBalance = savingsBalance;
        
        // Get frozen savings from self-guarantee loans
        BigDecimal frozenSavings = savingsAccount.map(Account::getFrozenSavings).orElse(BigDecimal.ZERO);
        if (frozenSavings == null) frozenSavings = BigDecimal.ZERO;
        log.debug("Frozen Savings from self-guarantees: {}", frozenSavings);
        
        // Subtract frozen pledges from available balance
        // If member is a guarantor for other loans, those pledges freeze their savings
        BigDecimal frozenPledges = guarantorRepository.sumActivePledgesByMemberId(member.getId());
        if (frozenPledges == null) frozenPledges = BigDecimal.ZERO;
        log.debug("Frozen Pledges (guarantor for others): {}", frozenPledges);
        
        BigDecimal availableBalance = totalBalance.subtract(frozenSavings).subtract(frozenPledges);
        log.debug("Available Balance: {} - {} - {} = {}", totalBalance, frozenSavings, frozenPledges, availableBalance);

        result.setSavingsBalance(savingsBalance);
        result.setSharesBalance(sharesBalance);
        result.setTotalBalance(totalBalance);
        result.setBaseSavings(savingsBalance);

        // Get member's loan history from database
        List<Loan> memberLoans = loanRepository.findByMemberId(member.getId());
        
        // CRITICAL: Calculate total disbursed (original loan amounts, not outstanding)
        // Only count loans with active statuses: DISBURSED, APPROVED, PARTIALLY_REPAID
        BigDecimal totalDisbursed = memberLoans.stream()
                .filter(loan -> loan.getStatus() == Loan.Status.DISBURSED || 
                               loan.getStatus() == Loan.Status.APPROVED ||
                               loan.getStatus() == Loan.Status.REPAID)
                .map(Loan::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        log.debug("Total Disbursed Loans: {}", totalDisbursed);
        
        // Calculate true savings using frozen savings (not total disbursed)
        // True Savings = Current Savings - Frozen Savings (from self-guarantees)
        BigDecimal trueSavings = savingsBalance.subtract(frozenSavings);
        if (trueSavings.compareTo(BigDecimal.ZERO) < 0) {
            trueSavings = BigDecimal.ZERO;
        }
        log.debug("True Savings: {} - {} = {}", savingsBalance, frozenSavings, trueSavings);
        
        result.setTotalDisbursed(totalDisbursed);
        result.setTrueSavings(trueSavings);
        
        // Calculate total outstanding (only active loans)
        BigDecimal totalOutstanding = memberLoans.stream()
                .filter(loan -> loan.getStatus() == Loan.Status.DISBURSED || 
                               loan.getStatus() == Loan.Status.APPROVED ||
                               loan.getStatus() == Loan.Status.REPAID)
                .map(Loan::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long defaultedLoans = memberLoans.stream()
                .filter(loan -> loan.getStatus() == Loan.Status.DEFAULTED)
                .count();

        long activeLoans = memberLoans.stream()
                .filter(loan -> loan.getStatus() == Loan.Status.DISBURSED || 
                               loan.getStatus() == Loan.Status.APPROVED ||
                               loan.getStatus() == Loan.Status.REPAID)
                .count();

        result.setTotalOutstanding(totalOutstanding);
        result.setDefaultedLoans(defaultedLoans);
        result.setActiveLoans(activeLoans);

        // Check 1: Member must be ACTIVE
        if (member.getStatus() != Member.Status.ACTIVE) {
            result.getErrors().add("Member is not ACTIVE (Current status: " + member.getStatus() + ")");
            result.setEligible(false);
        }

        // Check 1.5: Member must not be suspended
        if (memberSuspensionService.isMemberSuspended(member.getId())) {
            result.getErrors().add("Member is currently suspended");
            result.setEligible(false);
        }

        // Check 2: Member must not have defaulted loans (unless allowed by rules)
        if (defaultedLoans > 0) {
            if (!rules.getAllowDefaulters()) {
                result.getErrors().add("Member has " + defaultedLoans + " defaulted loan(s)");
                result.setEligible(false);
            } else {
                result.getWarnings().add("Member has " + defaultedLoans + " defaulted loan(s)");
            }
        }

        // Check 2.5: Six-month contribution rule
        Boolean testModeOverride = systemSettingsService.getSettingAsBoolean("TEST_MODE_OVERRIDE");
        Integer minContributionMonths = systemSettingsService.getSettingAsInteger("MIN_CONTRIBUTION_MONTHS");
        if (minContributionMonths == null) minContributionMonths = 6;
        
        Integer memberMonths = member.getConsecutiveMonthsCounter() != null ? member.getConsecutiveMonthsCounter() : 0;
        Boolean isLegacy = member.getIsLegacyMember() != null ? member.getIsLegacyMember() : false;
        
        // Legacy members are always eligible (counter = 999)
        if (!isLegacy && !testModeOverride) {
            if (memberMonths < minContributionMonths) {
                result.getErrors().add("Member has contributed " + memberMonths + " months. " + 
                    (minContributionMonths - memberMonths) + " more months required");
                result.setEligible(false);
            }
        }

        // Check 3: Minimum savings requirement (use TRUE savings, not current)
        if (trueSavings.compareTo(rules.getMinMemberSavings()) < 0) {
            result.getErrors().add("Member true savings (KES " + trueSavings + ") below minimum (KES " + rules.getMinMemberSavings() + ")");
            result.setEligible(false);
        }

        // Check 4: True savings should be at least X% of loan amount
        BigDecimal requiredBalance = loanAmount.multiply(rules.getMinSavingsToLoanRatio());
        if (trueSavings.compareTo(requiredBalance) < 0) {
            result.getErrors().add("Member true savings (KES " + trueSavings + ") should be at least " + 
                rules.getMinSavingsToLoanRatio().multiply(new BigDecimal("100")) + 
                "% of loan amount (KES " + requiredBalance + ")");
            result.setEligible(false);
        }

        // Check 5: Outstanding balance should not exceed X% of true savings
        BigDecimal maxAllowedOutstanding = trueSavings.multiply(rules.getMaxOutstandingToSavingsRatio());
        if (totalOutstanding.compareTo(maxAllowedOutstanding) > 0) {
            result.getWarnings().add("Member's outstanding balance (KES " + totalOutstanding + ") exceeds " + 
                rules.getMaxOutstandingToSavingsRatio().multiply(new BigDecimal("100")) + 
                "% of true savings (KES " + maxAllowedOutstanding + ")");
        }

        // Check 6: Maximum active loans limit (read from system settings)
        Integer maxActiveLoans = systemSettingsService.getSettingAsInteger("MAX_ACTIVE_LOANS");
        if (maxActiveLoans == null) maxActiveLoans = 3;
        
        if (activeLoans >= maxActiveLoans) {
            result.getErrors().add("Member has reached the maximum of " + maxActiveLoans + " active loans");
            result.setEligible(false);
        }

        // Check 7: CRITICAL - Loan amount cannot exceed X times TRUE savings minus EXTERNAL guaranteed outstanding
        // Formula: Gross Eligibility = True Savings × Multiplier
        //          Net Eligibility = Gross Eligibility - External Guarantee Outstanding Only
        // RULE 4 & 5: Self-guaranteed portion already frozen, don't deduct again
        BigDecimal multiplier = rules.getMaxLoanToSavingsMultiplier() != null ? rules.getMaxLoanToSavingsMultiplier() : new BigDecimal("3.0");
        BigDecimal grossEligibility = trueSavings.multiply(multiplier);
        log.debug("Gross Eligibility: {} × {} = {}", trueSavings, multiplier, grossEligibility);
        
        // Calculate only EXTERNAL guarantee portion (not self-guaranteed portion)
        BigDecimal externalGuaranteeOutstanding = calculateExternalGuaranteeOutstanding(memberLoans);
        log.debug("External Guarantee Outstanding: {}", externalGuaranteeOutstanding);
        
        BigDecimal netEligibleAmount = grossEligibility.subtract(externalGuaranteeOutstanding);
        log.debug("Net Eligible Amount: {} - {} = {}", grossEligibility, externalGuaranteeOutstanding, netEligibleAmount);
        
        if (netEligibleAmount.compareTo(BigDecimal.ZERO) < 0) {
            netEligibleAmount = BigDecimal.ZERO;
        }
        log.debug("Final Net Eligible Amount (after zero check): {}", netEligibleAmount);
        
        result.setGrossEligibility(grossEligibility);
        result.setMaxEligibleAmount(grossEligibility);
        result.setNetEligibleAmount(netEligibleAmount);
        
        if (loanAmount.compareTo(netEligibleAmount) > 0) {
            result.getErrors().add("Loan amount (KES " + loanAmount + ") exceeds remaining eligible amount (KES " + netEligibleAmount + "). Gross eligibility: KES " + grossEligibility + ", Outstanding loans: KES " + totalOutstanding);
            result.setEligible(false);
        }

        return result;
    }
    
    /**
     * Calculate only the EXTERNAL guarantee portion of outstanding loans
     * Self-guaranteed portion is already frozen and should not be deducted again
     */
    private BigDecimal calculateExternalGuaranteeOutstanding(List<Loan> memberLoans) {
        BigDecimal total = BigDecimal.ZERO;
        
        for (Loan loan : memberLoans) {
            // Only consider active loans
            if (loan.getStatus() != Loan.Status.DISBURSED && 
                loan.getStatus() != Loan.Status.APPROVED &&
                loan.getStatus() != Loan.Status.REPAID) {
                continue;
            }
            
            if (loan.getOutstandingBalance() == null || loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            // Get self-guarantee amount for this loan
            BigDecimal selfGuaranteeAmount = getSelfGuaranteeAmount(loan);

            // Calculate the portion NOT covered by self-guarantee (external portion)
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
     * Returns the FROZEN amount (pledgeAmount), not the original guarantee amount
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
}
