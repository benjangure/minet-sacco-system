package com.minet.sacco.service;

import com.minet.sacco.dto.GuarantorReportDTO;
import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GuarantorReportService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountRepository accountRepository;

    /**
     * Generate Guarantor Report for a single member
     */
    public GuarantorReportDTO generateGuarantorReport(Long memberId, String guarantorStatus) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found with ID: " + memberId));

        GuarantorReportDTO.MemberGuarantorDetail memberDetail = new GuarantorReportDTO.MemberGuarantorDetail();
        memberDetail.setMemberId(member.getId());
        memberDetail.setMemberNumber(member.getMemberNumber());
        memberDetail.setMemberName(member.getFirstName() + " " + member.getLastName());
        memberDetail.setMemberStatus(member.getStatus().toString());

        // Calculate Total Savings (sum of all account balances)
        BigDecimal totalSavings = calculateTotalSavings(memberId);
        memberDetail.setTotalSavings(totalSavings);

        // Calculate Frozen Self Guarantee Amount (self-guarantee pledges only)
        BigDecimal frozenSelfGuarantee = calculateFrozenSelfGuarantee(memberId);
        memberDetail.setFrozenSelfGuaranteeAmount(frozenSelfGuarantee);

        // Calculate Available Savings
        BigDecimal availableSavings = totalSavings.subtract(frozenSelfGuarantee);
        if (availableSavings.compareTo(BigDecimal.ZERO) < 0) {
            availableSavings = BigDecimal.ZERO;
        }
        memberDetail.setAvailableSavings(availableSavings);

        // Calculate Total Pledge Amount (as guarantor for others)
        BigDecimal totalPledgeAmount = calculateTotalPledgeAmount(memberId);
        memberDetail.setTotalPledgeAmount(totalPledgeAmount);

        // Calculate Available Guarantorship Capacity
        BigDecimal availableGuarantorshipCapacity = availableSavings.subtract(totalPledgeAmount);
        if (availableGuarantorshipCapacity.compareTo(BigDecimal.ZERO) < 0) {
            availableGuarantorshipCapacity = BigDecimal.ZERO;
        }
        memberDetail.setAvailableGuarantorshipCapacity(availableGuarantorshipCapacity);

        // Get loans they are guaranteeing
        List<GuarantorReportDTO.GuarantorLoansDetail> loansGuaranteeing = getLoansTheyAreGuaranteeing(memberId, guarantorStatus);
        memberDetail.setLoansGuaranteeing(loansGuaranteeing);
        memberDetail.setNumberOfLoansGuaranteeing(loansGuaranteeing.size());

        return new GuarantorReportDTO(memberDetail);
    }

    /**
     * Generate Guarantor Report for all members (summary view for Treasurer)
     */
    public GuarantorReportDTO generateGuarantorReportAll() {
        List<Member> allMembers = memberRepository.findAll();

        List<GuarantorReportDTO.MemberGuarantorSummary> summaries = allMembers.stream()
                .map(member -> {
                    GuarantorReportDTO.MemberGuarantorSummary summary = new GuarantorReportDTO.MemberGuarantorSummary();
                    summary.setMemberId(member.getId());
                    summary.setMemberNumber(member.getMemberNumber());
                    summary.setMemberName(member.getFirstName() + " " + member.getLastName());
                    summary.setMemberStatus(member.getStatus().toString());

                    // Calculate Available Savings
                    BigDecimal totalSavings = calculateTotalSavings(member.getId());
                    BigDecimal frozenSelfGuarantee = calculateFrozenSelfGuarantee(member.getId());
                    BigDecimal availableSavings = totalSavings.subtract(frozenSelfGuarantee);
                    if (availableSavings.compareTo(BigDecimal.ZERO) < 0) {
                        availableSavings = BigDecimal.ZERO;
                    }
                    summary.setAvailableSavings(availableSavings);

                    // Calculate Available Guarantorship Capacity
                    BigDecimal totalPledgeAmount = calculateTotalPledgeAmount(member.getId());
                    BigDecimal availableGuarantorshipCapacity = availableSavings.subtract(totalPledgeAmount);
                    if (availableGuarantorshipCapacity.compareTo(BigDecimal.ZERO) < 0) {
                        availableGuarantorshipCapacity = BigDecimal.ZERO;
                    }
                    summary.setAvailableGuarantorshipCapacity(availableGuarantorshipCapacity);

                    // Count loans they are guaranteeing
                    List<Guarantor> guarantorPledges = guarantorRepository.findByMemberIdAndSelfGuaranteeIsFalseAndStatusNotIn(
                            member.getId(),
                            Arrays.asList(Guarantor.Status.DECLINED, Guarantor.Status.REJECTED, Guarantor.Status.RELEASED)
                    );
                    summary.setNumberOfLoansGuaranteeing(guarantorPledges.size());

                    return summary;
                })
                .collect(Collectors.toList());

        return new GuarantorReportDTO(summaries);
    }

    /**
     * Calculate total savings for a member (sum of all account balances)
     */
    private BigDecimal calculateTotalSavings(Long memberId) {
        List<Account> accounts = accountRepository.findByMemberId(memberId);
        return accounts.stream()
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
     * Calculate total pledge amount (as external guarantor for others)
     * Total Pledge Amount = SUM(guarantor.pledgeAmount) WHERE guarantor_member_id = X 
     *                      AND self_guarantee = false AND status = ACTIVE
     */
    private BigDecimal calculateTotalPledgeAmount(Long memberId) {
        List<Guarantor> pledges = guarantorRepository.findByMemberIdAndSelfGuaranteeIsFalseAndStatus(
                memberId,
                Guarantor.Status.ACTIVE
        );
        return pledges.stream()
                .map(Guarantor::getPledgeAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get loans that a member is guaranteeing
     */
    private List<GuarantorReportDTO.GuarantorLoansDetail> getLoansTheyAreGuaranteeing(Long memberId, String guarantorStatusFilter) {
        // Find all guarantor pledges for this member (where they are the guarantor)
        List<Guarantor> guarantorPledges;

        if (guarantorStatusFilter != null && !guarantorStatusFilter.isEmpty()) {
            guarantorPledges = guarantorRepository.findByMemberIdAndSelfGuaranteeIsFalseAndStatus(
                    memberId,
                    Guarantor.Status.valueOf(guarantorStatusFilter)
            );
        } else {
            guarantorPledges = guarantorRepository.findByMemberIdAndSelfGuaranteeIsFalse(memberId);
        }

        return guarantorPledges.stream()
                .map(guarantor -> {
                    Loan loan = guarantor.getLoan();
                    GuarantorReportDTO.GuarantorLoansDetail detail = new GuarantorReportDTO.GuarantorLoansDetail();
                    detail.setLoanId(loan.getId());
                    detail.setLoanNumber(loan.getLoanNumber());
                    detail.setBorrowerName(loan.getMember().getFirstName() + " " + loan.getMember().getLastName());
                    detail.setLoanAmount(loan.getOriginalPrincipal() != null ? loan.getOriginalPrincipal() : loan.getAmount());
                    detail.setOutstandingBalance(loan.getOutstandingBalance());

                    // Calculate repayment progress using originalPrincipal
                    BigDecimal principal = loan.getOriginalPrincipal() != null ? loan.getOriginalPrincipal() : loan.getAmount();
                    if (principal != null && principal.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal repaid = principal.subtract(loan.getOutstandingBalance() != null ? loan.getOutstandingBalance() : BigDecimal.ZERO);
                        BigDecimal progress = repaid.multiply(new BigDecimal(100)).divide(principal, 2, java.math.RoundingMode.HALF_UP);
                        detail.setRepaymentProgress(progress);
                    } else {
                        detail.setRepaymentProgress(BigDecimal.ZERO);
                    }

                    detail.setGuarantorPledgeAmount(guarantor.getPledgeAmount());
                    detail.setStatus(guarantor.getStatus().toString());

                    return detail;
                })
                .collect(Collectors.toList());
    }
}
