package com.minet.sacco.service;

import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.MemberSuspension;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.MemberSuspensionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class MemberSuspensionService {

    @Autowired
    private MemberSuspensionRepository memberSuspensionRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationService notificationService;

    /**
     * Suspend a member
     */
    @Transactional
    public MemberSuspension suspendMember(Long memberId, String reason, User suspendedBy) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Check if already suspended
        Optional<MemberSuspension> existing = memberSuspensionRepository.findByMemberIdAndIsActiveTrue(memberId);
        if (existing.isPresent()) {
            throw new RuntimeException("Member is already suspended");
        }

        MemberSuspension suspension = new MemberSuspension();
        suspension.setMember(member);
        suspension.setReason(reason);
        suspension.setSuspendedBy(suspendedBy);
        suspension.setIsActive(false);
        suspension.setStatus("PENDING");

        MemberSuspension saved = memberSuspensionRepository.save(suspension);

        auditService.logAction(suspendedBy, "MEMBER_SUSPENSION_INITIATED",
                "Member", memberId,
                "Member: " + member.getEmployeeId() + ", Reason: " + reason,
                "Credit Committee (acting as HR) initiated suspension - pending Treasurer approval", "SUCCESS");

        // Notify Treasurers about pending suspension
        notificationService.notifyUsersByRole("TREASURER",
                "Member suspension pending approval: " + member.getFirstName() + " " + member.getLastName() + " (" + member.getEmployeeId() + ")",
                "SUSPENSION_PENDING",
                null,
                memberId,
                "MEMBER_SUSPENSION");

        return saved;
    }

    /**
     * Lift suspension
     */
    @Transactional
    public MemberSuspension liftSuspension(Long memberId, User liftedBy) {
        MemberSuspension suspension = memberSuspensionRepository.findByMemberIdAndIsActiveTrue(memberId)
                .orElseThrow(() -> new RuntimeException("No active suspension found for member"));

        suspension.setIsActive(false);
        suspension.setLiftedBy(liftedBy);
        suspension.setLiftedAt(LocalDateTime.now());

        MemberSuspension updated = memberSuspensionRepository.save(suspension);

        auditService.logAction(liftedBy, "MEMBER_SUSPENSION_LIFTED",
                "Member", memberId,
                "Suspension lifted",
                "Member suspension lifted", "SUCCESS");

        return updated;
    }

    /**
     * Check if member is suspended
     */
    public boolean isMemberSuspended(Long memberId) {
        return memberSuspensionRepository.findByMemberIdAndIsActiveTrue(memberId).isPresent();
    }

    /**
     * Get active suspension for member
     */
    public Optional<MemberSuspension> getActiveSuspension(Long memberId) {
        return memberSuspensionRepository.findByMemberIdAndIsActiveTrue(memberId);
    }

    /**
     * Get all suspensions for member
     */
    public List<MemberSuspension> getSuspensionHistory(Long memberId) {
        return memberSuspensionRepository.findByMemberId(memberId);
    }

    /**
     * Get pending suspensions (for Treasurer approval)
     */
    public List<MemberSuspension> getPendingSuspensions() {
        return memberSuspensionRepository.findByStatus("PENDING");
    }

    /**
     * Validate suspension (Credit Committee approval)
     */
    @Transactional
    public MemberSuspension validateSuspension(Long suspensionId, String validationNotes, User validatedBy) {
        MemberSuspension suspension = memberSuspensionRepository.findById(suspensionId)
                .orElseThrow(() -> new RuntimeException("Suspension not found"));

        // Add validation information and approve suspension
        suspension.setValidationNotes(validationNotes);
        suspension.setValidatedBy(validatedBy);
        suspension.setValidatedAt(LocalDateTime.now());
        suspension.setStatus("APPROVED");
        suspension.setIsActive(true);

        // Update member status to SUSPENDED
        Member member = suspension.getMember();
        member.setStatus(Member.Status.SUSPENDED);
        memberRepository.save(member);

        MemberSuspension saved = memberSuspensionRepository.save(suspension);

        // Log validation
        auditService.logAction(validatedBy, "SUSPENSION_APPROVED",
                "MemberSuspension", suspensionId,
                "Suspension approved: " + validationNotes,
                "Treasurer approved member suspension", "SUCCESS");

        return saved;
    }

    /**
     * Reactivate a member
     */
    @Transactional
    public String reactivateMember(Long memberId, User reactivatedBy) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // Check if member has active suspension
        Optional<MemberSuspension> suspension = memberSuspensionRepository.findByMemberIdAndIsActiveTrue(memberId);
        if (!suspension.isPresent()) {
            return "Member is not currently suspended";
        }

        // Deactivate the suspension
        MemberSuspension activeSuspension = suspension.get();
        activeSuspension.setIsActive(false);
        activeSuspension.setLiftedAt(LocalDateTime.now());
        memberSuspensionRepository.save(activeSuspension);

        // Log the reactivation
        auditService.logAction(reactivatedBy, "MEMBER_REACTIVATED",
                "Member", memberId, 
                "Member " + member.getFirstName() + " " + member.getLastName() + " reactivated",
                "Suspension lifted and member reactivated", "SUCCESS");

        return "Member reactivated successfully";
    }

    /**
     * Get all active suspensions
     */
    public List<MemberSuspension> getAllActiveSuspensions() {
        return memberSuspensionRepository.findByIsActiveTrue();
    }
}
