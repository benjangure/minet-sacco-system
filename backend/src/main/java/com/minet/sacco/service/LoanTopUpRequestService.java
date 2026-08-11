package com.minet.sacco.service;

import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for handling member-initiated loan top-up requests.
 * Mirrors the full loan approval pipeline:
 *   PENDING_GUARANTOR_APPROVAL → PENDING_LOAN_OFFICER_REVIEW →
 *   PENDING_CREDIT_COMMITTEE → PENDING_TREASURER → APPROVED → DISBURSED
 */
@Service
public class LoanTopUpRequestService {

    @Autowired private LoanTopUpRequestRepository topUpRequestRepository;
    @Autowired private TopUpGuarantorRepository topUpGuarantorRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private NotificationService notificationService;
    @Autowired private RealtimeNotificationService realtimeNotificationService;
    @Autowired private AuditService auditService;
    @Autowired private UserService userService;
    @Autowired private LoanService loanService;
    @Autowired private LoanTopUpHistoryRepository topUpHistoryRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // MEMBER ACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    /** Member creates a top-up request with guarantors. */
    @Transactional
    public LoanTopUpRequest createTopUpRequest(
            Long loanId, Long memberId, BigDecimal requestedAmount,
            String purpose, List<GuarantorAssignment> guarantorAssignments) {

        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found"));
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new RuntimeException("Member not found"));

        if (!loan.getMember().getId().equals(memberId))
            throw new RuntimeException("Loan does not belong to this member");
        if (loan.getStatus() != Loan.Status.DISBURSED)
            throw new RuntimeException("Only disbursed loans can be topped up");

        // Block if a live top-up already exists for this loan
        long pendingCount =
            topUpRequestRepository.countByLoanIdAndStatus(loanId, LoanTopUpRequest.Status.PENDING_GUARANTOR_APPROVAL)
          + topUpRequestRepository.countByLoanIdAndStatus(loanId, LoanTopUpRequest.Status.PENDING_LOAN_OFFICER_REVIEW)
          + topUpRequestRepository.countByLoanIdAndStatus(loanId, LoanTopUpRequest.Status.PENDING_CREDIT_COMMITTEE)
          + topUpRequestRepository.countByLoanIdAndStatus(loanId, LoanTopUpRequest.Status.PENDING_TREASURER)
          + topUpRequestRepository.countByLoanIdAndStatus(loanId, LoanTopUpRequest.Status.APPROVED);
        if (pendingCount > 0)
            throw new RuntimeException("There is already a pending top-up request for this loan");

        if (requestedAmount.compareTo(BigDecimal.ZERO) <= 0)
            throw new RuntimeException("Top-up amount must be greater than 0");
        if (guarantorAssignments == null || guarantorAssignments.isEmpty())
            throw new RuntimeException("At least one guarantor is required");

        // Validate 100 % coverage
        BigDecimal totalGuarantee = guarantorAssignments.stream()
            .map(GuarantorAssignment::getGuaranteeAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal coverage = totalGuarantee.multiply(BigDecimal.valueOf(100))
            .divide(requestedAmount, 2, java.math.RoundingMode.HALF_UP);
        if (coverage.compareTo(BigDecimal.valueOf(100)) < 0)
            throw new RuntimeException(String.format(
                "Total guarantee coverage (%.2f%%) must be at least 100%%", coverage.doubleValue()));

        LoanTopUpRequest topUpRequest = new LoanTopUpRequest(loan, member, requestedAmount, purpose);
        topUpRequest = topUpRequestRepository.save(topUpRequest);

        for (GuarantorAssignment assignment : guarantorAssignments) {
            Member gMember = memberRepository.findByMemberNumber(assignment.getMemberNumber())
                .orElseThrow(() -> new RuntimeException("Guarantor not found: " + assignment.getMemberNumber()));
            if (gMember.getId().equals(memberId))
                throw new RuntimeException("Member cannot be their own guarantor for top-ups");
            if (gMember.getStatus() != Member.Status.ACTIVE)
                throw new RuntimeException("Guarantor " + assignment.getMemberNumber() + " is not active");

            Optional<Account> savingsOpt = accountRepository.findByMemberIdAndAccountType(
                gMember.getId(), Account.AccountType.SAVINGS);
            if (savingsOpt.isEmpty())
                throw new RuntimeException("Guarantor " + assignment.getMemberNumber() + " has no savings account");

            Account savings = savingsOpt.get();
            BigDecimal available = savings.getBalance().subtract(
                savings.getFrozenSavings() != null ? savings.getFrozenSavings() : BigDecimal.ZERO);
            if (available.compareTo(assignment.getGuaranteeAmount()) < 0)
                throw new RuntimeException(String.format(
                    "Guarantor %s has insufficient available savings. Required: KES %s, Available: KES %s",
                    assignment.getMemberNumber(), assignment.getGuaranteeAmount(), available));

            TopUpGuarantor guarantor = new TopUpGuarantor(topUpRequest, gMember, assignment.getGuaranteeAmount());
            topUpRequest.addGuarantor(guarantor);
            topUpGuarantorRepository.save(guarantor);

            Optional<User> gUser = userService.getUserByMemberId(gMember.getId());
            if (gUser.isPresent()) {
                notificationService.notifyUser(gUser.get().getId(),
                    String.format("%s %s has requested you to guarantee their loan top-up of KES %s. Please review and approve/reject.",
                        member.getFirstName(), member.getLastName(), requestedAmount),
                    "GUARANTOR_REQUEST");
                
                // Real-time notification to guarantor
                realtimeNotificationService.notifyTopUpGuarantorRequest(
                    gMember.getId(),
                    topUpRequest.getId(),
                    loan.getId(),
                    loan.getLoanNumber(),
                    member.getFirstName() + " " + member.getLastName(),
                    assignment.getGuaranteeAmount().doubleValue()
                );
            }
        }

        topUpRequest = topUpRequestRepository.save(topUpRequest);
        
        // Real-time notification: Top-up request created
        realtimeNotificationService.notifyTopUpRequestCreated(
            topUpRequest.getId(),
            loan.getId(),
            loan.getLoanNumber(),
            member.getId(),
            requestedAmount.doubleValue()
        );
        
        auditService.logAction(null, "TOPUP_REQUEST_CREATED", "LoanTopUpRequest", topUpRequest.getId(),
            String.format("Member %s %s requested top-up of KES %s for loan %s",
                member.getFirstName(), member.getLastName(), requestedAmount, loan.getLoanNumber()),
            purpose, "SUCCESS");
        return topUpRequest;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GUARANTOR ACTIONS
    // ─────────────────────────────────────────────────────────────────────────

    /** Guarantor approves their share of a top-up guarantee. */
    @Transactional
    public void approveTopUpGuarantee(Long topUpRequestId, Long guarantorMemberId) {
        TopUpGuarantor guarantor = topUpGuarantorRepository
            .findByTopUpRequestIdAndMemberId(topUpRequestId, guarantorMemberId)
            .orElseThrow(() -> new RuntimeException("Guarantor record not found"));

        if (guarantor.getStatus() != TopUpGuarantor.Status.PENDING)
            throw new RuntimeException("Guarantor request has already been processed");

        guarantor.approve();
        topUpGuarantorRepository.save(guarantor);

        LoanTopUpRequest topUpRequest = topUpRequestRepository.findById(topUpRequestId)
            .orElseThrow(() -> new RuntimeException("Top-up request not found"));
        
        String oldStatus = topUpRequest.getStatus().name();
        topUpRequest.recalculateGuaranteeAmounts();
        topUpRequestRepository.save(topUpRequest);
        
        // Real-time notification: status change if it happened
        if (!oldStatus.equals(topUpRequest.getStatus().name())) {
            realtimeNotificationService.notifyTopUpStatusChanged(
                topUpRequest.getId(),
                topUpRequest.getLoan().getId(),
                topUpRequest.getLoan().getLoanNumber(),
                topUpRequest.getMember().getId(),
                oldStatus,
                topUpRequest.getStatus().name()
            );
        }

        // If transition to PENDING_LOAN_OFFICER_REVIEW happened, notify loan officers
        if (topUpRequest.getStatus() == LoanTopUpRequest.Status.PENDING_LOAN_OFFICER_REVIEW) {
            List<User> officers = userService.getUsersByRole("LOAN_OFFICER");
            for (User officer : officers) {
                notificationService.notifyUser(officer.getId(),
                    String.format("Top-up request for loan %s (KES %s) has all guarantors approved and is ready for your review.",
                        topUpRequest.getLoan().getLoanNumber(), topUpRequest.getRequestedAmount()),
                    "TOPUP_REVIEW_NEEDED");
            }
        }

        // Notify borrower
        notifyMember(topUpRequest.getMember().getId(),
            String.format("%s %s has approved your top-up guarantee request of KES %s",
                guarantor.getMember().getFirstName(), guarantor.getMember().getLastName(),
                topUpRequest.getRequestedAmount()),
            "GUARANTOR_APPROVED");

        auditService.logAction(null, "TOPUP_GUARANTOR_APPROVED", "TopUpGuarantor", guarantor.getId(),
            String.format("Guarantor %s %s approved top-up request #%d",
                guarantor.getMember().getFirstName(), guarantor.getMember().getLastName(), topUpRequestId),
            null, "SUCCESS");
    }

    /** Guarantor rejects their share of a top-up guarantee. */
    @Transactional
    public void rejectTopUpGuarantee(Long topUpRequestId, Long guarantorMemberId, String reason) {
        TopUpGuarantor guarantor = topUpGuarantorRepository
            .findByTopUpRequestIdAndMemberId(topUpRequestId, guarantorMemberId)
            .orElseThrow(() -> new RuntimeException("Guarantor record not found"));

        if (guarantor.getStatus() != TopUpGuarantor.Status.PENDING)
            throw new RuntimeException("Guarantor request has already been processed");

        guarantor.reject(reason);
        topUpGuarantorRepository.save(guarantor);

        LoanTopUpRequest topUpRequest = topUpRequestRepository.findById(topUpRequestId)
            .orElseThrow(() -> new RuntimeException("Top-up request not found"));
        topUpRequest.recalculateGuaranteeAmounts();
        topUpRequestRepository.save(topUpRequest);

        notifyMember(topUpRequest.getMember().getId(),
            String.format("%s %s has rejected your top-up guarantee request. Reason: %s",
                guarantor.getMember().getFirstName(), guarantor.getMember().getLastName(), reason),
            "GUARANTOR_REJECTED");

        auditService.logAction(null, "TOPUP_GUARANTOR_REJECTED", "TopUpGuarantor", guarantor.getId(),
            String.format("Guarantor %s %s rejected top-up request #%d. Reason: %s",
                guarantor.getMember().getFirstName(), guarantor.getMember().getLastName(), topUpRequestId, reason),
            null, "SUCCESS");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // STAFF APPROVAL PIPELINE  (mirrors Loan: LO → CC → Treasurer → Disburse)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loan Officer approves → PENDING_CREDIT_COMMITTEE
     * Loan Officer rejects → REJECTED (final; borrower notified)
     */
    @Transactional
    public void loanOfficerDecision(Long topUpRequestId, User officer, boolean approved, String comments) {
        LoanTopUpRequest req = findAndValidate(topUpRequestId,
            LoanTopUpRequest.Status.PENDING_LOAN_OFFICER_REVIEW, "LOAN_OFFICER");

        if (approved) {
            req.setStatus(LoanTopUpRequest.Status.PENDING_CREDIT_COMMITTEE);
            req.setReviewedBy(officer);
            req.setReviewDate(LocalDateTime.now());
            topUpRequestRepository.save(req);

            // Real-time notification: status changed
            realtimeNotificationService.notifyTopUpStatusChanged(
                req.getId(),
                req.getLoan().getId(),
                req.getLoan().getLoanNumber(),
                req.getMember().getId(),
                "PENDING_LOAN_OFFICER_REVIEW",
                "PENDING_CREDIT_COMMITTEE"
            );

            notifyRole("CREDIT_COMMITTEE",
                String.format("Top-up request for loan %s (KES %s) approved by Loan Officer. Please review.",
                    req.getLoan().getLoanNumber(), req.getRequestedAmount()),
                "TOPUP_CC_REVIEW");

            auditDecision(officer, "TOPUP_LO_APPROVED", req, comments);
        } else {
            rejectFinal(req, officer, comments, "Rejected by Loan Officer");
        }
    }

    /**
     * Credit Committee approves → PENDING_TREASURER
     * Credit Committee rejects → PENDING_LOAN_OFFICER_REVIEW (send back)
     */
    @Transactional
    public void creditCommitteeDecision(Long topUpRequestId, User member, boolean approved, String comments) {
        LoanTopUpRequest req = findAndValidate(topUpRequestId,
            LoanTopUpRequest.Status.PENDING_CREDIT_COMMITTEE, "CREDIT_COMMITTEE");

        if (approved) {
            req.setStatus(LoanTopUpRequest.Status.PENDING_TREASURER);
            req.setReviewedBy(member);
            req.setReviewDate(LocalDateTime.now());
            topUpRequestRepository.save(req);

            // Real-time notification
            realtimeNotificationService.notifyTopUpStatusChanged(
                req.getId(),
                req.getLoan().getId(),
                req.getLoan().getLoanNumber(),
                req.getMember().getId(),
                "PENDING_CREDIT_COMMITTEE",
                "PENDING_TREASURER"
            );

            notifyRole("TREASURER",
                String.format("Top-up request for loan %s (KES %s) approved by Credit Committee. Please review.",
                    req.getLoan().getLoanNumber(), req.getRequestedAmount()),
                "TOPUP_TREASURER_REVIEW");

            auditDecision(member, "TOPUP_CC_APPROVED", req, comments);
        } else {
            // Send back to Loan Officer
            req.setStatus(LoanTopUpRequest.Status.PENDING_LOAN_OFFICER_REVIEW);
            req.setRejectionReason(comments);
            topUpRequestRepository.save(req);

            // Real-time notification
            realtimeNotificationService.notifyTopUpStatusChanged(
                req.getId(),
                req.getLoan().getId(),
                req.getLoan().getLoanNumber(),
                req.getMember().getId(),
                "PENDING_CREDIT_COMMITTEE",
                "PENDING_LOAN_OFFICER_REVIEW"
            );

            notifyRole("LOAN_OFFICER",
                String.format("Top-up request for loan %s was returned by Credit Committee. Reason: %s",
                    req.getLoan().getLoanNumber(), comments),
                "TOPUP_RETURNED");

            auditDecision(member, "TOPUP_CC_REJECTED", req, comments);
        }
    }

    /**
     * Treasurer approves → APPROVED (ready for disbursement)
     * Treasurer rejects → PENDING_CREDIT_COMMITTEE (send back)
     */
    @Transactional
    public void treasurerDecision(Long topUpRequestId, User treasurer, boolean approved, String comments) {
        LoanTopUpRequest req = findAndValidate(topUpRequestId,
            LoanTopUpRequest.Status.PENDING_TREASURER, "TREASURER");

        if (approved) {
            req.setStatus(LoanTopUpRequest.Status.APPROVED);
            req.setReviewedBy(treasurer);
            req.setReviewDate(LocalDateTime.now());
            topUpRequestRepository.save(req);

            // Real-time notification
            realtimeNotificationService.notifyTopUpStatusChanged(
                req.getId(),
                req.getLoan().getId(),
                req.getLoan().getLoanNumber(),
                req.getMember().getId(),
                "PENDING_TREASURER",
                "APPROVED"
            );

            notifyMember(req.getMember().getId(),
                String.format("Your top-up request of KES %s for loan %s has been approved and is ready for disbursement.",
                    req.getRequestedAmount(), req.getLoan().getLoanNumber()),
                "TOPUP_APPROVED");

            auditDecision(treasurer, "TOPUP_TREASURER_APPROVED", req, comments);
        } else {
            // Send back to Credit Committee
            req.setStatus(LoanTopUpRequest.Status.PENDING_CREDIT_COMMITTEE);
            req.setRejectionReason(comments);
            topUpRequestRepository.save(req);

            // Real-time notification
            realtimeNotificationService.notifyTopUpStatusChanged(
                req.getId(),
                req.getLoan().getId(),
                req.getLoan().getLoanNumber(),
                req.getMember().getId(),
                "PENDING_TREASURER",
                "PENDING_CREDIT_COMMITTEE"
            );

            notifyRole("CREDIT_COMMITTEE",
                String.format("Top-up request for loan %s was returned by Treasurer. Reason: %s",
                    req.getLoan().getLoanNumber(), comments),
                "TOPUP_RETURNED");

            auditDecision(treasurer, "TOPUP_TREASURER_REJECTED", req, comments);
        }
    }

    /**
     * Treasurer disburses an APPROVED top-up → DISBURSED.
     * Updates the loan outstanding balance and freezes guarantor savings.
     */
    @Transactional
    public LoanTopUpRequest disburseTopUp(Long topUpRequestId, User disbursedBy) {
        LoanTopUpRequest req = topUpRequestRepository.findById(topUpRequestId)
            .orElseThrow(() -> new RuntimeException("Top-up request not found"));

        if (req.getStatus() != LoanTopUpRequest.Status.APPROVED)
            throw new RuntimeException("Only APPROVED top-up requests can be disbursed. Current status: " + req.getStatus());

        Loan loan = req.getLoan();
        if (loan.getStatus() != Loan.Status.DISBURSED)
            throw new RuntimeException("Loan must be in DISBURSED status to receive a top-up");

        BigDecimal outstandingBefore = loan.getOutstandingBalance();
        BigDecimal outstandingAfter  = outstandingBefore.add(req.getRequestedAmount());

        loan.setOutstandingBalance(outstandingAfter);
        loan.setAmount(outstandingAfter);

        BigDecimal currentTotal = loan.getTotalTopupAmount() != null ? loan.getTotalTopupAmount() : BigDecimal.ZERO;
        loan.setTotalTopupAmount(currentTotal.add(req.getRequestedAmount()));
        loan.setTopupCount((loan.getTopupCount() != null ? loan.getTopupCount() : 0) + 1);
        loan.setLastTopupDate(LocalDateTime.now());
        loan.calculateRepaymentDetails();
        loanRepository.save(loan);

        // Freeze guarantor savings
        for (TopUpGuarantor g : req.getGuarantors()) {
            if (g.getStatus() == TopUpGuarantor.Status.APPROVED) {
                freezeGuarantorSavings(g.getMember(), g.getGuaranteeAmount());
            }
        }

        req.setStatus(LoanTopUpRequest.Status.DISBURSED);
        req.setDisbursedBy(disbursedBy);
        req.setDisbursementDate(LocalDateTime.now());
        req = topUpRequestRepository.save(req);

        // Real-time notification: Top-up disbursed
        realtimeNotificationService.notifyTopUpStatusChanged(
            req.getId(),
            loan.getId(),
            loan.getLoanNumber(),
            req.getMember().getId(),
            "APPROVED",
            "DISBURSED"
        );
        
        realtimeNotificationService.notifyTopUpDisbursed(
            req.getMember().getId(),
            req.getId(),
            loan.getId(),
            loan.getLoanNumber(),
            req.getRequestedAmount().doubleValue(),
            outstandingAfter.doubleValue()
        );

        // History record
        com.minet.sacco.entity.LoanTopUpHistory history = new com.minet.sacco.entity.LoanTopUpHistory(
            loan, req.getRequestedAmount(), outstandingBefore, outstandingAfter,
            loan.getOriginalPrincipal().subtract(outstandingBefore),
            req.getGuarantors().size(), disbursedBy, req.getPurpose());
        topUpHistoryRepository.save(history);

        // Notify member
        notifyMember(req.getMember().getId(),
            String.format("Your top-up of KES %s has been disbursed. New outstanding balance: KES %s",
                req.getRequestedAmount(), outstandingAfter),
            "TOPUP_DISBURSED");

        // Notify guarantors
        for (TopUpGuarantor g : req.getGuarantors()) {
            notifyMember(g.getMember().getId(),
                String.format("The top-up you guaranteed for %s %s (KES %s) has been disbursed.",
                    req.getMember().getFirstName(), req.getMember().getLastName(), req.getRequestedAmount()),
                "TOPUP_DISBURSED");
        }

        auditService.logAction(disbursedBy, "TOPUP_DISBURSED", "LoanTopUpRequest", req.getId(),
            String.format("%s disbursed top-up #%d for loan %s. Amount: KES %s",
                disbursedBy.getUsername(), topUpRequestId, loan.getLoanNumber(), req.getRequestedAmount()),
            req.getPurpose(), "SUCCESS");

        return req;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LEGACY / COMPATIBILITY  (keep old single-step approve+disburse for any
    // existing callers; internally delegates to the new pipeline)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * @deprecated Use {@link #treasurerDecision} + {@link #disburseTopUp} instead.
     */
    @Deprecated
    @Transactional
    public LoanTopUpRequest approveAndDisburseTopUpRequest(Long topUpRequestId, User approver) {
        LoanTopUpRequest req = topUpRequestRepository.findById(topUpRequestId)
            .orElseThrow(() -> new RuntimeException("Top-up request not found"));

        // Accept from any pre-disburse status to keep old callers working
        if (req.getStatus() == LoanTopUpRequest.Status.PENDING_LOAN_OFFICER_REVIEW
         || req.getStatus() == LoanTopUpRequest.Status.PENDING_REVIEW) { // legacy
            req.setStatus(LoanTopUpRequest.Status.PENDING_CREDIT_COMMITTEE);
            topUpRequestRepository.save(req);
        }
        if (req.getStatus() == LoanTopUpRequest.Status.PENDING_CREDIT_COMMITTEE) {
            req.setStatus(LoanTopUpRequest.Status.PENDING_TREASURER);
            topUpRequestRepository.save(req);
        }
        if (req.getStatus() == LoanTopUpRequest.Status.PENDING_TREASURER) {
            req.setStatus(LoanTopUpRequest.Status.APPROVED);
            req.setReviewedBy(approver);
            req.setReviewDate(LocalDateTime.now());
            topUpRequestRepository.save(req);
        }
        return disburseTopUp(topUpRequestId, approver);
    }

    /** Reject a top-up request at any staff-review stage. */
    @Transactional
    public void rejectTopUpRequest(Long topUpRequestId, User reviewer, String reason) {
        LoanTopUpRequest req = topUpRequestRepository.findById(topUpRequestId)
            .orElseThrow(() -> new RuntimeException("Top-up request not found"));

        boolean isReviewable =
            req.getStatus() == LoanTopUpRequest.Status.PENDING_LOAN_OFFICER_REVIEW ||
            req.getStatus() == LoanTopUpRequest.Status.PENDING_CREDIT_COMMITTEE    ||
            req.getStatus() == LoanTopUpRequest.Status.PENDING_TREASURER           ||
            req.getStatus() == LoanTopUpRequest.Status.PENDING_REVIEW; // legacy

        if (!isReviewable)
            throw new RuntimeException("Top-up request cannot be rejected in status: " + req.getStatus());

        rejectFinal(req, reviewer, reason, "Rejected by staff");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MEMBER CANCEL
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void cancelTopUpRequest(Long topUpRequestId, Long memberId) {
        LoanTopUpRequest req = topUpRequestRepository.findById(topUpRequestId)
            .orElseThrow(() -> new RuntimeException("Top-up request not found"));

        if (!req.getMember().getId().equals(memberId))
            throw new RuntimeException("You can only cancel your own top-up requests");
        if (!req.canBeCancelled())
            throw new RuntimeException("This top-up request cannot be cancelled in status: " + req.getStatus());

        req.setStatus(LoanTopUpRequest.Status.CANCELLED);
        req.setRejectionReason("Cancelled by member");
        topUpRequestRepository.save(req);

        // Notify pending guarantors
        for (TopUpGuarantor g : topUpGuarantorRepository.findByTopUpRequestId(topUpRequestId)) {
            if (g.getStatus() == TopUpGuarantor.Status.PENDING) {
                notifyMember(g.getMember().getId(),
                    String.format("%s %s has cancelled their top-up request.",
                        req.getMember().getFirstName(), req.getMember().getLastName()),
                    "TOPUP_CANCELLED");
            }
        }

        auditService.logAction(null, "TOPUP_CANCELLED", "LoanTopUpRequest", topUpRequestId,
            "Member cancelled top-up request #" + topUpRequestId, null, "SUCCESS");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUERY METHODS
    // ─────────────────────────────────────────────────────────────────────────

    public List<LoanTopUpRequest> getTopUpRequestsByLoan(Long loanId) {
        return topUpRequestRepository.findByLoanId(loanId);
    }

    public List<LoanTopUpRequest> getTopUpRequestsByMember(Long memberId) {
        return topUpRequestRepository.findByMemberId(memberId);
    }

    public List<TopUpGuarantor> getPendingGuarantorApprovals(Long memberId) {
        return topUpGuarantorRepository.findByMemberIdAndStatus(memberId, TopUpGuarantor.Status.PENDING);
    }

    public Optional<LoanTopUpRequest> getTopUpRequestById(Long id) {
        return topUpRequestRepository.findById(id);
    }

    /** Returns requests visible to the Loan Officer queue. */
    public List<LoanTopUpRequest> getPendingTopUpRequestsForLoanOfficer() {
        return topUpRequestRepository.findByStatus(LoanTopUpRequest.Status.PENDING_LOAN_OFFICER_REVIEW);
    }

    /** Returns requests visible to the Credit Committee queue. */
    public List<LoanTopUpRequest> getPendingTopUpRequestsForCreditCommittee() {
        return topUpRequestRepository.findByStatus(LoanTopUpRequest.Status.PENDING_CREDIT_COMMITTEE);
    }

    /** Returns requests visible to the Treasurer queue. */
    public List<LoanTopUpRequest> getPendingTopUpRequestsForTreasurer() {
        return topUpRequestRepository.findByStatus(LoanTopUpRequest.Status.PENDING_TREASURER);
    }

    /** Returns APPROVED requests ready for disbursement (Treasurer only). */
    public List<LoanTopUpRequest> getApprovedTopUpRequests() {
        return topUpRequestRepository.findByStatus(LoanTopUpRequest.Status.APPROVED);
    }

    /**
     * Legacy: previously returned only PENDING_REVIEW; now returns
     * PENDING_LOAN_OFFICER_REVIEW so the existing admin endpoint still works.
     */
    public List<LoanTopUpRequest> getPendingTopUpRequests() {
        return topUpRequestRepository.findByStatus(LoanTopUpRequest.Status.PENDING_LOAN_OFFICER_REVIEW);
    }

    public List<LoanTopUpRequest> getTopUpRequestsByStatus(List<LoanTopUpRequest.Status> statuses) {
        return topUpRequestRepository.findByStatusIn(statuses);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private LoanTopUpRequest findAndValidate(Long id, LoanTopUpRequest.Status expectedStatus, String roleName) {
        LoanTopUpRequest req = topUpRequestRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Top-up request not found"));
        if (req.getStatus() != expectedStatus)
            throw new RuntimeException(
                "Only " + roleName + " can act on requests in " + expectedStatus + " status. Current: " + req.getStatus());
        return req;
    }

    private void rejectFinal(LoanTopUpRequest req, User reviewer, String reason, String context) {
        req.setStatus(LoanTopUpRequest.Status.REJECTED);
        req.setReviewedBy(reviewer);
        req.setReviewDate(LocalDateTime.now());
        req.setRejectionReason(reason);
        topUpRequestRepository.save(req);

        notifyMember(req.getMember().getId(),
            String.format("Your top-up request of KES %s has been rejected. Reason: %s",
                req.getRequestedAmount(), reason),
            "TOPUP_REJECTED");

        // Notify guarantors who already approved
        for (TopUpGuarantor g : req.getGuarantors()) {
            if (g.getStatus() == TopUpGuarantor.Status.APPROVED) {
                notifyMember(g.getMember().getId(),
                    String.format("The top-up you guaranteed for %s %s has been rejected by staff.",
                        req.getMember().getFirstName(), req.getMember().getLastName()),
                    "TOPUP_REJECTED");
            }
        }

        auditService.logAction(reviewer, "TOPUP_REJECTED", "LoanTopUpRequest", req.getId(),
            String.format("%s rejected top-up #%d. Reason: %s", reviewer.getUsername(), req.getId(), reason),
            null, "SUCCESS");
    }

    private void freezeGuarantorSavings(Member member, BigDecimal amount) {
        accountRepository.findByMemberIdAndAccountType(member.getId(), Account.AccountType.SAVINGS)
            .ifPresent(account -> {
                BigDecimal current = account.getFrozenSavings() != null ? account.getFrozenSavings() : BigDecimal.ZERO;
                account.setFrozenSavings(current.add(amount));
                accountRepository.save(account);
            });
    }

    private void notifyMember(Long memberId, String message, String type) {
        userService.getUserByMemberId(memberId)
            .ifPresent(u -> notificationService.notifyUser(u.getId(), message, type));
    }

    private void notifyRole(String role, String message, String type) {
        for (User u : userService.getUsersByRole(role)) {
            notificationService.notifyUser(u.getId(), message, type);
        }
    }

    private void auditDecision(User actor, String action, LoanTopUpRequest req, String comments) {
        auditService.logAction(actor, action, "LoanTopUpRequest", req.getId(),
            String.format("%s acted on top-up #%d for loan %s",
                actor.getUsername(), req.getId(), req.getLoan().getLoanNumber()),
            comments, "SUCCESS");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DTO
    // ─────────────────────────────────────────────────────────────────────────

    public static class GuarantorAssignment {
        private String memberNumber;
        private BigDecimal guaranteeAmount;

        public GuarantorAssignment() {}
        public GuarantorAssignment(String memberNumber, BigDecimal guaranteeAmount) {
            this.memberNumber = memberNumber;
            this.guaranteeAmount = guaranteeAmount;
        }
        public String getMemberNumber() { return memberNumber; }
        public void setMemberNumber(String memberNumber) { this.memberNumber = memberNumber; }
        public BigDecimal getGuaranteeAmount() { return guaranteeAmount; }
        public void setGuaranteeAmount(BigDecimal guaranteeAmount) { this.guaranteeAmount = guaranteeAmount; }
    }
}
