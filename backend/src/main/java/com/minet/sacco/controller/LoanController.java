package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.dto.LoanApplicationRequest;
import com.minet.sacco.dto.LoanApprovalRequest;
import com.minet.sacco.dto.LoanApprovalValidationDTO;
import com.minet.sacco.dto.LoanRepaymentRequest;
import com.minet.sacco.entity.Loan;
import com.minet.sacco.entity.LoanRepayment;
import com.minet.sacco.entity.User;
import com.minet.sacco.entity.Guarantor;
import com.minet.sacco.service.LoanService;
import com.minet.sacco.service.UserService;
import com.minet.sacco.service.GuarantorValidationService;
import com.minet.sacco.service.GuarantorApprovalService;
import com.minet.sacco.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/loans")
@CrossOrigin
public class LoanController {

    @Autowired
    private LoanService loanService;

    @Autowired
    private UserService userService;

    @Autowired
    private GuarantorValidationService guarantorValidationService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private GuarantorApprovalService guarantorApprovalService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<Loan>>> getAllLoans() {
        List<Loan> loans = loanService.getAllLoans();
        return ResponseEntity.ok(ApiResponse.success("Loans retrieved successfully", loans));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<Loan>> getLoanById(@PathVariable Long id) {
        return loanService.getLoanById(id)
                .map(loan -> ResponseEntity.ok(ApiResponse.success("Loan found", loan)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<Loan>>> getLoansByMemberId(@PathVariable Long memberId) {
        List<Loan> loans = loanService.getLoansByMemberId(memberId);
        return ResponseEntity.ok(ApiResponse.success("Loans retrieved successfully", loans));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<Loan>>> getLoansByStatus(@PathVariable String status) {
        List<Loan> loans = loanService.getLoansByStatus(Loan.Status.valueOf(status));
        return ResponseEntity.ok(ApiResponse.success("Loans retrieved successfully", loans));
    }

    /**
     * Live pre-application eligibility check — no loan is created.
     * Loan Officer calls this while filling the form to see member + guarantor eligibility in real time.
     */
    @GetMapping("/pre-check")
    @PreAuthorize("hasAnyRole('ROLE_LOAN_OFFICER', 'ROLE_TELLER', 'ROLE_CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> preCheckEligibility(
            @RequestParam Long memberId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) List<Long> guarantorIds) {
        try {
            Map<String, Object> result = new HashMap<>();

            // Member eligibility
            com.minet.sacco.entity.Member member = loanService.getMemberById(memberId);
            com.minet.sacco.service.LoanEligibilityValidator.EligibilityResult memberResult =
                    loanService.checkMemberEligibility(member, amount);

            Map<String, Object> memberInfo = new HashMap<>();
            memberInfo.put("name", member.getFirstName() + " " + member.getLastName());
            memberInfo.put("memberNumber", member.getMemberNumber());
            memberInfo.put("eligible", memberResult.isEligible());
            memberInfo.put("errors", memberResult.getErrors());
            memberInfo.put("warnings", memberResult.getWarnings());
            memberInfo.put("savingsBalance", memberResult.getSavingsBalance());
            memberInfo.put("sharesBalance", memberResult.getSharesBalance());
            memberInfo.put("totalBalance", memberResult.getTotalBalance());
            memberInfo.put("activeLoans", memberResult.getActiveLoans());
            result.put("member", memberInfo);

            // Guarantor eligibility (if any selected)
            if (guarantorIds != null && !guarantorIds.isEmpty()) {
                List<GuarantorValidationService.GuarantorValidationResult> gResults =
                        guarantorValidationService.validateAllGuarantors(guarantorIds, amount);
                result.put("guarantors", gResults);
                result.put("allGuarantorsEligible", guarantorValidationService.areAllGuarantorsEligible(gResults));
            } else {
                result.put("guarantors", List.of());
                result.put("allGuarantorsEligible", true);
            }

            result.put("canProceed", memberResult.isEligible());
            return ResponseEntity.ok(ApiResponse.success("Pre-check completed", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('ROLE_LOAN_OFFICER', 'ROLE_TELLER')")
    public ResponseEntity<ApiResponse<Loan>> applyForLoan(
            @Valid @RequestBody LoanApplicationRequest request,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Loan loan = loanService.applyForLoan(request, user);
            
            // Send notifications to guarantors (same as member portal)
            if (request.getGuarantorIds() != null && !request.getGuarantorIds().isEmpty()) {
                com.minet.sacco.entity.Member member = loanService.getMemberById(request.getMemberId());
                for (Long guarantorMemberId : request.getGuarantorIds()) {
                    java.util.Optional<User> guarantorUserOpt = userService.getUserByMemberId(guarantorMemberId);
                    if (guarantorUserOpt.isPresent()) {
                        String message = "Member " + member.getMemberNumber() + " (" + member.getFirstName() + " " + 
                            member.getLastName() + ") has selected you as a guarantor for a loan of KES " + 
                            loan.getAmount() + ". Please review and approve or reject.";
                        notificationService.notifyUser(guarantorUserOpt.get().getId(), message, "GUARANTOR_REQUEST");
                    }
                }
            }
            
            return ResponseEntity.ok(ApiResponse.success("Loan application submitted successfully", loan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{loanId}/validate-approval")
    @PreAuthorize("hasRole('ROLE_CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<LoanApprovalValidationDTO>> validateApproval(@PathVariable Long loanId) {
        try {
            LoanApprovalValidationDTO validation = loanService.validateLoanApproval(loanId);
            return ResponseEntity.ok(ApiResponse.success("Loan approval validation completed", validation));
        } catch (NullPointerException e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("Null pointer error: " + e.getMessage() + " - Stack: " + e.getStackTrace()[0]));
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(ApiResponse.error("Error validating loan: " + e.getMessage()));
        }
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAnyRole('ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<Loan>> approveLoan(
            @Valid @RequestBody LoanApprovalRequest request,
            Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Loan loan = loanService.approveLoan(request, user);
        
        return ResponseEntity.ok(ApiResponse.success("Loan approval processed successfully", loan));
    }

    @PostMapping("/disburse/{loanId}")
    @PreAuthorize("hasRole('ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<Loan>> disburseLoan(
            @PathVariable Long loanId,
            Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Loan loan = loanService.disburseLoan(loanId, user);
        
        // Send notification to Loan Officer
        String notificationMessage = "Loan " + loan.getLoanNumber() + " for member " + 
            loan.getMember().getMemberNumber() + " has been disbursed";
        notificationService.notifyUsersByRole("LOAN_OFFICER", notificationMessage, "LOAN_DISBURSEMENT");
        
        return ResponseEntity.ok(ApiResponse.success("Loan disbursed successfully", loan));
    }

    @PostMapping("/repay")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_TELLER')")
    public ResponseEntity<ApiResponse<LoanRepayment>> makeRepayment(
            @Valid @RequestBody LoanRepaymentRequest request,
            Authentication authentication) {
        User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        LoanRepayment repayment = loanService.makeRepayment(request, user);
        return ResponseEntity.ok(ApiResponse.success("Repayment recorded successfully", repayment));
    }

    @GetMapping("/{loanId}/outstanding")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<BigDecimal>> getOutstandingBalance(@PathVariable Long loanId) {
        BigDecimal outstanding = loanService.getOutstandingBalance(loanId);
        return ResponseEntity.ok(ApiResponse.success("Outstanding balance retrieved", outstanding));
    }

    @GetMapping("/{loanId}/validate-guarantors")
    @PreAuthorize("hasRole('ROLE_CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateGuarantors(@PathVariable Long loanId) {
        Loan loan = loanService.getLoanById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        List<Guarantor> guarantors = loanService.getGuarantorsForLoan(loanId);
        
        if (guarantors.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("hasGuarantors", false);
            response.put("message", "No guarantors assigned to this loan");
            return ResponseEntity.ok(ApiResponse.success("No guarantors to validate", response));
        }

        List<Long> guarantorIds = guarantors.stream()
                .map(g -> g.getMember().getId())
                .toList();

        List<GuarantorValidationService.GuarantorValidationResult> validationResults = 
                guarantorValidationService.validateAllGuarantors(guarantorIds, loan.getAmount());

        Map<String, Object> response = new HashMap<>();
        response.put("hasGuarantors", true);
        response.put("allEligible", guarantorValidationService.areAllGuarantorsEligible(validationResults));
        response.put("validationSummary", guarantorValidationService.getValidationSummary(validationResults));
        response.put("validationDetails", validationResults);

        return ResponseEntity.ok(ApiResponse.success("Guarantor validation completed", response));
    }

    @PostMapping("/{loanId}/guarantor/{guarantorId}/approve")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<String>> approveGuarantee(
            @PathVariable Long loanId,
            @PathVariable Long guarantorId,
            Authentication authentication) {
        try {
            guarantorApprovalService.approveGuarantee(loanId, guarantorId);
            return ResponseEntity.ok(ApiResponse.success("Guarantee approved successfully", "Guarantee approved"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/{loanId}/guarantor/{guarantorId}/reject")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<String>> rejectGuarantee(
            @PathVariable Long loanId,
            @PathVariable Long guarantorId,
            @RequestParam String reason,
            Authentication authentication) {
        try {
            guarantorApprovalService.rejectGuarantee(loanId, guarantorId, reason);
            return ResponseEntity.ok(ApiResponse.success("Guarantee rejected", "Guarantee rejected"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/member/guarantor-requests")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<List<Guarantor>>> getPendingGuarantorRequests(
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            List<Guarantor> requests = guarantorApprovalService.getPendingGuarantorRequests(user.getMemberId());
            return ResponseEntity.ok(ApiResponse.success("Pending guarantor requests retrieved", requests));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}