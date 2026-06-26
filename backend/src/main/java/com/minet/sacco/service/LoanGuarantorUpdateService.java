package com.minet.sacco.service;

import com.minet.sacco.entity.*;
import com.minet.sacco.repository.GuarantorRepository;
import com.minet.sacco.repository.AccountRepository;
import com.minet.sacco.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Handles atomic guarantor updates on existing loans with freeze/unfreeze mechanics.
 * Used by loan migration (UPDATE mode) and other loan modification flows.
 * 
 * Key properties:
 * - All-or-nothing: if any guarantor update provided, replaces ALL existing guarantors
 * - Transactional: entire operation succeeds or rolls back
 * - Audit trail: logs before/after guarantor changes
 */
@Service
public class LoanGuarantorUpdateService {

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuditService auditService;

    /**
     * Helper class to hold guarantor pair data
     */
    public static class GuarantorPair {
        public String employeeId;
        public BigDecimal pledgeAmount;

        public GuarantorPair(String employeeId, BigDecimal pledgeAmount) {
            this.employeeId = employeeId;
            this.pledgeAmount = pledgeAmount;
        }
    }

    /**
     * Update guarantors for a loan. If new guarantors provided, replaces all existing ones.
     * Handles freeze/unfreeze of member savings automatically for DISBURSED loans.
     * 
     * @param loan The loan to update guarantors for
     * @param newGuarantors List of new guarantor pairs (employeeId, pledgeAmount). If null or empty, keeps existing.
     * @param auditor User performing the update
     * @return Audit message describing changes made
     * @throws RuntimeException if validation fails or guarantor freeze fails
     */
    @Transactional
    public String updateGuarantors(Loan loan, List<GuarantorPair> newGuarantors, User auditor) {
        if (loan == null) {
            throw new RuntimeException("Loan cannot be null");
        }

        // If no new guarantors provided, skip guarantor update
        if (newGuarantors == null || newGuarantors.isEmpty()) {
            return "No guarantor changes";
        }

        // Load current guarantors for audit trail
        List<Guarantor> currentGuarantors = guarantorRepository.findByLoan(loan);
        String oldGuarantorSummary = buildGuarantorSummary(currentGuarantors);

        // Validate all new guarantors exist and are ACTIVE members
        List<Member> validatedMembers = new ArrayList<>();
        for (GuarantorPair pair : newGuarantors) {
            if (pair.employeeId == null || pair.employeeId.isBlank()) {
                throw new RuntimeException("Guarantor Employee ID cannot be blank");
            }
            if (pair.pledgeAmount == null || pair.pledgeAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Pledge amount for guarantor '" + pair.employeeId + "' must be greater than 0");
            }

            Optional<Member> memberOpt = memberRepository.findByMemberNumber(pair.employeeId.trim());
            if (memberOpt.isEmpty()) {
                throw new RuntimeException("Guarantor with Employee ID '" + pair.employeeId + "' not found");
            }

            Member member = memberOpt.get();
            // For migrated loans, be more lenient with member status - allow any member, not just ACTIVE
            // This accounts for historical data where members may have different statuses
            if (loan.getMigrationStatus() != null && "MIGRATED".equals(loan.getMigrationStatus())) {
                // For migrated loans: allow members with any valid status (not just ACTIVE)
                // Only reject if member is explicitly null or in ERROR state
                if (member.getStatus() == null) {
                    throw new RuntimeException("Guarantor '" + pair.employeeId + "' has invalid status");
                }
            } else {
                // For non-migrated loans: strictly require ACTIVE status
                if (member.getStatus() == null || !member.getStatus().equals(Member.Status.ACTIVE)) {
                    throw new RuntimeException("Guarantor '" + pair.employeeId + "' is not ACTIVE (status: " + member.getStatus() + ")");
                }
            }

            validatedMembers.add(member);
        }

        // For DISBURSED loans, validate guarantors have sufficient available savings
        if (loan.getStatus() == Loan.Status.DISBURSED) {
            for (int i = 0; i < newGuarantors.size(); i++) {
                GuarantorPair pair = newGuarantors.get(i);
                Member member = validatedMembers.get(i);

                Optional<Account> savingsAccount = accountRepository.findByMemberIdAndAccountType(
                    member.getId(), Account.AccountType.SAVINGS
                );

                if (savingsAccount.isPresent()) {
                    BigDecimal availableSavings = getAvailableSavings(savingsAccount.get());
                    if (availableSavings.compareTo(pair.pledgeAmount) < 0) {
                        throw new RuntimeException(
                            "Member '" + pair.employeeId + "' has insufficient available savings. " +
                            "Required: " + pair.pledgeAmount + ", Available: " + availableSavings
                        );
                    }
                } else {
                    throw new RuntimeException("Member '" + pair.employeeId + "' has no savings account");
                }
            }
        }

        // Begin atomic transaction: unfreeze old, delete old, create new, freeze new
        
        // Step 1: Unfreeze all old guarantors' savings (if DISBURSED loan)
        if (loan.getStatus() == Loan.Status.DISBURSED) {
            for (Guarantor oldGuarantor : currentGuarantors) {
                unfreezeGuarantorSavings(oldGuarantor.getMember(), oldGuarantor.getPledgeAmount());
            }
        }

        // Step 2: Delete old guarantor records
        guarantorRepository.deleteAll(currentGuarantors);

        // Step 3: Create new guarantor records
        List<Guarantor> newGuarantorEntities = new ArrayList<>();
        for (int i = 0; i < newGuarantors.size(); i++) {
            GuarantorPair pair = newGuarantors.get(i);
            Member member = validatedMembers.get(i);

            Guarantor guarantor = new Guarantor();
            guarantor.setLoan(loan);
            guarantor.setMember(member);
            guarantor.setSelfGuarantee(false);
            guarantor.setGuaranteeAmount(pair.pledgeAmount);
            // For DISBURSED loans, freeze the pledge. For others, leave as 0.
            guarantor.setPledgeAmount(loan.getStatus() == Loan.Status.DISBURSED ? pair.pledgeAmount : BigDecimal.ZERO);
            guarantor.setPledgeFrozenAtFullAmount(true);  // Mark as manually set - DO NOT apply reduction ratio on repayment
            guarantor.setStatus(loan.getStatus() == Loan.Status.DISBURSED ? Guarantor.Status.ACTIVE : Guarantor.Status.RELEASED);
            guarantor.setApprovedAt(LocalDateTime.now());
            guarantor.setMigrationStatus("ACTIVE");
            newGuarantorEntities.add(guarantor);
        }
        guarantorRepository.saveAll(newGuarantorEntities);

        // Step 4: Freeze new guarantors' savings (if DISBURSED loan)
        if (loan.getStatus() == Loan.Status.DISBURSED) {
            for (int i = 0; i < newGuarantors.size(); i++) {
                freezeGuarantorSavings(validatedMembers.get(i), newGuarantors.get(i).pledgeAmount);
            }
        }

        // Build audit message
        String newGuarantorSummary = buildGuarantorSummary(newGuarantorEntities);
        String changeDescription = "Guarantors changed: " + oldGuarantorSummary + " → " + newGuarantorSummary;

        // Log to audit trail
        auditService.logAction(auditor, "LOAN_GUARANTOR_UPDATE", "Loan", loan.getId(),
            changeDescription + " for loan " + loan.getLoanNumber(), null, null);

        return changeDescription;
    }

    /**
     * Freeze a guarantor's savings for a pledge amount.
     */
    private void freezeGuarantorSavings(Member member, BigDecimal amount) {
        Optional<Account> accountOpt = accountRepository.findByMemberIdAndAccountType(
            member.getId(), Account.AccountType.SAVINGS
        );
        accountOpt.ifPresent(account -> {
            BigDecimal currentFrozen = account.getFrozenSavings() != null ? account.getFrozenSavings() : BigDecimal.ZERO;
            account.setFrozenSavings(currentFrozen.add(amount));
            accountRepository.save(account);
        });
    }

    /**
     * Unfreeze a guarantor's savings for a pledge amount.
     */
    private void unfreezeGuarantorSavings(Member member, BigDecimal amount) {
        Optional<Account> accountOpt = accountRepository.findByMemberIdAndAccountType(
            member.getId(), Account.AccountType.SAVINGS
        );
        accountOpt.ifPresent(account -> {
            BigDecimal currentFrozen = account.getFrozenSavings() != null ? account.getFrozenSavings() : BigDecimal.ZERO;
            BigDecimal newFrozen = currentFrozen.subtract(amount);
            // Ensure frozen never goes negative
            if (newFrozen.compareTo(BigDecimal.ZERO) < 0) {
                newFrozen = BigDecimal.ZERO;
            }
            account.setFrozenSavings(newFrozen);
            accountRepository.save(account);
        });
    }

    /**
     * Calculate available savings = balance - frozen
     */
    private BigDecimal getAvailableSavings(Account savingsAccount) {
        BigDecimal balance = savingsAccount.getBalance() != null ? savingsAccount.getBalance() : BigDecimal.ZERO;
        BigDecimal frozen = savingsAccount.getFrozenSavings() != null ? savingsAccount.getFrozenSavings() : BigDecimal.ZERO;
        return balance.subtract(frozen);
    }

    /**
     * Build human-readable summary of guarantors for audit trail.
     * Format: "EMP001(50k), EMP002(30k)" or "SELF" if self-guaranteed
     */
    private String buildGuarantorSummary(List<Guarantor> guarantors) {
        if (guarantors == null || guarantors.isEmpty()) {
            return "NONE";
        }

        List<String> parts = new ArrayList<>();
        for (Guarantor g : guarantors) {
            if (g.isSelfGuarantee()) {
                parts.add("SELF");
            } else {
                parts.add(g.getMember().getMemberNumber() + "(" + g.getGuaranteeAmount() + ")");
            }
        }
        return String.join(", ", parts);
    }
}
