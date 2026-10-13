package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.MemberExit;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberExitRepository;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.service.MemberExitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Maker-checker member exit workflow:
 * Credit Committee initiates -> Treasurer approves.
 */
@RestController
@RequestMapping("/api/members")
@CrossOrigin
public class MemberExitController {

    @Autowired
    private MemberExitService memberExitService;

    @Autowired
    private MemberExitRepository memberExitRepository;

    @Autowired
    private UserRepository userRepository;

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found: " + authentication.getName()));
    }

    @GetMapping("/exits/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREDIT_COMMITTEE', 'TREASURER', 'AUDITOR')")
    public ResponseEntity<ApiResponse<List<MemberExit>>> getAllExits() {
        return ResponseEntity.ok(ApiResponse.success("Exits retrieved", memberExitService.getAllExits()));
    }

    @GetMapping("/exits/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREDIT_COMMITTEE', 'TREASURER', 'AUDITOR')")
    public ResponseEntity<ApiResponse<List<MemberExit>>> getPendingExits() {
        return ResponseEntity.ok(ApiResponse.success("Pending exits retrieved", memberExitService.getPendingExits()));
    }

    @GetMapping("/exits/approved")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREDIT_COMMITTEE', 'TREASURER', 'AUDITOR')")
    public ResponseEntity<ApiResponse<List<MemberExit>>> getApprovedExits() {
        return ResponseEntity.ok(ApiResponse.success("Approved exits retrieved", memberExitService.getApprovedExits()));
    }

    @GetMapping("/{memberId}/exit/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREDIT_COMMITTEE', 'TREASURER', 'AUDITOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExitSummary(@PathVariable Long memberId) {
        try {
            return ResponseEntity.ok(ApiResponse.success("Exit summary calculated", memberExitService.calculateExitSummary(memberId)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{memberId}/exit/initiate")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<MemberExit>> initiateExit(
            @PathVariable Long memberId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        try {
            String reason = request.get("exitReason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Exit reason is required"));
            }
            MemberExit exit = memberExitService.initiateMemberExit(memberId, reason.trim(), currentUser(authentication));
            return ResponseEntity.ok(ApiResponse.success("Member exit initiated successfully", exit));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/exit/{exitId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
    public ResponseEntity<ApiResponse<MemberExit>> approveExit(
            @PathVariable Long exitId,
            @RequestBody(required = false) Map<String, String> request,
            Authentication authentication) {
        try {
            User approver = currentUser(authentication);
            MemberExit existing = memberExitRepository.findById(exitId)
                    .orElseThrow(() -> new RuntimeException("Exit not found"));
            if (existing.getInitiatedBy() != null && existing.getInitiatedBy().getId().equals(approver.getId())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("The person who initiated an exit cannot approve it"));
            }
            String notes = request != null ? request.get("approvalNotes") : null;
            MemberExit exit = memberExitService.approveMemberExit(exitId, notes, approver);
            return ResponseEntity.ok(ApiResponse.success("Member exit approved successfully", exit));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
