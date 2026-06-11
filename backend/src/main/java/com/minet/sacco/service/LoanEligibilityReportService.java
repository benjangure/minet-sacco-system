package com.minet.sacco.service;

import com.minet.sacco.dto.LoanEligibilityReportDTO;
import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
public class LoanEligibilityReportService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private LoanRepository loanRepository;

    private static final Integer MINIMUM_CONTRIBUTION_MONTHS = 6;

    /**
     * Generate Loan Eligibility Report for a member
     */
    public LoanEligibilityReportDTO generateLoanEligibilityReport(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found with ID: " + memberId));

        LoanEligibilityReportDTO report = new LoanEligibilityReportDTO();
        report.setMemberId(member.getId());
        report.setMemberNumber(member.getMemberNumber());
        report.setMemberName(member.getFirstName() + " " + member.getLastName());
        report.setMemberStatus(member.getStatus().toString());
        report.setDateJoined(member.getCreatedAt() != null ? member.getCreatedAt().toLocalDate() : LocalDate.now());

        // Calculate months as member
        LocalDate dateJoined = report.getDateJoined();
        LocalDate today = LocalDate.now();
        long monthsDiff = java.time.temporal.ChronoUnit.MONTHS.between(dateJoined, today);
        report.setMonthsAsMember((int) monthsDiff);

        // Get Savings Balance (SAVINGS account type)
        BigDecimal savingsBalance = calculateSavingsBalance(memberId);
        report.setSavingsBalance(savingsBalance);

        // Calculate Frozen Amount (self-guarantee pledges only)
        BigDecimal frozenAmount = calculateFrozenSelfGuarantee(memberId);
        report.setFrozenAmount(frozenAmount);

        // Calculate Available Savings
        BigDecimal availableSavings = savingsBalance.subtract(frozenAmount);
        if (availableSavings.compareTo(BigDecimal.ZERO) < 0) {
            availableSavings = BigDecimal.ZERO;
        }
        report.setAvailableSavings(availableSavings);

        // Calculate Gross Eligibility (Available Savings * 3)
        BigDecimal grossEligibility = availableSavings.multiply(new BigDecimal(3));
        report.setGrossEligibility(grossEligibility);

        // Calculate Outstanding Loan Balance (DISBURSED loans only)
        BigDecimal outstandingLoanBalance = calculateOutstandingLoanBalance(memberId);
        report.setOutstandingLoanBalance(outstandingLoanBalance);

        // Calculate Remaining Eligibility
        BigDecimal remainingEligibility = grossEligibility.subtract(outstandingLoanBalance);
        if (remainingEligibility.compareTo(BigDecimal.ZERO) < 0) {
            remainingEligibility = BigDecimal.ZERO;
        }
        report.setRemainingEligibility(remainingEligibility);

        // Get Months Contributed from member.consecutiveMonthsCounter
        Integer monthsContributed = member.getConsecutiveMonthsCounter() != null ? member.getConsecutiveMonthsCounter() : 0;
        report.setMonthsContributed(monthsContributed);

        // Determine Eligibility Status
        determineEligibilityStatus(report, member);

        return report;
    }

    /**
     * Calculate savings balance for a member (SAVINGS account type)
     */
    private BigDecimal calculateSavingsBalance(Long memberId) {
        List<Account> savingsAccounts = accountRepository.findByMemberIdAndAccountType(memberId, "SAVINGS");
        return savingsAccounts.stream()
                .map(Account::getBalance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate frozen self-guarantee amount
     * Frozen Amount = SUM(guarantor.pledgeAmount) WHERE self_guarantee = true AND status = ACTIVE
     */
    private BigDecimal calculateFrozenSelfGuarantee(Long memberId) {
        List<Guarantor> selfGuarantees = guarantorRepository.findByMemberIdAndSelfGuaranteeIsTrueAndStatus(
                memberId,
                Guarantor.Status.ACTIVE
        );
        return selfGuarantees.stream()
                .map(Guarantor::getPledgeAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculate outstanding loan balance (DISBURSED loans only)
     */
    private BigDecimal calculateOutstandingLoanBalance(Long memberId) {
        List<Loan> disbursedLoans = loanRepository.findByMemberIdAndStatus(memberId, Loan.Status.DISBURSED);
        return disbursedLoans.stream()
                .map(Loan::getOutstandingBalance)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Determine eligibility status and reason
     */
    private void determineEligibilityStatus(LoanEligibilityReportDTO report, Member member) {
        // Check all eligibility conditions
        if (!member.getStatus().equals(Member.Status.ACTIVE)) {
            report.setEligibilityStatus("NOT_ELIGIBLE");
            report.setEligibilityReason("Member status is " + member.getStatus());
            return;
        }

        if (report.getMonthsContributed() < MINIMUM_CONTRIBUTION_MONTHS) {
            report.setEligibilityStatus("NOT_ELIGIBLE");
            report.setEligibilityReason("Less than " + MINIMUM_CONTRIBUTION_MONTHS + " months membership");
            return;
        }

        if (report.getAvailableSavings().compareTo(BigDecimal.ZERO) <= 0) {
            report.setEligibilityStatus("NOT_ELIGIBLE");
            report.setEligibilityReason("Insufficient savings");
            return;
        }

        if (report.getRemainingEligibility().compareTo(BigDecimal.ZERO) <= 0) {
            report.setEligibilityStatus("NOT_ELIGIBLE");
            report.setEligibilityReason("Outstanding loan balance exceeds eligibility");
            return;
        }

        // All conditions met
        report.setEligibilityStatus("ELIGIBLE");
        report.setEligibilityReason("Member meets all eligibility criteria");
    }
}
