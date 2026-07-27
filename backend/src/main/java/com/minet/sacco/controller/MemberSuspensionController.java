package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.MemberSuspension;
import com.minet.sacco.entity.MemberExit;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.repository.MemberExitRepository;
import com.minet.sacco.repository.MemberSuspensionRepository;
import com.minet.sacco.service.MemberSuspensionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/members")
public class MemberSuspensionController {

    @Autowired
    private MemberSuspensionService memberSuspensionService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberExitRepository memberExitRepository;

    @Autowired
    private MemberSuspensionRepository memberSuspensionRepository;

    /**
     * Suspend a member
     * Credit Committee initiates suspension (acting as HR), Treasurer approves
     */
    @PostMapping("/{memberId}/suspend")
    @PreAuthorize("hasRole('CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<MemberSuspension>> suspendMember(
            @PathVariable String memberId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String reason = request.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Reason is required", null));
            }

            // Find member by employee ID
            Member member = memberRepository.findByEmployeeId(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found with employee ID: " + memberId));

            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            MemberSuspension suspension = memberSuspensionService.suspendMember(member.getId(), reason, user);

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
    @PreAuthorize("hasRole('CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<MemberSuspension>> liftSuspension(
            @PathVariable String memberId,
            Authentication authentication) {

        try {
            // Find member by employee ID
            Member member = memberRepository.findByEmployeeId(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found with employee ID: " + memberId));

            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));
            MemberSuspension suspension = memberSuspensionService.liftSuspension(member.getId(), user);

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
     * Get all suspensions (for reports)
     */
    @GetMapping("/suspensions/all")
    @PreAuthorize("hasRole('TREASURER') or hasRole('CREDIT_COMMITTEE') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<MemberSuspension>>> getAllSuspensions() {
        try {
            List<MemberSuspension> all = memberSuspensionRepository.findAll();
            return ResponseEntity.ok(new ApiResponse<>(true, "All suspensions retrieved", all));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get pending suspensions (for Treasurer approval)
     */
    @GetMapping("/suspensions/pending")
    @PreAuthorize("hasRole('TREASURER')")
    public ResponseEntity<ApiResponse<List<MemberSuspension>>> getPendingSuspensions() {
        try {
            List<MemberSuspension> pending = memberSuspensionService.getPendingSuspensions();
            return ResponseEntity.ok(new ApiResponse<>(true, "Pending suspensions retrieved", pending));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get member status (active, suspended, exited)
     */
    @GetMapping("/{memberId}/status")
    public ResponseEntity<ApiResponse<Map<String, String>>> getMemberStatus(@PathVariable String memberId) {
        try {
            Member member = memberRepository.findByEmployeeId(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            String status = "ACTIVE";

            // Check if member has an approved exit
            Optional<MemberExit> exit = memberExitRepository.findByMemberIdAndStatus(member.getId(), "APPROVED");
            if (exit.isPresent()) {
                status = "EXITED";
            } else {
                // Check if member has an active suspension
                Optional<MemberSuspension> suspension = memberSuspensionRepository.findByMemberIdAndIsActiveTrue(member.getId());
                if (suspension.isPresent()) {
                    status = "SUSPENDED";
                }
            }

            Map<String, String> response = Map.of("memberId", memberId, "status", status);
            return ResponseEntity.ok(new ApiResponse<>(true, "Member status retrieved", response));
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
     * Validate suspension (Treasurer approval)
     * Only TREASURER can validate suspensions
     */
    @PostMapping("/suspension/{suspensionId}/validate")
    @PreAuthorize("hasRole('TREASURER')")
    public ResponseEntity<ApiResponse<MemberSuspension>> validateSuspension(
            @PathVariable Long suspensionId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            String validationNotes = request.get("validationNotes");

            // Get the user
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found: " + username));

            // Validate suspension
            MemberSuspension suspension = memberSuspensionService.validateSuspension(suspensionId, validationNotes, user);

            return ResponseEntity.ok(new ApiResponse<>(true, "Suspension validated successfully", suspension));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(false, e.getMessage(), null));
        }
    }

    /**
     * Get all active suspensions
     * Only ADMIN and CREDIT_COMMITTEE can view
     */
    @GetMapping("/suspensions/active")
    @PreAuthorize("hasRole('CREDIT_COMMITTEE')")
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
