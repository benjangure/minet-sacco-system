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
import com.minet.sacco.repository.LoanRepository;
import com.minet.sacco.repository.GuarantorRepository;
import com.minet.sacco.service.LoanService;
import com.minet.sacco.service.UserService;
import com.minet.sacco.service.GuarantorValidationService;
import com.minet.sacco.service.GuarantorApprovalService;
import com.minet.sacco.service.GuarantorTrackingService;
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

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private GuarantorTrackingService guarantorTrackingService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllLoans() {
        List<Loan> loans = loanService.getAllLoans();
        List<Map<String, Object>> loansWithGuarantors = new java.util.ArrayList<>();
        
        for (Loan loan : loans) {
            Map<String, Object> loanMap = new java.util.HashMap<>();
            loanMap.put("id", loan.getId());
            loanMap.put("loanNumber", loan.getLoanNumber());
            loanMap.put("member", loan.getMember());
            loanMap.put("loanProduct", loan.getLoanProduct());
            loanMap.put("amount", loan.getAmount());
            loanMap.put("interestRate", loan.getInterestRate());
            loanMap.put("termMonths", loan.getTermMonths());
            loanMap.put("status", loan.getStatus());
            loanMap.put("monthlyRepayment", loan.getMonthlyRepayment());
            loanMap.put("totalInterest", loan.getTotalInterest());
            loanMap.put("totalRepayable", loan.getTotalRepayable());
            loanMap.put("outstandingBalance", loan.getOutstandingBalance());
            loanMap.put("purpose", loan.getPurpose());
            loanMap.put("rejectionReason", loan.getRejectionReason());
            loanMap.put("memberEligibilityStatus", loan.getMemberEligibilityStatus());
            loanMap.put("memberEligibilityErrors", loan.getMemberEligibilityErrors());
            loanMap.put("memberEligibilityWarnings", loan.getMemberEligibilityWarnings());
            loanMap.put("applicationDate", loan.getApplicationDate());
            loanMap.put("approvalDate", loan.getApprovalDate());
            loanMap.put("disbursementDate", loan.getDisbursementDate());
            loanMap.put("createdBy", loan.getCreatedBy());
            loanMap.put("approvedBy", loan.getApprovedBy());
            loanMap.put("disbursedBy", loan.getDisbursedBy());
            
            // Fetch and add guarantors with their guarantee amounts
            List<Guarantor> guarantors = loanService.getGuarantorsForLoan(loan.getId());
            List<Map<String, Object>> guarantorsList = new java.util.ArrayList<>();
            for (Guarantor g : guarantors) {
                Map<String, Object> gMap = new java.util.HashMap<>();
                gMap.put("id", g.getId());
                gMap.put("member", g.getMember());
                gMap.put("status", g.getStatus());
                gMap.put("guaranteeAmount", g.getGuaranteeAmount());
                gMap.put("pledgeAmount", g.getPledgeAmount());
                gMap.put("selfGuarantee", g.isSelfGuarantee());
                gMap.put("createdAt", g.getCreatedAt());
                gMap.put("approvedAt", g.getApprovedAt());
                guarantorsList.add(gMap);
            }
            loanMap.put("guarantors", guarantorsList);
            
            loansWithGuarantors.add(loanMap);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Loans retrieved successfully", loansWithGuarantors));
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
            @RequestParam BigDecimal amount) {
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

            // Note: Guarantor eligibility is checked during loan application with specific guarantee amounts
            result.put("guarantors", List.of());
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
            
            // Notifications are sent by LoanService.applyForLoan() for non-self guarantors
            
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

    /**
     * Mark a loan as defaulted and debit guarantor accounts proportionally
     */
    @PostMapping("/{loanId}/mark-default")
    @PreAuthorize("hasRole('ROLE_CREDIT_COMMITTEE')")
    public ResponseEntity<ApiResponse<Loan>> markLoanAsDefault(
            @PathVariable Long loanId,
            @RequestParam String reason,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Loan loan = loanRepository.findById(loanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found"));
            
            if (loan.getStatus() != Loan.Status.DISBURSED) {
                throw new RuntimeException("Only disbursed loans can be marked as defaulted");
            }
            
            // Calculate default amount (outstanding balance)
            BigDecimal defaultAmount = loanService.getOutstandingBalance(loanId);
            
            // Mark loan as defaulted
            loan.setStatus(Loan.Status.DEFAULTED);
            loanRepository.save(loan);
            
            // Handle default debit for guarantors
            guarantorTrackingService.handleDefaultDebit(loan, defaultAmount, user);
            
            // Send notifications
            java.util.Optional<User> memberUserOpt = userService.getUserByMemberId(loan.getMember().getId());
            if (memberUserOpt.isPresent()) {
                notificationService.notifyUser(memberUserOpt.get().getId(),
                    "Your loan " + loan.getLoanNumber() + " has been marked as defaulted. Amount: KES " + defaultAmount,
                    "LOAN_DEFAULT", loan.getId(), loan.getMember().getId(), "LOAN_DEFAULT");
            }
            
            return ResponseEntity.ok(ApiResponse.success("Loan marked as defaulted successfully", loan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
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

    /**
     * Get all guarantors for a specific loan (visible to staff and member)
     */
    @GetMapping("/{loanId}/guarantors")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_TREASURER', 'ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<List<com.minet.sacco.dto.GuarantorDetailsDTO>>> getGuarantorsForLoan(
            @PathVariable Long loanId) {
        try {
            loanRepository.findById(loanId)
                    .orElseThrow(() -> new RuntimeException("Loan not found"));
            
            List<Guarantor> guarantors = loanService.getGuarantorsForLoan(loanId);
            List<com.minet.sacco.dto.GuarantorDetailsDTO> details = new java.util.ArrayList<>();
            
            for (Guarantor g : guarantors) {
                com.minet.sacco.dto.GuarantorDetailsDTO dto = new com.minet.sacco.dto.GuarantorDetailsDTO();
                dto.setGuarantorId(g.getId());
                dto.setMemberId(g.getMember().getId());
                dto.setMemberNumber(g.getMember().getMemberNumber());
                dto.setFirstName(g.getMember().getFirstName());
                dto.setLastName(g.getMember().getLastName());
                dto.setStatus(g.getStatus().toString());
                dto.setGuaranteeAmount(g.getGuaranteeAmount());
                dto.setFrozenPledge(g.getPledgeAmount());
                dto.setSelfGuarantee(g.isSelfGuarantee());
                dto.setCreatedAt(g.getCreatedAt());
                dto.setApprovedAt(g.getApprovedAt());
                details.add(dto);
            }
            
            return ResponseEntity.ok(ApiResponse.success("Guarantors retrieved successfully", details));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get all guarantees for a member (loans where member is guarantor)
     */
    @GetMapping("/member/guarantees")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMemberGuarantees(
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<Guarantor> guarantees = guarantorRepository.findByMemberId(user.getMemberId());
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            
            for (Guarantor g : guarantees) {
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("guarantorId", g.getId());
                item.put("loanId", g.getLoan().getId());
                item.put("loanNumber", g.getLoan().getLoanNumber());
                item.put("memberName", g.getLoan().getMember().getFirstName() + " " + g.getLoan().getMember().getLastName());
                item.put("memberNumber", g.getLoan().getMember().getMemberNumber());
                item.put("loanAmount", g.getLoan().getAmount());
                item.put("guaranteeAmount", g.getGuaranteeAmount());
                item.put("frozenPledge", g.getPledgeAmount());
                item.put("status", g.getStatus().toString());
                item.put("loanStatus", g.getLoan().getStatus().toString());
                item.put("isSelfGuarantee", g.isSelfGuarantee());
                result.add(item);
            }
            
            return ResponseEntity.ok(ApiResponse.success("Member guarantees retrieved successfully", result));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
