package com.minet.sacco.service;

import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class GuarantorValidationService {

    private static final Logger log = LoggerFactory.getLogger(GuarantorValidationService.class);

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private LoanEligibilityRulesService rulesService;

    @Autowired
    private MemberSuspensionService memberSuspensionService;

    /**
     * Validate a single guarantor for a loan application with custom guarantee amount.
     * @param guarantor The guarantor member
     * @param guaranteeAmount The amount this guarantor is pledging (not the full loan)
     */
    public GuarantorValidationResult validateGuarantorWithCustomAmount(Member guarantor, BigDecimal guaranteeAmount) {
        return validateGuarantorWithCustomAmount(guarantor, guaranteeAmount, null);
    }

    /**
     * Validate a single guarantor for a loan application with custom guarantee amount.
     * @param guarantor The guarantor member
     * @param guaranteeAmount The amount this guarantor is pledging (not the full loan)
     * @param excludeLoanId optional loan ID to exclude from pledge sum (for re-validation of existing applications)
     */
    public GuarantorValidationResult validateGuarantorWithCustomAmount(Member guarantor, BigDecimal guaranteeAmount, Long excludeLoanId) {
        log.debug("=== GUARANTOR VALIDATION START (Custom Amount) ===");
        log.debug("Guarantor: {} {}, ID: {}", guarantor.getFirstName(), guarantor.getLastName(), guarantor.getId());
        log.debug("Guarantee amount: {}", guaranteeAmount);
        
        LoanEligibilityRules rules = rulesService.getRules();
        if (rules == null) {
            log.error("ERROR: Rules object is NULL!");
            throw new RuntimeException("Loan eligibility rules not configured");
        }
        
        GuarantorValidationResult result = new GuarantorValidationResult();
        result.setGuarantorId(guarantor.getId());
        result.setGuarantorName(guarantor.getFirstName() + " " + guarantor.getLastName());
        result.setIsEligible(true);

        // Get guarantor's savings balance
        Optional<Account> savingsAccount = accountRepository.findByMemberIdAndAccountType(
                guarantor.getId(), Account.AccountType.SAVINGS);
        BigDecimal savingsBalance = savingsAccount.map(Account::getBalance).orElse(BigDecimal.ZERO);
        
        log.debug("Savings: {}", savingsBalance);
        
        result.setSavingsBalance(savingsBalance);
        result.setTotalBalance(savingsBalance);
        result.setOutstandingBalance(BigDecimal.ZERO);
        result.setAvailableGuaranteeCapacity(savingsBalance);
        result.setErrors(new ArrayList<>());
        result.setWarnings(new ArrayList<>());

        // Check 1: Member must be ACTIVE
        log.debug("Check 1: Member status = {}", guarantor.getStatus());
        if (guarantor.getStatus() != Member.Status.ACTIVE) {
            log.debug("FAIL: Not ACTIVE");
            result.addError("Guarantor is not ACTIVE (Current status: " + guarantor.getStatus() + ")");
            result.setIsEligible(false);
            return result;
        }
        log.debug("PASS: Member is ACTIVE");

        // Check 2: Member must not be suspended
        log.debug("Check 2: Checking suspension status");
        if (memberSuspensionService.isMemberSuspended(guarantor.getId())) {
            log.debug("FAIL: Member is suspended");
            Optional<MemberSuspension> suspension = memberSuspensionService.getActiveSuspension(guarantor.getId());
            String reason = suspension.map(MemberSuspension::getReason).orElse("Unknown");
            result.addError("Guarantor is suspended (Reason: " + reason + ")");
            result.setIsEligible(false);
            return result;
        }
        log.debug("PASS: Member is not suspended");

        // Check 3: Guarantor must have at least the guarantee amount
        log.debug("Check 3: Guarantee amount - Required: {}, Actual: {}", guaranteeAmount, savingsBalance);
        if (savingsBalance.compareTo(guaranteeAmount) < 0) {
            log.debug("FAIL: Insufficient savings - {} < {}", savingsBalance, guaranteeAmount);
            result.addError("Savings balance (KES " + savingsBalance + ") is less than guarantee amount (KES " + guaranteeAmount + ")");
            result.setIsEligible(false);
            return result;
        }
        log.debug("PASS: Has sufficient savings for guarantee amount");

        // Check 3: Check available guarantee capacity (already pledged amounts)
        log.debug("Check 3: Checking available guarantee capacity");
        BigDecimal alreadyPledged = excludeLoanId != null
                ? guarantorRepository.sumActivePledgesByMemberIdExcludingLoan(guarantor.getId(), excludeLoanId)
                : guarantorRepository.sumActivePledgesByMemberId(guarantor.getId());
        if (alreadyPledged == null) alreadyPledged = BigDecimal.ZERO;

        BigDecimal availableCapacity = savingsBalance.subtract(alreadyPledged);
        result.setAvailableGuaranteeCapacity(availableCapacity);

        log.debug("Check 3: savingsBalance={}, alreadyPledged={}, availableCapacity={}, guaranteeAmount={}",
                savingsBalance, alreadyPledged, availableCapacity, guaranteeAmount);

        if (availableCapacity.compareTo(guaranteeAmount) < 0) {
            log.debug("FAIL: Insufficient available capacity - {} < {}", availableCapacity, guaranteeAmount);
            result.addError(String.format(
                    "Insufficient guarantee capacity. Available: KES %,.0f (Total savings: KES %,.0f minus already pledged: KES %,.0f). Required: KES %,.0f",
                    availableCapacity, savingsBalance, alreadyPledged, guaranteeAmount));
            result.setIsEligible(false);
            return result;
        }
        log.debug("PASS: Sufficient guarantee capacity");

        // Check 4: Check for defaulted loans
        log.debug("Check 4: Checking for defaulted loans");
        List<Loan> guarantorLoans = loanRepository.findByMemberId(guarantor.getId());
        boolean hasDefaultedLoan = guarantorLoans.stream()
                .anyMatch(loan -> loan.getStatus() == Loan.Status.DEFAULTED);
        
        if (hasDefaultedLoan && !rules.getAllowDefaulters()) {
            log.debug("FAIL: Has defaulted loans and not allowed");
            result.addError("Guarantor has defaulted loans");
            result.setIsEligible(false);
            return result;
        }
        if (hasDefaultedLoan) {
            log.debug("WARNING: Has defaulted loans but allowed by rules");
            result.addWarning("Guarantor has defaulted loans");
        }
        log.debug("PASS: No blocking defaulted loans");

        log.debug("=== FINAL RESULT (Custom Amount) ===");
        log.debug("Guarantor: {}", result.getGuarantorName());
        log.debug("Is Eligible: {}", result.isEligible());
        log.debug("Savings: {}", result.getTotalBalance());
        log.debug("Errors: {}", result.getErrors());
        log.debug("=== END VALIDATION ===");
        return result;
    }

    /**
     * Validate a single guarantor for a loan application.
     * Delegates to the overload with no excluded loan.
     */
    public GuarantorValidationResult validateGuarantor(Member guarantor, BigDecimal loanAmount) {
        return validateGuarantor(guarantor, loanAmount, null);
    }

    /**
     * Validate a single guarantor for a loan application.
     * @param excludeLoanId optional loan ID to exclude from pledge sum (for re-validation of existing applications)
     */
    public GuarantorValidationResult validateGuarantor(Member guarantor, BigDecimal loanAmount, Long excludeLoanId) {
        // For backward compatibility, use loanAmount as guaranteeAmount if not specified
        return validateGuarantorWithGuaranteeAmount(guarantor, loanAmount, loanAmount, excludeLoanId);
    }

    public GuarantorValidationResult validateGuarantorWithGuaranteeAmount(Member guarantor, BigDecimal loanAmount, BigDecimal guaranteeAmount, Long excludeLoanId) {
        log.debug("=== GUARANTOR VALIDATION START ===");
        log.debug("Guarantor: {} {}, ID: {}", guarantor.getFirstName(), guarantor.getLastName(), guarantor.getId());
        log.debug("Loan amount: {}, Guarantee amount: {}", loanAmount, guaranteeAmount);
        
        LoanEligibilityRules rules = rulesService.getRules();
        if (rules == null) {
            log.error("ERROR: Rules object is NULL!");
            throw new RuntimeException("Loan eligibility rules not configured");
        }
        
        log.debug("Rules object: {}", rules);
        log.debug("Rules ID: {}", rules.getId());
        
        // Ensure all required fields have default values
        if (rules.getMinGuarantorSavings() == null) {
            log.warn("minGuarantorSavings is NULL, setting default to 10000");
            rules.setMinGuarantorSavings(new BigDecimal("10000"));
        }
        if (rules.getMinGuarantorSavingsToLoanRatio() == null) {
            log.warn("minGuarantorSavingsToLoanRatio is NULL, setting default to 0.50");
            rules.setMinGuarantorSavingsToLoanRatio(new BigDecimal("0.50"));
        }
        if (rules.getAllowDefaulters() == null) {
            log.warn("allowDefaulters is NULL, setting default to false");
            rules.setAllowDefaulters(false);
        }
        if (rules.getMaxGuarantorCommitments() == null) {
            log.warn("maxGuarantorCommitments is NULL, setting default to 3");
            rules.setMaxGuarantorCommitments(3);
        }
        if (rules.getMaxGuarantorOutstandingToSavingsRatio() == null) {
            log.warn("maxGuarantorOutstandingToSavingsRatio is NULL, setting default to 0.50");
            rules.setMaxGuarantorOutstandingToSavingsRatio(new BigDecimal("0.50"));
        }
        
        log.debug("Rules - minGuarantorSavings: {}, minGuarantorSavingsToLoanRatio: {}", 
            rules.getMinGuarantorSavings(), rules.getMinGuarantorSavingsToLoanRatio());
        log.debug("Rules - allowDefaulters: {}, maxGuarantorCommitments: {}", 
            rules.getAllowDefaulters(), rules.getMaxGuarantorCommitments());
        
        GuarantorValidationResult result = new GuarantorValidationResult();
        result.setGuarantorId(guarantor.getId());
        result.setGuarantorName(guarantor.getFirstName() + " " + guarantor.getLastName());
        result.setIsEligible(true);

        // Get guarantor's savings and shares balance
        Optional<Account> savingsAccount = accountRepository.findByMemberIdAndAccountType(
                guarantor.getId(), Account.AccountType.SAVINGS);
        Optional<Account> sharesAccount = accountRepository.findByMemberIdAndAccountType(
                guarantor.getId(), Account.AccountType.SHARES);
        
        BigDecimal savingsBalance = savingsAccount.map(Account::getBalance).orElse(BigDecimal.ZERO);
        BigDecimal sharesBalance = sharesAccount.map(Account::getBalance).orElse(BigDecimal.ZERO);
        // For guarantor eligibility, only SAVINGS count (not shares)
        // Shares are capital contributions and don't count toward guarantee capacity
        BigDecimal totalBalance = savingsBalance;
        
        log.debug("Savings: {}, Shares: {}, Total: {}", savingsBalance, sharesBalance, totalBalance);
        log.debug("Savings account found: {}, Shares account found: {}", savingsAccount.isPresent(), sharesAccount.isPresent());
        
        // Initialize all fields immediately to avoid null pointer exceptions
        result.setSavingsBalance(savingsBalance);
        result.setSharesBalance(sharesBalance);
        result.setTotalBalance(totalBalance);
        result.setOutstandingBalance(BigDecimal.ZERO); // Initialize to zero
        result.setAvailableGuaranteeCapacity(totalBalance); // Will be updated after pledge check
        result.setErrors(new ArrayList<>());
        result.setWarnings(new ArrayList<>());

        // Check 1: Member must be ACTIVE
        log.debug("Check 1: Member status = {}", guarantor.getStatus());
        if (guarantor.getStatus() != Member.Status.ACTIVE) {
            log.debug("FAIL: Not ACTIVE");
            result.addError("Guarantor is not ACTIVE (Current status: " + guarantor.getStatus() + ")");
            result.setIsEligible(false);
            log.debug("=== RESULT: NOT ELIGIBLE (Check 1 - Status) ===");
            return result;
        }
        log.debug("PASS: Member is ACTIVE");

        // Check 2: Member must not be suspended
        log.debug("Check 2: Checking suspension status");
        if (memberSuspensionService.isMemberSuspended(guarantor.getId())) {
            log.debug("FAIL: Member is suspended");
            Optional<MemberSuspension> suspension = memberSuspensionService.getActiveSuspension(guarantor.getId());
            String reason = suspension.map(MemberSuspension::getReason).orElse("Unknown");
            result.addError("Guarantor is suspended (Reason: " + reason + ")");
            result.setIsEligible(false);
            log.debug("=== RESULT: NOT ELIGIBLE (Check 2 - Suspension) ===");
            return result;
        }
        log.debug("PASS: Member is not suspended");

        // Check 3: Minimum savings requirement
        log.debug("Check 3: Min savings - Required: {}, Actual: {}", rules.getMinGuarantorSavings(), totalBalance);
        if (totalBalance.compareTo(rules.getMinGuarantorSavings()) < 0) {
            log.debug("FAIL: Below minimum savings - {} < {}", totalBalance, rules.getMinGuarantorSavings());
            result.addError("Savings balance (KES " + totalBalance + ") below minimum (KES " + rules.getMinGuarantorSavings() + ")");
            result.setIsEligible(false);
            log.debug("=== RESULT: NOT ELIGIBLE (Check 3 - Min Balance) ===");
            return result;
        }
        log.debug("PASS: Meets minimum balance");

        // Check 4 (NEW - MOVED BEFORE LOAN RATIO): Available guarantee capacity check
        // A guarantor's savings are partially "frozen" for each active guarantee.
        // available_capacity = totalBalance - sum(active pledge amounts)
        // This must be checked BEFORE the loan ratio check so we always show correct available capacity
        log.debug("Check 4: Checking available guarantee capacity");
        BigDecimal alreadyPledged = excludeLoanId != null
                ? guarantorRepository.sumActivePledgesByMemberIdExcludingLoan(guarantor.getId(), excludeLoanId)
                : guarantorRepository.sumActivePledgesByMemberId(guarantor.getId());
        if (alreadyPledged == null) alreadyPledged = BigDecimal.ZERO;

        BigDecimal availableCapacity = totalBalance.subtract(alreadyPledged);
        result.setAvailableGuaranteeCapacity(availableCapacity);

        log.debug("Check 3: totalBalance={}, alreadyPledged={}, availableCapacity={}, guaranteeAmount={}",
                totalBalance, alreadyPledged, availableCapacity, guaranteeAmount);

        if (availableCapacity.compareTo(guaranteeAmount) < 0) {
            log.debug("FAIL: Insufficient available guarantee capacity - {} < {}", availableCapacity, guaranteeAmount);
            result.addError(String.format(
                    "Insufficient guarantee capacity. Available: KES %,.0f (Total savings: KES %,.0f minus already pledged: KES %,.0f). Required: KES %,.0f",
                    availableCapacity, totalBalance, alreadyPledged, guaranteeAmount));
            result.setIsEligible(false);
            log.debug("=== RESULT: NOT ELIGIBLE (Check 3 - Pledge Capacity) ===");
            return result;
        }
        log.debug("PASS: Sufficient guarantee capacity");

        // Check 5: Savings should be at least X% of guarantee amount (not full loan amount)
        BigDecimal requiredBalance = guaranteeAmount.multiply(rules.getMinGuarantorSavingsToLoanRatio());
        log.debug("Check 5: Guarantee ratio - Guarantee: {}, Ratio: {}, Required: {}, Actual: {}", 
            guaranteeAmount, rules.getMinGuarantorSavingsToLoanRatio(), requiredBalance, totalBalance);
        if (totalBalance.compareTo(requiredBalance) < 0) {
            log.debug("FAIL: Below required ratio - {} < {}", totalBalance, requiredBalance);
            result.addError("Savings balance (KES " + totalBalance + ") should be at least " + 
                rules.getMinGuarantorSavingsToLoanRatio().multiply(new BigDecimal("100")) + 
                "% of guarantee amount (KES " + requiredBalance + ")");
            result.setIsEligible(false);
            log.debug("=== RESULT: NOT ELIGIBLE (Check 4 - Guarantee Ratio) ===");
            return result;
        }
        log.debug("PASS: Meets guarantee ratio requirement");

        // Check 6: Check for defaulted loans
        log.debug("Check 6: Checking for defaulted loans");
        List<Loan> guarantorLoans = loanRepository.findByMemberId(guarantor.getId());
        log.debug("Total loans for guarantor: {}", guarantorLoans.size());
        boolean hasDefaultedLoan = guarantorLoans.stream()
                .anyMatch(loan -> loan.getStatus() == Loan.Status.DEFAULTED);
        log.debug("Has defaulted loan: {}, allowDefaulters: {}", hasDefaultedLoan, rules.getAllowDefaulters());
        
        if (hasDefaultedLoan && !rules.getAllowDefaulters()) {
            log.debug("FAIL: Has defaulted loans and not allowed");
            result.addError("Guarantor has defaulted loans");
            result.setIsEligible(false);
            log.debug("=== RESULT: NOT ELIGIBLE (Check 5) ===");
            return result;
        }
        if (hasDefaultedLoan) {
            log.debug("WARNING: Has defaulted loans but allowed by rules");
            result.addWarning("Guarantor has defaulted loans");
        }
        log.debug("PASS: No blocking defaulted loans");

        // Check 7: Count active guarantor commitments (warning only)
        log.debug("Check 7: Checking active guarantor commitments");
        List<Guarantor> activeGuarantorships = guarantorRepository.findByMemberIdAndStatus(
                guarantor.getId(), Guarantor.Status.ACCEPTED);
        
        if (activeGuarantorships.size() >= rules.getMaxGuarantorCommitments()) {
            log.debug("WARNING: At max commitments");
            result.addWarning("Already guarantor for " + activeGuarantorships.size() + " loans");
        }

        // Check 8: Outstanding loan balance (warning only)
        log.debug("Check 8: Checking outstanding balance");
        BigDecimal totalOutstanding = guarantorLoans.stream()
                .filter(loan -> loan.getStatus() == Loan.Status.DISBURSED || loan.getStatus() == Loan.Status.APPROVED)
                .map(Loan::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        result.setOutstandingBalance(totalOutstanding);

        BigDecimal maxAllowedOutstanding = totalBalance.multiply(rules.getMaxGuarantorOutstandingToSavingsRatio());
        if (totalOutstanding.compareTo(maxAllowedOutstanding) > 0) {
            log.debug("WARNING: Outstanding exceeds threshold");
            result.addWarning("Outstanding balance exceeds " + 
                rules.getMaxGuarantorOutstandingToSavingsRatio().multiply(new BigDecimal("100")) + "% of total balance");
        }

        log.debug("=== FINAL RESULT ===");
        log.debug("Guarantor: {}", result.getGuarantorName());
        log.debug("Is Eligible: {}", result.isEligible());
        log.debug("Total Balance: {}", result.getTotalBalance());
        log.debug("Errors: {}", result.getErrors());
        log.debug("Warnings: {}", result.getWarnings());
        log.debug("=== END VALIDATION ===");
        return result;
    }

    /**
     * Validate all guarantors for a loan
     */
    public List<GuarantorValidationResult> validateAllGuarantors(List<Long> guarantorIds, BigDecimal loanAmount) {
        List<GuarantorValidationResult> results = new ArrayList<>();
        
        for (Long guarantorId : guarantorIds) {
            Member guarantor = memberRepository.findById(guarantorId)
                    .orElseThrow(() -> new RuntimeException("Guarantor not found: " + guarantorId));
            
            GuarantorValidationResult result = validateGuarantor(guarantor, loanAmount);
            results.add(result);
        }
        
        return results;
    }

    /**
     * Check if all guarantors are eligible (no errors)
     */
    public boolean areAllGuarantorsEligible(List<GuarantorValidationResult> results) {
        return results.stream().allMatch(GuarantorValidationResult::isEligible);
    }

    /**
     * Get summary of validation issues
     */
    public String getValidationSummary(List<GuarantorValidationResult> results) {
        StringBuilder summary = new StringBuilder();
        
        for (GuarantorValidationResult result : results) {
            summary.append("\n--- ").append(result.getGuarantorName()).append(" ---\n");
            
            if (result.isEligible()) {
                summary.append("✓ ELIGIBLE\n");
            } else {
                summary.append("✗ NOT ELIGIBLE\n");
            }
            
            if (!result.getErrors().isEmpty()) {
                summary.append("Errors:\n");
                for (String error : result.getErrors()) {
                    summary.append("  • ").append(error).append("\n");
                }
            }
            
            if (!result.getWarnings().isEmpty()) {
                summary.append("Warnings:\n");
                for (String warning : result.getWarnings()) {
                    summary.append("  ⚠ ").append(warning).append("\n");
                }
            }
        }
        
        return summary.toString();
    }

    /**
     * Result class for guarantor validation
     */
    public static class GuarantorValidationResult {
        private Long guarantorId;
        private String guarantorName;
        
        @com.fasterxml.jackson.annotation.JsonProperty("isEligible")
        private boolean isEligible;
        
        private BigDecimal savingsBalance;
        private BigDecimal sharesBalance;
        private BigDecimal totalBalance;
        private BigDecimal outstandingBalance;
        private BigDecimal availableGuaranteeCapacity;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        public void addError(String error) {
            this.errors.add(error);
        }

        public void addWarning(String warning) {
            this.warnings.add(warning);
        }

        // Getters and Setters
        public Long getGuarantorId() { return guarantorId; }
        public void setGuarantorId(Long guarantorId) { this.guarantorId = guarantorId; }

        public String getGuarantorName() { return guarantorName; }
        public void setGuarantorName(String guarantorName) { this.guarantorName = guarantorName; }

        public boolean isEligible() { return isEligible; }
        public void setIsEligible(boolean eligible) { isEligible = eligible; }

        public BigDecimal getSavingsBalance() { return savingsBalance; }
        public void setSavingsBalance(BigDecimal savingsBalance) { this.savingsBalance = savingsBalance; }

        public BigDecimal getSharesBalance() { return sharesBalance; }
        public void setSharesBalance(BigDecimal sharesBalance) { this.sharesBalance = sharesBalance; }

        public BigDecimal getTotalBalance() { return totalBalance; }
        public void setTotalBalance(BigDecimal totalBalance) { this.totalBalance = totalBalance; }

        public BigDecimal getOutstandingBalance() { return outstandingBalance; }
        public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }

        public BigDecimal getAvailableGuaranteeCapacity() { return availableGuaranteeCapacity; }
        public void setAvailableGuaranteeCapacity(BigDecimal availableGuaranteeCapacity) { this.availableGuaranteeCapacity = availableGuaranteeCapacity; }

        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }

        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }
}
