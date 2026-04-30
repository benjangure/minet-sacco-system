package com.minet.sacco.service;

import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class MemberExitService {

    @Autowired
    private MemberExitRepository memberExitRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private AuditService auditService;

    /**
     * Initiate member exit
     */
    @Transactional
    public MemberExit initiateMemberExit(Long memberId, String exitReason, User initiatedBy) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Check if member is active guarantor
        long activeGuarantorCount = guarantorRepository.countByMemberIdAndStatus(memberId, Guarantor.Status.ACTIVE);
        if (activeGuarantorCount > 0) {
            throw new RuntimeException("Member is an active guarantor for " + activeGuarantorCount + " loan(s). Cannot exit until guarantor roles are replaced.");
        }

        // Check if exit already initiated
        Optional<MemberExit> existing = memberExitRepository.findByMemberId(memberId);
        if (existing.isPresent() && existing.get().getApprovedBy() == null) {
            throw new RuntimeException("Exit already initiated for this member");
        }

        // Calculate exit summary
        Account savingsAccount = accountRepository.findByMemberIdAndAccountType(memberId, Account.AccountType.SAVINGS)
                .orElse(null);
        BigDecimal savingsBalance = savingsAccount != null ? savingsAccount.getBalance() : BigDecimal.ZERO;

        // Get outstanding loans
        List<Loan> activeLoans = loanRepository.findByMemberIdAndStatus(memberId, Loan.Status.DISBURSED);
        BigDecimal outstandingLoan = activeLoans.stream()
                .map(Loan::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate deduction and payout
        BigDecimal loanDeduction = savingsBalance.min(outstandingLoan);
        BigDecimal remainingPayout = savingsBalance.subtract(loanDeduction);
        if (remainingPayout.compareTo(BigDecimal.ZERO) < 0) {
            remainingPayout = BigDecimal.ZERO;
        }

        BigDecimal sharesRefund = new BigDecimal("3000.00");
        BigDecimal totalPayout = remainingPayout.add(sharesRefund);

        // Create exit record
        MemberExit exit = new MemberExit();
        exit.setMember(member);
        exit.setExitReason(exitReason);
        exit.setInitiatedBy(initiatedBy);
        exit.setSavingsBalance(savingsBalance);
        exit.setOutstandingLoan(outstandingLoan);
        exit.setLoanDeduction(loanDeduction);
        exit.setRemainingPayout(remainingPayout);
        exit.setSharesRefund(sharesRefund);
        exit.setTotalPayout(totalPayout);
        exit.setIsActiveGuarantor(activeGuarantorCount > 0);

        MemberExit saved = memberExitRepository.save(exit);

        auditService.logAction(initiatedBy, "MEMBER_EXIT_INITIATED",
                "Member", memberId,
                "Exit reason: " + exitReason + ", Total payout: " + totalPayout,
                "Member exit initiated", "SUCCESS");

        return saved;
    }

    /**
     * Approve member exit
     */
    @Transactional
    public MemberExit approveMemberExit(Long exitId, User approvedBy) {
        MemberExit exit = memberExitRepository.findById(exitId)
                .orElseThrow(() -> new RuntimeException("Exit record not found"));

        if (exit.getApprovedBy() != null) {
            throw new RuntimeException("Exit already approved");
        }

        exit.setApprovedBy(approvedBy);
        exit.setApprovedAt(LocalDateTime.now());
        exit.setExitDate(LocalDateTime.now());

        // Update member status to INACTIVE
        Member member = exit.getMember();
        member.setStatus(Member.Status.EXITED);
        memberRepository.save(member);

        MemberExit updated = memberExitRepository.save(exit);

        auditService.logAction(approvedBy, "MEMBER_EXIT_APPROVED",
                "Member", member.getId(),
                "Total payout: " + exit.getTotalPayout(),
                "Member exit approved", "SUCCESS");

        return updated;
    }

    /**
     * Get exit record for member
     */
    public Optional<MemberExit> getMemberExit(Long memberId) {
        return memberExitRepository.findByMemberId(memberId);
    }

    /**
     * Get pending exits (not yet approved)
     */
    public List<MemberExit> getPendingExits() {
        return memberExitRepository.findByApprovedByIsNull();
    }

    /**
     * Get approved exits
     */
    public List<MemberExit> getApprovedExits() {
        return memberExitRepository.findByApprovedByIsNotNull();
    }

    /**
     * Calculate exit summary
     */
    public Map<String, Object> calculateExitSummary(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        Account savingsAccount = accountRepository.findByMemberIdAndAccountType(memberId, Account.AccountType.SAVINGS)
                .orElse(null);
        BigDecimal savingsBalance = savingsAccount != null ? savingsAccount.getBalance() : BigDecimal.ZERO;

        List<Loan> activeLoans = loanRepository.findByMemberIdAndStatus(memberId, Loan.Status.DISBURSED);
        BigDecimal outstandingLoan = activeLoans.stream()
                .map(Loan::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal loanDeduction = savingsBalance.min(outstandingLoan);
        BigDecimal remainingPayout = savingsBalance.subtract(loanDeduction);
        if (remainingPayout.compareTo(BigDecimal.ZERO) < 0) {
            remainingPayout = BigDecimal.ZERO;
        }

        BigDecimal sharesRefund = new BigDecimal("3000.00");
        BigDecimal totalPayout = remainingPayout.add(sharesRefund);

        long activeGuarantorCount = guarantorRepository.countByMemberIdAndStatus(memberId, Guarantor.Status.ACTIVE);

        return Map.of(
                "memberId", memberId,
                "memberName", member.getFirstName() + " " + member.getLastName(),
                "savingsBalance", savingsBalance,
                "outstandingLoan", outstandingLoan,
                "loanDeduction", loanDeduction,
                "remainingPayout", remainingPayout,
                "sharesRefund", sharesRefund,
                "totalPayout", totalPayout,
                "isActiveGuarantor", activeGuarantorCount > 0,
                "activeGuarantorCount", activeGuarantorCount
        );
    }
}
