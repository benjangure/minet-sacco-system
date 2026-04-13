package com.minet.sacco.service;

import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.SupportTicket;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.SupportTicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerSupportService {

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuditService auditService;

    /**
     * Create a support ticket
     */
    @Transactional
    public SupportTicket createTicket(Long memberId, String subject, String description, 
                                      SupportTicket.Priority priority, User createdBy) {
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));

        SupportTicket ticket = new SupportTicket();
        ticket.setMember(member);
        ticket.setSubject(subject);
        ticket.setDescription(description);
        ticket.setPriority(priority);
        ticket.setCreatedBy(createdBy);
        ticket.setStatus(SupportTicket.Status.OPEN);
        ticket.setCreatedAt(LocalDateTime.now());

        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        auditService.logAction(createdBy, "CREATE_TICKET", "SUPPORT", ticket.getId(),
                "Support ticket created for member: " + member.getFirstName() + " " + member.getLastName(),
                "Subject: " + subject, "SUCCESS");

        return savedTicket;
    }

    /**
     * Get tickets for a member
     */
    public List<SupportTicket> getTicketsByMember(Long memberId) {
        return supportTicketRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    /**
     * Get tickets created by a support staff
     */
    public List<SupportTicket> getTicketsByCreator(Long userId) {
        return supportTicketRepository.findByCreatedByIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Get open tickets
     */
    public List<SupportTicket> getOpenTickets() {
        return supportTicketRepository.findByStatusOrderByCreatedAtDesc(SupportTicket.Status.OPEN);
    }

    /**
     * Resolve a ticket
     */
    @Transactional
    public SupportTicket resolveTicket(Long ticketId, String resolution, User resolvedBy) {
        SupportTicket ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> new RuntimeException("Ticket not found"));

        ticket.setStatus(SupportTicket.Status.RESOLVED);
        ticket.setResolution(resolution);
        ticket.setResolvedBy(resolvedBy);
        ticket.setResolvedAt(LocalDateTime.now());

        SupportTicket savedTicket = supportTicketRepository.save(ticket);

        auditService.logAction(resolvedBy, "RESOLVE_TICKET", "SUPPORT", ticketId,
                "Support ticket resolved for member: " + ticket.getMember().getFirstName(),
                "Resolution: " + resolution, "SUCCESS");

        return savedTicket;
    }

    /**
     * Get ticket count by status
     */
    public Long getTicketCountByStatus(SupportTicket.Status status) {
        return supportTicketRepository.countByStatus(status);
    }
}
