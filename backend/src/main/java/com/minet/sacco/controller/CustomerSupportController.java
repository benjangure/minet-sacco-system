package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.SupportTicket;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.service.CustomerSupportService;
import com.minet.sacco.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@CrossOrigin
public class CustomerSupportController {

    @Autowired
    private CustomerSupportService customerSupportService;

    @Autowired
    private UserService userService;

    @Autowired
    private MemberRepository memberRepository;

    @PostMapping("/tickets")
    @PreAuthorize("hasRole('ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<SupportTicket>> createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            Authentication authentication) {

        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        SupportTicket.Priority priority;
        try {
            priority = SupportTicket.Priority.valueOf(request.getPriority().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Invalid priority: " + request.getPriority()));
        }

        SupportTicket ticket = customerSupportService.createTicket(
                request.getMemberId(),
                request.getSubject(),
                request.getDescription(),
                priority,
                user
        );

        return ResponseEntity.ok(ApiResponse.success("Support ticket created", ticket));
    }

    @GetMapping("/tickets/member/{memberId}")
    @PreAuthorize("hasRole('ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<List<SupportTicket>>> getTicketsByMember(
            @PathVariable Long memberId) {

        List<SupportTicket> tickets = customerSupportService.getTicketsByMember(memberId);
        return ResponseEntity.ok(ApiResponse.success("Member tickets retrieved", tickets));
    }

    @GetMapping("/tickets/my-tickets")
    @PreAuthorize("hasRole('ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<List<SupportTicket>>> getMyTickets(
            Authentication authentication) {

        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<SupportTicket> tickets = customerSupportService.getTicketsByCreator(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Your tickets retrieved", tickets));
    }

    @GetMapping("/tickets/open")
    @PreAuthorize("hasRole('ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<List<SupportTicket>>> getOpenTickets() {
        List<SupportTicket> tickets = customerSupportService.getOpenTickets();
        return ResponseEntity.ok(ApiResponse.success("Open tickets retrieved", tickets));
    }

    @PostMapping("/tickets/{ticketId}/resolve")
    @PreAuthorize("hasRole('ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<SupportTicket>> resolveTicket(
            @PathVariable Long ticketId,
            @Valid @RequestBody ResolveTicketRequest request,
            Authentication authentication) {

        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        SupportTicket ticket = customerSupportService.resolveTicket(ticketId, request.getResolution(), user);
        return ResponseEntity.ok(ApiResponse.success("Ticket resolved", ticket));
    }

    @GetMapping("/members/{memberId}/profile")
    @PreAuthorize("hasRole('ROLE_CUSTOMER_SUPPORT')")
    public ResponseEntity<ApiResponse<Member>> getMemberProfile(@PathVariable Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        return ResponseEntity.ok(ApiResponse.success("Member profile retrieved", member));
    }

    // DTOs
    public static class CreateTicketRequest {
        private Long memberId;
        private String subject;
        private String description;
        private String priority = "MEDIUM";

        public Long getMemberId() { return memberId; }
        public void setMemberId(Long memberId) { this.memberId = memberId; }

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
    }

    public static class ResolveTicketRequest {
        private String resolution;

        public String getResolution() { return resolution; }
        public void setResolution(String resolution) { this.resolution = resolution; }
    }
}
