package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.MemberReactivation;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.service.MemberReactivationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
public class MemberReactivationController {

    @Autowired
    private MemberReactivationService memberReactivationService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Initiate member reactivation
     * Credit Committee initiates reactivation, Treasurer approves
     */
    @PostMapping("/{memberId}/reactivate")
    @PreAuthorize("hasRole('CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<MemberReactivation>> initiateReactivation(
            @PathVariable String memberId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String reason = request.get("reason");

            // Find member by employee ID
            Member member = memberRepository.findByEmployeeId(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found with employee ID: " + memberId));

            // Get the user
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            MemberReactivation reactivation = memberReactivationService.initiateReactivation(member.getId(), reason, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Reactivation initiated successfully", reactivation));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Validate reactivation (Treasurer approval)
     * Only TREASURER can validate reactivations
     */
    @PostMapping("/reactivation/{reactivationId}/validate")
    @PreAuthorize("hasRole('TREASURER')")
    public ResponseEntity<ApiResponse<MemberReactivation>> validateReactivation(
            @PathVariable Long reactivationId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String validationNotes = request.get("validationNotes");

            // Get the user
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            // Validate reactivation
            MemberReactivation reactivation = memberReactivationService.validateReactivation(reactivationId, validationNotes, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Reactivation validated successfully", reactivation));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get pending reactivations (for Treasurer approval)
     */
    @GetMapping("/reactivations/pending")
    @PreAuthorize("hasRole('TREASURER')")
    public ResponseEntity<ApiResponse<List<MemberReactivation>>> getPendingReactivations() {
        try {
            List<MemberReactivation> pending = memberReactivationService.getPendingReactivations();
            return ResponseEntity.ok(new ApiResponse<>(true, "Pending reactivations retrieved", pending));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get all reactivations (for reports)
     */
    @GetMapping("/reactivations/all")
    @PreAuthorize("hasRole('TREASURER') or hasRole('CREDIT_COMMITTEE') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<MemberReactivation>>> getAllReactivations() {
        try {
            List<MemberReactivation> all = memberReactivationService.getAllReactivations();
            return ResponseEntity.ok(new ApiResponse<>(true, "All reactivations retrieved", all));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
