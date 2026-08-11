package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.LoanTopUpRequest;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.TopUpGuarantor;
import com.minet.sacco.entity.User;
import com.minet.sacco.service.LoanTopUpRequestService;
import com.minet.sacco.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LoanTopUpRequestController {

    @Autowired
    private LoanTopUpRequestService topUpRequestService;

    @Autowired
    private UserService userService;

    /**
     * Member creates a top-up request
     */
    @PostMapping("/loans/{loanId}/request-topup")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createTopUpRequest(
            @PathVariable Long loanId,
            @RequestBody TopUpRequestDTO requestDTO,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (user.getMemberId() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User is not associated with a member"));
            }
            
            Long memberId = user.getMemberId();
            
            // Convert DTO guarantors to service format
            if (requestDTO.getGuarantors() == null || requestDTO.getGuarantors().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("At least one guarantor is required for top-up request"));
            }
            
            List<LoanTopUpRequestService.GuarantorAssignment> guarantorAssignments = requestDTO.getGuarantors().stream()
                .map(g -> new LoanTopUpRequestService.GuarantorAssignment(
                    g.getMemberNumber(), 
                    g.getGuaranteeAmount()))
                .collect(Collectors.toList());
            
            LoanTopUpRequest topUpRequest = topUpRequestService.createTopUpRequest(
                loanId,
                memberId,
                requestDTO.getRequestedAmount(),
                requestDTO.getPurpose(),
                guarantorAssignments
            );
            
            // Build response
            Map<String, Object> response = buildTopUpRequestResponse(topUpRequest);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Top-up request created successfully. Waiting for guarantor approvals.", 
                response));
                
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get top-up requests for a specific loan
     */
    @GetMapping("/loans/{loanId}/topup-requests")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopUpRequestsByLoan(
            @PathVariable Long loanId,
            Authentication authentication) {
        try {
            List<LoanTopUpRequest> requests = topUpRequestService.getTopUpRequestsByLoan(loanId);
            
            List<Map<String, Object>> response = requests.stream()
                .map(this::buildTopUpRequestResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("Top-up requests retrieved", response));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get member's top-up requests
     */
    @GetMapping("/member/topup-requests")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMemberTopUpRequests(
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (user.getMemberId() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User is not associated with a member"));
            }
            
            Long memberId = user.getMemberId();
            List<LoanTopUpRequest> requests = topUpRequestService.getTopUpRequestsByMember(memberId);
            
            List<Map<String, Object>> response = requests.stream()
                .map(this::buildTopUpRequestResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("Your top-up requests retrieved", response));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get pending guarantor approvals for the logged-in member
     */
    @GetMapping("/member/pending-topup-guarantees")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingGuarantorApprovals(
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (user.getMemberId() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User is not associated with a member"));
            }
            
            Long memberId = user.getMemberId();
            List<TopUpGuarantor> guarantors = topUpRequestService.getPendingGuarantorApprovals(memberId);
            
            List<Map<String, Object>> response = guarantors.stream()
                .map(this::buildGuarantorResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(
                "Pending guarantor approvals retrieved", response));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Guarantor approves a top-up request
     */
    @PostMapping("/topup-requests/{topUpRequestId}/guarantor/approve")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<String>> approveTopUpGuarantee(
            @PathVariable Long topUpRequestId,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (user.getMemberId() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User is not associated with a member"));
            }
            
            Long memberId = user.getMemberId();
            topUpRequestService.approveTopUpGuarantee(topUpRequestId, memberId);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Top-up guarantee approved successfully", null));
                
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Guarantor rejects a top-up request
     */
    @PostMapping("/topup-requests/{topUpRequestId}/guarantor/reject")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<String>> rejectTopUpGuarantee(
            @PathVariable Long topUpRequestId,
            @RequestBody Map<String, String> requestBody,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (user.getMemberId() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User is not associated with a member"));
            }
            
            String reason = requestBody.get("reason");
            if (reason == null || reason.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Rejection reason is required"));
            }
            
            Long memberId = user.getMemberId();
            topUpRequestService.rejectTopUpGuarantee(topUpRequestId, memberId, reason);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Top-up guarantee rejected successfully", null));
                
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Member cancels their top-up request
     */
    @PostMapping("/topup-requests/{topUpRequestId}/cancel")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<String>> cancelTopUpRequest(
            @PathVariable Long topUpRequestId,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (user.getMemberId() == null) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User is not associated with a member"));
            }
            
            Long memberId = user.getMemberId();
            topUpRequestService.cancelTopUpRequest(topUpRequestId, memberId);
            
            return ResponseEntity.ok(ApiResponse.success(
                "Top-up request cancelled successfully", null));
                
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // STAFF APPROVAL PIPELINE  (Loan Officer → Credit Committee → Treasurer)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Generic approve endpoint — delegates to the correct stage based on
     * the caller's role.  Accepted roles: LOAN_OFFICER, CREDIT_COMMITTEE, TREASURER.
     *
     * POST /api/admin/topup-requests/{id}/approve
     * Body (optional): { "comments": "..." }
     */
    @PostMapping("/admin/topup-requests/{topUpRequestId}/approve")
    @PreAuthorize("hasAnyRole('ROLE_LOAN_OFFICER','ROLE_CREDIT_COMMITTEE','ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveTopUpRequest(
            @PathVariable Long topUpRequestId,
            @RequestBody(required = false) Map<String, String> body,
            Authentication authentication) {
        try {
            User user = getUser(authentication);
            String comments = body != null ? body.getOrDefault("comments", "") : "";
            String role = user.getRole().name();

            if ("ROLE_LOAN_OFFICER".equals(role) || "LOAN_OFFICER".equals(role)) {
                topUpRequestService.loanOfficerDecision(topUpRequestId, user, true, comments);
            } else if ("ROLE_CREDIT_COMMITTEE".equals(role) || "CREDIT_COMMITTEE".equals(role)) {
                topUpRequestService.creditCommitteeDecision(topUpRequestId, user, true, comments);
            } else if ("ROLE_TREASURER".equals(role) || "TREASURER".equals(role)) {
                topUpRequestService.treasurerDecision(topUpRequestId, user, true, comments);
            } else {
                return ResponseEntity.status(403).body(ApiResponse.error("Role not authorized to approve top-up requests"));
            }

            LoanTopUpRequest updated = topUpRequestService.getTopUpRequestById(topUpRequestId)
                .orElseThrow(() -> new RuntimeException("Top-up request not found"));
            return ResponseEntity.ok(ApiResponse.success("Top-up request approved successfully", buildTopUpRequestResponse(updated)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Generic reject endpoint — delegates to the correct stage.
     *
     * POST /api/admin/topup-requests/{id}/reject
     * Body: { "reason": "..." }
     */
    @PostMapping("/admin/topup-requests/{topUpRequestId}/reject")
    @PreAuthorize("hasAnyRole('ROLE_LOAN_OFFICER','ROLE_CREDIT_COMMITTEE','ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<String>> rejectTopUpRequest(
            @PathVariable Long topUpRequestId,
            @RequestBody Map<String, String> requestBody,
            Authentication authentication) {
        try {
            User user = getUser(authentication);
            String reason = requestBody.get("reason");
            if (reason == null || reason.trim().isEmpty())
                return ResponseEntity.badRequest().body(ApiResponse.error("Rejection reason is required"));

            String role = user.getRole().name();
            if ("ROLE_LOAN_OFFICER".equals(role) || "LOAN_OFFICER".equals(role)) {
                topUpRequestService.loanOfficerDecision(topUpRequestId, user, false, reason);
            } else if ("ROLE_CREDIT_COMMITTEE".equals(role) || "CREDIT_COMMITTEE".equals(role)) {
                topUpRequestService.creditCommitteeDecision(topUpRequestId, user, false, reason);
            } else if ("ROLE_TREASURER".equals(role) || "TREASURER".equals(role)) {
                topUpRequestService.treasurerDecision(topUpRequestId, user, false, reason);
            } else {
                return ResponseEntity.status(403).body(ApiResponse.error("Role not authorized to reject top-up requests"));
            }

            return ResponseEntity.ok(ApiResponse.success("Top-up request rejected successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Treasurer disburses an APPROVED top-up.
     *
     * POST /api/admin/topup-requests/{id}/disburse
     */
    @PostMapping("/admin/topup-requests/{topUpRequestId}/disburse")
    @PreAuthorize("hasRole('ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> disburseTopUpRequest(
            @PathVariable Long topUpRequestId,
            Authentication authentication) {
        try {
            User user = getUser(authentication);
            LoanTopUpRequest topUpRequest = topUpRequestService.disburseTopUp(topUpRequestId, user);
            return ResponseEntity.ok(ApiResponse.success(
                "Top-up disbursed successfully", buildTopUpRequestResponse(topUpRequest)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUEUE ENDPOINTS — each role fetches its own pending queue
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns requests relevant to the calling user's role so the frontend
     * can show a unified queue regardless of role.
     *
     * GET /api/admin/topup-requests/pending
     */
    @GetMapping("/admin/topup-requests/pending")
    @PreAuthorize("hasAnyRole('ROLE_LOAN_OFFICER','ROLE_CREDIT_COMMITTEE','ROLE_TREASURER','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingTopUpRequests(
            Authentication authentication) {
        try {
            User user = getUser(authentication);
            String role = user.getRole().name();
            List<LoanTopUpRequest> requests;

            if ("ROLE_LOAN_OFFICER".equals(role) || "LOAN_OFFICER".equals(role)) {
                requests = topUpRequestService.getPendingTopUpRequestsForLoanOfficer();
            } else if ("ROLE_CREDIT_COMMITTEE".equals(role) || "CREDIT_COMMITTEE".equals(role)) {
                requests = topUpRequestService.getPendingTopUpRequestsForCreditCommittee();
            } else if ("ROLE_TREASURER".equals(role) || "TREASURER".equals(role)) {
                // Treasurer sees both PENDING_TREASURER and APPROVED (ready to disburse)
                java.util.ArrayList<LoanTopUpRequest> combined = new java.util.ArrayList<>();
                combined.addAll(topUpRequestService.getPendingTopUpRequestsForTreasurer());
                combined.addAll(topUpRequestService.getApprovedTopUpRequests());
                requests = combined;
            } else {
                // Admin sees everything active
                requests = topUpRequestService.getTopUpRequestsByStatus(java.util.Arrays.asList(
                    LoanTopUpRequest.Status.PENDING_GUARANTOR_APPROVAL,
                    LoanTopUpRequest.Status.PENDING_LOAN_OFFICER_REVIEW,
                    LoanTopUpRequest.Status.PENDING_CREDIT_COMMITTEE,
                    LoanTopUpRequest.Status.PENDING_TREASURER,
                    LoanTopUpRequest.Status.APPROVED
                ));
            }

            List<LoanTopUpRequest> finalRequests = requests;
            List<Map<String, Object>> response = finalRequests.stream()
                .map(request -> {
                    try {
                        return buildTopUpRequestResponse(request);
                    } catch (Exception e) {
                        System.err.println("Error building response for top-up request " + request.getId() + ": " + e.getMessage());
                        Map<String, Object> minimal = new HashMap<>();
                        minimal.put("id", request.getId());
                        minimal.put("error", "Error loading request details");
                        return minimal;
                    }
                })
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success("Pending top-up requests retrieved", response));
        } catch (Exception e) {
            System.err.println("Error fetching pending top-up requests: " + e.getMessage());
            return ResponseEntity.status(500).body(ApiResponse.error("Error fetching top-up requests: " + e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // LEGACY: original pending endpoint kept for backward compat
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Get all pending top-up requests — legacy endpoint used by some old
     * admin pages.  Delegates to the role-aware pending endpoint above.
     * @deprecated Use /admin/topup-requests/pending (now role-aware)
     */
    @GetMapping("/admin/topup-requests/pending-legacy")
    @PreAuthorize("hasAnyRole('ROLE_LOAN_OFFICER', 'ROLE_TREASURER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getPendingTopUpRequestsLegacy() {
        try {
            List<LoanTopUpRequest> requests = topUpRequestService.getPendingTopUpRequests();
            
            List<Map<String, Object>> response = requests.stream()
                .map(request -> {
                    try {
                        return buildTopUpRequestResponse(request);
                    } catch (Exception e) {
                        System.err.println("Error building response for top-up request " + request.getId() + ": " + e.getMessage());
                        e.printStackTrace();
                        // Return minimal info if full build fails
                        Map<String, Object> minimal = new HashMap<>();
                        minimal.put("id", request.getId());
                        minimal.put("error", "Error loading request details");
                        return minimal;
                    }
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(
                "Pending top-up requests retrieved", response));
                
        } catch (Exception e) {
            System.err.println("Error fetching pending top-up requests: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("Error fetching top-up requests: " + e.getMessage()));
        }
    }

    /**
     * Get all top-up requests (with optional status filter)
     */
    @GetMapping("/admin/topup-requests")
    @PreAuthorize("hasAnyRole('ROLE_LOAN_OFFICER', 'ROLE_TREASURER', 'ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllTopUpRequests(
            @RequestParam(required = false) List<String> statuses) {
        try {
            List<LoanTopUpRequest> requests;
            
            if (statuses != null && !statuses.isEmpty()) {
                List<LoanTopUpRequest.Status> statusEnums = statuses.stream()
                    .map(LoanTopUpRequest.Status::valueOf)
                    .collect(Collectors.toList());
                requests = topUpRequestService.getTopUpRequestsByStatus(statusEnums);
            } else {
                requests = topUpRequestService.getTopUpRequestsByStatus(
                    Arrays.asList(
                        LoanTopUpRequest.Status.PENDING_GUARANTOR_APPROVAL,
                        LoanTopUpRequest.Status.PENDING_LOAN_OFFICER_REVIEW,
                        LoanTopUpRequest.Status.PENDING_CREDIT_COMMITTEE,
                        LoanTopUpRequest.Status.PENDING_TREASURER,
                        LoanTopUpRequest.Status.APPROVED,
                        LoanTopUpRequest.Status.DISBURSED,
                        LoanTopUpRequest.Status.REJECTED
                    )
                );
            }
            
            List<Map<String, Object>> response = requests.stream()
                .map(this::buildTopUpRequestResponse)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success(
                "Top-up requests retrieved", response));
                
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get a specific top-up request details
     */
    @GetMapping("/admin/topup-requests/{topUpRequestId}")
    @PreAuthorize("hasAnyRole('ROLE_LOAN_OFFICER','ROLE_CREDIT_COMMITTEE','ROLE_TREASURER','ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTopUpRequestDetails(
            @PathVariable Long topUpRequestId) {
        try {
            LoanTopUpRequest request = topUpRequestService.getTopUpRequestById(topUpRequestId)
                .orElseThrow(() -> new RuntimeException("Top-up request not found"));
            return ResponseEntity.ok(ApiResponse.success(
                "Top-up request details retrieved", buildTopUpRequestResponse(request)));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPER
    // ─────────────────────────────────────────────────────────────────────────
    private User getUser(Authentication authentication) {
        return userService.getUserByUsername(authentication.getName())
            .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Helper methods
    private Map<String, Object> buildTopUpRequestResponse(LoanTopUpRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", request.getId());
        response.put("loanId", request.getLoan().getId());
        response.put("loanNumber", request.getLoan().getLoanNumber());
        
        // Add full loan details including updated outstanding balance
        Map<String, Object> loanInfo = new HashMap<>();
        loanInfo.put("id", request.getLoan().getId());
        loanInfo.put("loanNumber", request.getLoan().getLoanNumber());
        loanInfo.put("amount", request.getLoan().getAmount());
        loanInfo.put("outstandingBalance", request.getLoan().getOutstandingBalance());
        loanInfo.put("status", request.getLoan().getStatus().name());
        
        // Loan member info
        Member loanMember = request.getLoan().getMember();
        Map<String, Object> loanMemberInfo = new HashMap<>();
        loanMemberInfo.put("memberNumber", loanMember.getMemberNumber());
        loanMemberInfo.put("firstName", loanMember.getFirstName());
        loanMemberInfo.put("lastName", loanMember.getLastName());
        loanInfo.put("member", loanMemberInfo);
        
        response.put("loan", loanInfo);
        
        response.put("requestedAmount", request.getRequestedAmount());
        response.put("purpose", request.getPurpose());
        response.put("status", request.getStatus().name());
        response.put("requestedDate", request.getRequestedDate());
        response.put("totalGuaranteeAmount", request.getTotalGuaranteeAmount());
        response.put("guarantorApprovalCount", request.getGuarantorApprovalCount());
        response.put("guarantorRejectionCount", request.getGuarantorRejectionCount());
        response.put("allGuarantorsApproved", request.getAllGuarantorsApproved());
        
        // Member info
        Member member = request.getMember();
        Map<String, Object> memberInfo = new HashMap<>();
        memberInfo.put("id", member.getId());
        memberInfo.put("memberNumber", member.getMemberNumber());
        memberInfo.put("firstName", member.getFirstName());
        memberInfo.put("lastName", member.getLastName());
        response.put("member", memberInfo);
        
        // Guarantors
        List<Map<String, Object>> guarantors = new ArrayList<>();
        if (request.getGuarantors() != null) {
            guarantors = request.getGuarantors().stream()
                .map(this::buildGuarantorResponse)
                .collect(Collectors.toList());
        }
        response.put("guarantors", guarantors);
        
        // Review info
        if (request.getReviewedBy() != null) {
            response.put("reviewedBy", request.getReviewedBy().getUsername());
            response.put("reviewDate", request.getReviewDate());
        }
        if (request.getRejectionReason() != null) {
            response.put("rejectionReason", request.getRejectionReason());
        }
        
        return response;
    }

    private Map<String, Object> buildGuarantorResponse(TopUpGuarantor guarantor) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", guarantor.getId());
        response.put("guaranteeAmount", guarantor.getGuaranteeAmount());
        response.put("status", guarantor.getStatus().name());
        response.put("requestedDate", guarantor.getRequestedDate());
        
        if (guarantor.getApprovedAt() != null) {
            response.put("approvedAt", guarantor.getApprovedAt());
        }
        if (guarantor.getRejectedAt() != null) {
            response.put("rejectedAt", guarantor.getRejectedAt());
            response.put("rejectionReason", guarantor.getRejectionReason());
        }
        
        // Member info
        Member member = guarantor.getMember();
        Map<String, Object> memberInfo = new HashMap<>();
        memberInfo.put("id", member.getId());
        memberInfo.put("memberNumber", member.getMemberNumber());
        memberInfo.put("firstName", member.getFirstName());
        memberInfo.put("lastName", member.getLastName());
        response.put("member", memberInfo);
        
        // Top-up request info (if needed in context)
        if (guarantor.getTopUpRequest() != null) {
            LoanTopUpRequest topUpRequest = guarantor.getTopUpRequest();
            Map<String, Object> requestInfo = new HashMap<>();
            requestInfo.put("id", topUpRequest.getId());
            requestInfo.put("loanNumber", topUpRequest.getLoan().getLoanNumber());
            requestInfo.put("requestedAmount", topUpRequest.getRequestedAmount());
            requestInfo.put("purpose", topUpRequest.getPurpose());
            requestInfo.put("status", topUpRequest.getStatus().name());
            
            // Requesting member info
            Member requestingMember = topUpRequest.getMember();
            Map<String, Object> requestingMemberInfo = new HashMap<>();
            requestingMemberInfo.put("memberNumber", requestingMember.getMemberNumber());
            requestingMemberInfo.put("firstName", requestingMember.getFirstName());
            requestingMemberInfo.put("lastName", requestingMember.getLastName());
            requestInfo.put("requestingMember", requestingMemberInfo);
            
            response.put("topUpRequest", requestInfo);
        }
        
        return response;
    }

    // DTOs
    public static class TopUpRequestDTO {
        private BigDecimal requestedAmount;
        private String purpose;
        private List<GuarantorDTO> guarantors;

        public BigDecimal getRequestedAmount() {
            return requestedAmount;
        }

        public void setRequestedAmount(BigDecimal requestedAmount) {
            this.requestedAmount = requestedAmount;
        }

        public String getPurpose() {
            return purpose;
        }

        public void setPurpose(String purpose) {
            this.purpose = purpose;
        }

        public List<GuarantorDTO> getGuarantors() {
            return guarantors;
        }

        public void setGuarantors(List<GuarantorDTO> guarantors) {
            this.guarantors = guarantors;
        }
    }

    public static class GuarantorDTO {
        private String memberNumber;
        private BigDecimal guaranteeAmount;

        public String getMemberNumber() {
            return memberNumber;
        }

        public void setMemberNumber(String memberNumber) {
            this.memberNumber = memberNumber;
        }

        public BigDecimal getGuaranteeAmount() {
            return guaranteeAmount;
        }

        public void setGuaranteeAmount(BigDecimal guaranteeAmount) {
            this.guaranteeAmount = guaranteeAmount;
        }
    }

    /**
     * MIGRATION/FIX ENDPOINT: Backfill history for already-disbursed top-ups
     * This creates missing history records for top-ups that were disbursed before history tracking was added
     */

}
