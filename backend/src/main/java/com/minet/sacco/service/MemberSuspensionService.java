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
        suspension.setIsActive(true);

        MemberSuspension saved = memberSuspensionRepository.save(suspension);

        auditService.logAction(suspendedBy, "MEMBER_SUSPENDED",
                "Member", memberId,
                "Member: " + member.getEmployeeId() + ", Reason: " + reason,
                "Member suspended", "SUCCESS");

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
     * Get all active suspensions
     */
    public List<MemberSuspension> getAllActiveSuspensions() {
        return memberSuspensionRepository.findByIsActiveTrue();
    }
}
