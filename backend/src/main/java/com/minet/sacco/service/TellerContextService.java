package com.minet.sacco.service;

import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class TellerContextService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    private final Map<Long, Long> tellerMemberContext = new HashMap<>();

    /**
     * Set the current member context for a teller
     */
    @Transactional
    public void setMemberContext(Long userId, Long memberId, User tellerUser) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        tellerMemberContext.put(userId, memberId);

        auditService.logAction(tellerUser, "SET_CONTEXT", "MEMBER", memberId,
                "Teller set member context: " + member.getFirstName() + " " + member.getLastName(),
                "Member context set for teller", "SUCCESS");
    }

    /**
     * Get the current member context for a teller
     */
    public Long getMemberContext(Long userId) {
        return tellerMemberContext.get(userId);
    }

    /**
     * Clear the member context for a teller
     */
    public void clearMemberContext(Long userId, User tellerUser) {
        Long memberId = tellerMemberContext.remove(userId);
        if (memberId != null) {
            auditService.logAction(tellerUser, "CLEAR_CONTEXT", "MEMBER", memberId,
                    "Teller cleared member context",
                    "Member context cleared for teller", "SUCCESS");
        }
    }

    /**
     * Check if teller has a member context set
     */
    public boolean hasMemberContext(Long userId) {
        return tellerMemberContext.containsKey(userId);
    }

    /**
     * Validate that teller is accessing the correct member
     */
    public void validateMemberAccess(Long userId, Long requestedMemberId) {
        Long contextMemberId = tellerMemberContext.get(userId);
        if (contextMemberId == null) {
            throw new RuntimeException("Teller must set member context first");
        }
        if (!contextMemberId.equals(requestedMemberId)) {
            throw new RuntimeException("Teller can only access the selected member");
        }
    }
}
