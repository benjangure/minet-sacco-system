package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.MemberExit;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.service.MemberExitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
public class MemberExitController {

    @Autowired
    private MemberExitService memberExitService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Initiate member exit
     * Credit Committee initiates member exit (acting as HR), Treasurer approves
     */
    @PostMapping("/{memberId}/exit")
    @PreAuthorize("hasRole('CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<MemberExit>> initiateMemberExit(
            @PathVariable String memberId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String exitReason = request.get("exitReason");
            if (exitReason == null || exitReason.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Exit reason is required", null));
            }

            // Find member by employee ID
            Member member = memberRepository.findByEmployeeId(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found with employee ID: " + memberId));

            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            MemberExit exit = memberExitService.initiateMemberExit(member.getId(), exitReason, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Member exit initiated successfully", exit));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get all exits (for reports)
     */
    @GetMapping("/exits/all")
    @PreAuthorize("hasRole('TREASURER') or hasRole('CREDIT_COMMITTEE') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<MemberExit>>> getAllExits() {
        try {
            List<MemberExit> all = memberExitService.getAllExits();
            return ResponseEntity.ok(new ApiResponse<>(true, "All exits retrieved", all));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Approve member exit
     * Only TREASURER can approve exits
     */
    @PostMapping("/exit/{exitId}/approve")
    @PreAuthorize("hasRole('TREASURER')")
    public ResponseEntity<ApiResponse<MemberExit>> approveMemberExit(
            @PathVariable Long exitId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String approvalNotes = request.get("approvalNotes");

            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            MemberExit exit = memberExitService.approveMemberExit(exitId, approvalNotes, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Member exit approved successfully", exit));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get exit summary for member
     */
    @GetMapping("/{memberId}/exit/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExitSummary(@PathVariable String memberId) {
        try {
            // Find member by employee ID
            Member member = memberRepository.findByEmployeeId(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found with employee ID: " + memberId));
            
            Map<String, Object> summary = memberExitService.calculateExitSummary(member.getId());
            return ResponseEntity.ok(new ApiResponse<>(true, "Exit summary calculated", summary));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get pending exits
     */
    @GetMapping("/exits/pending")
    @PreAuthorize("hasRole('TREASURER')")
    public ResponseEntity<ApiResponse<List<MemberExit>>> getPendingExits() {
        try {
            List<MemberExit> exits = memberExitService.getPendingExits();
            return ResponseEntity.ok(new ApiResponse<>(true, "Pending exits retrieved", exits));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get approved exits
     */
    @GetMapping("/exits/approved")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ResponseEntity<ApiResponse<List<MemberExit>>> getApprovedExits() {
        try {
            List<MemberExit> exits = memberExitService.getApprovedExits();
            return ResponseEntity.ok(new ApiResponse<>(true, "Approved exits retrieved", exits));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Approve member exit (Treasurer approval)
     * Only TREASURER and ADMIN can approve exits
     */
    @PostMapping("/{memberId}/approve-exit")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ResponseEntity<ApiResponse<MemberExit>> approveMemberExit(
            @PathVariable String memberId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String approvalNotes = request.get("approvalNotes");
            
            // Find member by employee ID
            Member member = memberRepository.findByEmployeeId(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found with employee ID: " + memberId));

            // Get pending exit
            MemberExit pendingExit = memberExitService.getPendingExit(member.getId())
                    .orElseThrow(() -> new RuntimeException("No pending exit found for member"));

            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            MemberExit approvedExit = memberExitService.approveMemberExit(pendingExit.getId(), approvalNotes, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Member exit approved successfully", approvedExit));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get exit record for member
     */
    @GetMapping("/{memberId}/exit")
    public ResponseEntity<ApiResponse<MemberExit>> getMemberExit(@PathVariable String memberId) {
        try {
            // Find member by employee ID
            Member member = memberRepository.findByEmployeeId(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found with employee ID: " + memberId));
            
            return memberExitService.getMemberExit(member.getId())
                    .map(exit -> ResponseEntity.ok(new ApiResponse<>(true, "Exit record retrieved", exit)))
                    .orElseGet(() -> ResponseEntity.ok(new ApiResponse<>(true, "No exit record found", null)));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
