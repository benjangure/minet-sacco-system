package com.minet.sacco.service;

import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.SupportTicket;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.SupportTicketRepository;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.controller.CustomerSupportController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CustomerSupportService {

    @Autowired
    private SupportTicketRepository supportTicketRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuditService auditService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private EmailService emailService;

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

    /**
     * Send password reset email to member (Admin initiated)
     * This method requests a password reset for a member via the PasswordResetService
     * which handles token generation, storage, and email sending.
     */
    @Transactional
    public void sendPasswordResetEmail(Member member, User initiatedBy) {
        try {
            // Get member's user account
            User memberUser = userRepository.findByMemberId(member.getId())
                    .orElseThrow(() -> new RuntimeException("User account not found for member: " + member.getId()));
            
            // Use PasswordResetService to handle token generation and email sending
            // This is admin-initiated password reset (true = admin initiated)
            String resetToken = passwordResetService.requestPasswordResetForMember(memberUser.getUsername(), true);
            
            if (resetToken != null) {
                // Log successful password reset initiation
                auditService.logAction(initiatedBy, "RESET_PASSWORD_REQUEST", "MEMBER", member.getId(),
                        "Password reset initiated for member: " + member.getFirstName() + " " + member.getLastName(),
                        "Reset email sent to: " + member.getEmail(), "SUCCESS");
            } else {
                // Log failed password reset
                auditService.logAction(initiatedBy, "RESET_PASSWORD_REQUEST", "MEMBER", member.getId(),
                        "Password reset FAILED for member: " + member.getFirstName() + " " + member.getLastName(),
                        "Reason: Could not generate reset token", "FAILED");
            }
        } catch (Exception e) {
            // Log exception and fail gracefully
            auditService.logAction(initiatedBy, "RESET_PASSWORD_REQUEST", "MEMBER", member.getId(),
                    "Password reset FAILED for member: " + member.getFirstName() + " " + member.getLastName(),
                    "Error: " + e.getMessage(), "FAILED");
            throw new RuntimeException("Failed to send password reset email: " + e.getMessage(), e);
        }
    }

    /**
     * Set a temporary password for a member (Customer Support initiated)
     */
    @Transactional
    public CustomerSupportController.SetTemporaryPasswordResponse setTemporaryPassword(Member member, User initiatedBy) {
        // Generate a temporary password (8 characters: mix of letters and numbers)
        String temporaryPassword = generateTemporaryPassword();
        
        // Get or create user account for member
        User memberUser = userRepository.findByMemberId(member.getId())
                .orElseThrow(() -> new RuntimeException("User account not found for this member"));
        
        // Encode and set the temporary password
        memberUser.setPassword(passwordEncoder.encode(temporaryPassword));
        memberUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(memberUser);
        
        // Log the action
        auditService.logAction(initiatedBy, "SET_TEMPORARY_PASSWORD", "MEMBER", member.getId(),
                "Temporary password set for member: " + member.getFirstName() + " " + member.getLastName(),
                "Temporary password: " + temporaryPassword, "SUCCESS");
        
        // Return response with temporary password
        return new CustomerSupportController.SetTemporaryPasswordResponse(
                temporaryPassword,
                "Temporary password has been set. Member should change it on first login."
        );
    }

    /**
     * Generate a temporary password (8 characters: uppercase, lowercase, numbers)
     */
    private String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt((int) (Math.random() * chars.length())));
        }
        return password.toString();
    }
}
