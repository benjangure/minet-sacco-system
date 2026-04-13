package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.service.EligibilityCalculationService;
import com.minet.sacco.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * API endpoints for eligibility calculation
 * Provides real-time eligibility information for loan applications
 */
@RestController
@RequestMapping("/api/member/eligibility")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EligibilityCalculationController {

    private static final Logger log = LoggerFactory.getLogger(EligibilityCalculationController.class);

    @Autowired
    private EligibilityCalculationService eligibilityCalculationService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserService userService;

    /**
     * Get current eligibility for authenticated member
     * GET /api/member/eligibility
     */
    @GetMapping
    public ResponseEntity<?> getCurrentEligibility(Authentication authentication) {
        try {
            log.debug("Getting current eligibility for user: {}", authentication.getName());

            // Get user from authentication by username
            Optional<User> userOpt = userService.getUserByUsername(authentication.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "User not found", null));
            }

            User user = userOpt.get();
            if (user.getMemberId() == null) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "User is not associated with a member", null));
            }

            Optional<Member> memberOpt = memberRepository.findById(user.getMemberId());
            if (memberOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Member not found", null));
            }

            Member member = memberOpt.get();

            // Calculate eligibility
            EligibilityCalculationService.EligibilityResult result = 
                eligibilityCalculationService.calculateCurrentEligibility(member);

            return ResponseEntity.ok(new ApiResponse<>(true, "Eligibility calculated", result));
        } catch (Exception e) {
            log.error("Error calculating eligibility", e);
            return ResponseEntity.status(500).body(
                new ApiResponse<>(false, "Failed to calculate eligibility: " + e.getMessage(), null));
        }
    }

    /**
     * Calculate hypothetical eligibility with a new loan
     * POST /api/member/eligibility/calculate
     * 
     * Request body:
     * {
     *   "loanAmount": 20000,
     *   "selfGuaranteeAmount": 15000
     * }
     */
    @PostMapping("/calculate")
    public ResponseEntity<?> calculateHypotheticalEligibility(
            @RequestBody HypotheticalLoanRequest request,
            Authentication authentication) {
        try {
            log.debug("Calculating hypothetical eligibility - Loan: {}, Self-guarantee: {}",
                    request.getLoanAmount(), request.getSelfGuaranteeAmount());

            // Get user from authentication by username
            Optional<User> userOpt = userService.getUserByUsername(authentication.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "User not found", null));
            }

            User user = userOpt.get();
            if (user.getMemberId() == null) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "User is not associated with a member", null));
            }

            Optional<Member> memberOpt = memberRepository.findById(user.getMemberId());
            if (memberOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Member not found", null));
            }

            Member member = memberOpt.get();

            // Validate request
            if (request.getLoanAmount() == null || request.getLoanAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Loan amount must be greater than zero", null));
            }

            if (request.getSelfGuaranteeAmount() == null || 
                request.getSelfGuaranteeAmount().compareTo(BigDecimal.ZERO) < 0) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Self-guarantee amount cannot be negative", null));
            }

            if (request.getSelfGuaranteeAmount().compareTo(request.getLoanAmount()) > 0) {
                return ResponseEntity.badRequest().body(
                    new ApiResponse<>(false, "Self-guarantee amount cannot exceed loan amount", null));
            }

            // Calculate hypothetical eligibility
            EligibilityCalculationService.EligibilityResult result = 
                eligibilityCalculationService.calculateHypotheticalEligibility(
                    member, request.getLoanAmount(), request.getSelfGuaranteeAmount());

            return ResponseEntity.ok(new ApiResponse<>(true, "Hypothetical eligibility calculated", result));
        } catch (Exception e) {
            log.error("Error calculating hypothetical eligibility", e);
            return ResponseEntity.status(500).body(
                new ApiResponse<>(false, "Failed to calculate eligibility: " + e.getMessage(), null));
        }
    }

    /**
     * Get eligibility for a specific member (Admin/Loan Officer only)
     * GET /api/member/eligibility/{memberId}
     */
    @GetMapping("/{memberId}")
    public ResponseEntity<?> getMemberEligibility(@PathVariable Long memberId) {
        try {
            log.debug("Getting eligibility for member: {}", memberId);

            Optional<Member> memberOpt = memberRepository.findById(memberId);
            if (memberOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Member member = memberOpt.get();

            // Calculate eligibility
            EligibilityCalculationService.EligibilityResult result = 
                eligibilityCalculationService.calculateCurrentEligibility(member);

            return ResponseEntity.ok(new ApiResponse<>(true, "Eligibility calculated", result));
        } catch (Exception e) {
            log.error("Error calculating eligibility for member {}", memberId, e);
            return ResponseEntity.status(500).body(
                new ApiResponse<>(false, "Failed to calculate eligibility: " + e.getMessage(), null));
        }
    }

    /**
     * Request body for hypothetical eligibility calculation
     */
    public static class HypotheticalLoanRequest {
        private BigDecimal loanAmount;
        private BigDecimal selfGuaranteeAmount;

        public HypotheticalLoanRequest() {}

        public HypotheticalLoanRequest(BigDecimal loanAmount, BigDecimal selfGuaranteeAmount) {
            this.loanAmount = loanAmount;
            this.selfGuaranteeAmount = selfGuaranteeAmount;
        }

        public BigDecimal getLoanAmount() { return loanAmount; }
        public void setLoanAmount(BigDecimal loanAmount) { this.loanAmount = loanAmount; }

        public BigDecimal getSelfGuaranteeAmount() { return selfGuaranteeAmount; }
        public void setSelfGuaranteeAmount(BigDecimal selfGuaranteeAmount) { 
            this.selfGuaranteeAmount = selfGuaranteeAmount; 
        }
    }
}
