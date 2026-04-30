package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.MemberSuspension;
import com.minet.sacco.entity.User;
import com.minet.sacco.service.MemberSuspensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/members")
public class MemberSuspensionController {

    @Autowired
    private MemberSuspensionService memberSuspensionService;

    /**
     * Suspend a member
     * Only ADMIN and CREDIT_COMMITTEE can suspend members
     */
    @PostMapping("/{memberId}/suspend")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<MemberSuspension>> suspendMember(
            @PathVariable Long memberId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String reason = request.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Reason is required", null));
            }

            User user = (User) authentication.getPrincipal();
            MemberSuspension suspension = memberSuspensionService.suspendMember(memberId, reason, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Member suspended successfully", suspension));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Lift suspension
     * Only ADMIN and CREDIT_COMMITTEE can lift suspensions
     */
    @PostMapping("/{memberId}/lift-suspension")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<MemberSuspension>> liftSuspension(
            @PathVariable Long memberId,
            Authentication authentication) {

        try {
            User user = (User) authentication.getPrincipal();
            MemberSuspension suspension = memberSuspensionService.liftSuspension(memberId, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Suspension lifted successfully", suspension));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Check if member is suspended
     */
    @GetMapping("/{memberId}/suspension-status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSuspensionStatus(@PathVariable Long memberId) {
        try {
            boolean isSuspended = memberSuspensionService.isMemberSuspended(memberId);
            MemberSuspension suspension = memberSuspensionService.getActiveSuspension(memberId).orElse(null);

            Map<String, Object> response = Map.of(
                    "memberId", memberId,
                    "isSuspended", isSuspended,
                    "suspension", suspension
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "Suspension status retrieved", response));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get suspension history
     */
    @GetMapping("/{memberId}/suspension-history")
    public ResponseEntity<ApiResponse<List<MemberSuspension>>> getSuspensionHistory(@PathVariable Long memberId) {
        try {
            List<MemberSuspension> history = memberSuspensionService.getSuspensionHistory(memberId);
            return ResponseEntity.ok(new ApiResponse<>(true, "Suspension history retrieved", history));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get all active suspensions
     * Only ADMIN and CREDIT_COMMITTEE can view
     */
    @GetMapping("/suspensions/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<List<MemberSuspension>>> getAllActiveSuspensions() {
        try {
            List<MemberSuspension> suspensions = memberSuspensionService.getAllActiveSuspensions();
            return ResponseEntity.ok(new ApiResponse<>(true, "Active suspensions retrieved", suspensions));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }
}
