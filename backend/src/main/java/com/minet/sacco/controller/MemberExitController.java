package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.MemberExit;
import com.minet.sacco.entity.User;
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

    /**
     * Initiate member exit
     * Only ADMIN and CREDIT_COMMITTEE can initiate exits
     */
    @PostMapping("/{memberId}/exit")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<MemberExit>> initiateMemberExit(
            @PathVariable Long memberId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String exitReason = request.get("exitReason");
            if (exitReason == null || exitReason.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Exit reason is required", null));
            }

            User user = (User) authentication.getPrincipal();
            MemberExit exit = memberExitService.initiateMemberExit(memberId, exitReason, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Member exit initiated successfully", exit));

        } catch (RuntimeException e) {
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
            Authentication authentication) {

        try {
            User user = (User) authentication.getPrincipal();
            MemberExit exit = memberExitService.approveMemberExit(exitId, user);

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
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExitSummary(@PathVariable Long memberId) {
        try {
            Map<String, Object> summary = memberExitService.calculateExitSummary(memberId);
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
     * Get exit record for member
     */
    @GetMapping("/{memberId}/exit")
    public ResponseEntity<ApiResponse<MemberExit>> getMemberExit(@PathVariable Long memberId) {
        try {
            return memberExitService.getMemberExit(memberId)
                    .map(exit -> ResponseEntity.ok(new ApiResponse<>(true, "Exit record retrieved", exit)))
                    .orElseGet(() -> ResponseEntity.ok(new ApiResponse<>(true, "No exit record found", null)));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
