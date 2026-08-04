package com.minet.sacco.service;

import com.minet.sacco.dto.GuarantorReportDTO;
import com.minet.sacco.dto.OverCommittedGuarantorDTO;
import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GuarantorReportService {

    private static final Logger log = LoggerFactory.getLogger(GuarantorReportService.class);

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
        memberDetail.setMemberName(member.getFullName());
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
     * Generate Over-Committed Guarantor Report - OPTIMIZED VERSION
     * Uses native SQL query to calculate everything in one database call
     * Much faster than the N+1 query approach
     */
    public OverCommittedGuarantorDTO generateOverCommittedGuarantorReport() {
        log.info("Generating over-committed guarantor report...");
        
        // Get all members with their calculated values in ONE query
        List<Object[]> results = memberRepository.findOverCommittedGuarantors();
        
        List<OverCommittedGuarantorDTO.OverCommittedGuarantorDetail> overCommittedList = new ArrayList<>();
        
        for (Object[] row : results) {
            Long memberId = ((Number) row[0]).longValue();
            String memberNumber = (String) row[1];
            String memberName = (String) row[2];
            String memberStatus = (String) row[3];
            BigDecimal totalSavings = (BigDecimal) row[4];
            BigDecimal frozenSelfGuarantee = (BigDecimal) row[5];
            BigDecimal frozenPledges = (BigDecimal) row[6];
            BigDecimal availableSavings = (BigDecimal) row[7];
            BigDecimal amountOverCommitted = (BigDecimal) row[8];
            
            // Only include if actually over-committed
            if (amountOverCommitted.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            
            OverCommittedGuarantorDTO.OverCommittedGuarantorDetail detail = new OverCommittedGuarantorDTO.OverCommittedGuarantorDetail();
            detail.setMemberId(memberId);
            detail.setMemberNumber(memberNumber);
            detail.setMemberName(memberName);
            detail.setMemberStatus(memberStatus);
            detail.setTotalSavings(totalSavings);
            detail.setFrozenSelfGuarantee(frozenSelfGuarantee);
            detail.setFrozenPledges(frozenPledges);
            detail.setTotalFrozen(frozenSelfGuarantee.add(frozenPledges));
            detail.setAvailableSavings(availableSavings);
            detail.setAmountOverCommitted(amountOverCommitted);
            
            // Get risky guarantees for this member
            List<Guarantor> activePledges = guarantorRepository.findByMemberIdAndSelfGuaranteeIsFalseAndStatus(
                    memberId,
                    Guarantor.Status.ACTIVE
            );
            detail.setNumberOfLoansGuaranteeing(activePledges.size());
            detail.setRiskyGuarantees(activePledges.stream()
                    .map(guarantor -> {
                        Loan loan = guarantor.getLoan();
                        OverCommittedGuarantorDTO.RiskyGuaranteeDetail riskyDetail = new OverCommittedGuarantorDTO.RiskyGuaranteeDetail();
                        riskyDetail.setLoanId(loan.getId());
                        riskyDetail.setLoanNumber(loan.getLoanNumber());
                        riskyDetail.setBorrowerName(loan.getMember().getFirstName() + " " + loan.getMember().getLastName());
                        riskyDetail.setLoanAmount(loan.getOriginalPrincipal() != null ? loan.getOriginalPrincipal() : loan.getAmount());
                        riskyDetail.setOutstandingBalance(loan.getOutstandingBalance());
                        riskyDetail.setGuarantorPledgeAmount(guarantor.getGuaranteeAmount());
                        riskyDetail.setCurrentFrozenPledge(guarantor.getPledgeAmount());
                        riskyDetail.setGuarantorStatus(guarantor.getStatus().toString());
                        return riskyDetail;
                    })
                    .collect(Collectors.toList()));
            
            overCommittedList.add(detail);
        }
        
        // Sort by risk (highest first)
        overCommittedList.sort((a, b) -> b.getAmountOverCommitted().compareTo(a.getAmountOverCommitted()));
        
        log.info("Found {} over-committed guarantors", overCommittedList.size());
        return new OverCommittedGuarantorDTO(overCommittedList);
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
                    summary.setMemberName(member.getFullName());
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
        // SAVINGS-only accounts (not SHARES) to align with loan eligibility rules
        try {
            List<Account> savingsAccounts = accountRepository.findSavingsAccountsByMemberId(memberId);
            if (savingsAccounts == null || savingsAccounts.isEmpty()) {
                return BigDecimal.ZERO;
            }
            return savingsAccounts.stream()
                    .map(Account::getBalance)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            log.warn("Error calculating total savings for member {}: {}", memberId, e.getMessage());
            return BigDecimal.ZERO;
        }
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
