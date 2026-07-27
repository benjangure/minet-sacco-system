package com.minet.sacco.service;

import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.MemberReactivation;
import com.minet.sacco.entity.MemberSuspension;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberReactivationRepository;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.MemberSuspensionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MemberReactivationService {

    @Autowired
    private MemberReactivationRepository memberReactivationRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberSuspensionRepository memberSuspensionRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Initiate member reactivation (Credit Committee)
     */
    @Transactional
    public MemberReactivation initiateReactivation(Long memberId, String reason, User initiatedBy) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Check if member is suspended
        Optional<MemberSuspension> suspension = memberSuspensionRepository.findByMemberIdAndIsActiveTrue(memberId);
        if (suspension.isEmpty()) {
            throw new RuntimeException("Member is not suspended");
        }

        // Check if reactivation already initiated
        Optional<MemberReactivation> existing = memberReactivationRepository.findByMemberIdAndIsActiveTrue(memberId);
        if (existing.isPresent()) {
            throw new RuntimeException("Reactivation already initiated for this member");
        }

        MemberReactivation reactivation = new MemberReactivation();
        reactivation.setMember(member);
        reactivation.setReason(reason);
        reactivation.setInitiatedBy(initiatedBy);
        reactivation.setIsActive(false);
        reactivation.setStatus("PENDING");

        MemberReactivation saved = memberReactivationRepository.save(reactivation);

        auditService.logAction(initiatedBy, "MEMBER_REACTIVATION_INITIATED",
                "Member", memberId,
                "Member: " + member.getEmployeeId() + ", Reason: " + reason,
                "Credit Committee (acting as HR) initiated reactivation - pending Treasurer approval", "SUCCESS");

        // Notify Treasurers about pending reactivation
        notificationService.notifyUsersByRole("TREASURER",
                "Member reactivation pending approval: " + member.getFirstName() + " " + member.getLastName() + " (" + member.getEmployeeId() + ")",
                "REACTIVATION_PENDING",
                null,
                memberId,
                "MEMBER_REACTIVATION");

        return saved;
    }

    /**
     * Validate reactivation (Treasurer approval)
     */
    @Transactional
    public MemberReactivation validateReactivation(Long reactivationId, String validationNotes, User validatedBy) {
        MemberReactivation reactivation = memberReactivationRepository.findById(reactivationId)
                .orElseThrow(() -> new RuntimeException("Reactivation not found"));

        // Add validation information and approve reactivation
        reactivation.setValidationNotes(validationNotes);
        reactivation.setValidatedBy(validatedBy);
        reactivation.setValidatedAt(LocalDateTime.now());
        reactivation.setStatus("APPROVED");
        reactivation.setIsActive(true);

        // Lift the suspension
        Member member = reactivation.getMember();
        Optional<MemberSuspension> suspension = memberSuspensionRepository.findByMemberIdAndIsActiveTrue(member.getId());
        if (suspension.isPresent()) {
            MemberSuspension s = suspension.get();
            s.setIsActive(false);
            s.setLiftedBy(validatedBy);
            s.setLiftedAt(LocalDateTime.now());
            memberSuspensionRepository.save(s);
        }

        // Update member status to ACTIVE
        member.setStatus(Member.Status.ACTIVE);
        memberRepository.save(member);

        MemberReactivation saved = memberReactivationRepository.save(reactivation);

        auditService.logAction(validatedBy, "REACTIVATION_APPROVED",
                "MemberReactivation", reactivationId,
                "Reactivation approved: " + validationNotes,
                "Treasurer approved member reactivation", "SUCCESS");

        return saved;
    }

    /**
     * Get pending reactivations (for Treasurer approval)
     */
    public List<MemberReactivation> getPendingReactivations() {
        return memberReactivationRepository.findByStatus("PENDING");
    }

    /**
     * Get all reactivations (for reports)
     */
    public List<MemberReactivation> getAllReactivations() {
        return memberReactivationRepository.findAll();
    }
}
