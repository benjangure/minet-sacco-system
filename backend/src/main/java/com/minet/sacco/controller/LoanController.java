package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.dto.LoanApplicationRequest;
import com.minet.sacco.dto.LoanApprovalRequest;
import com.minet.sacco.dto.LoanApprovalValidationDTO;
import com.minet.sacco.dto.LoanRepaymentRequest;
import com.minet.sacco.dto.LoanUpdateRequestDTO;
import com.minet.sacco.dto.TreasurerLoanApprovalRequest;
import com.minet.sacco.entity.Loan;
import com.minet.sacco.entity.LoanRepayment;
import com.minet.sacco.entity.User;
import com.minet.sacco.entity.Guarantor;
import com.minet.sacco.entity.LoanProduct;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.Account;
import com.minet.sacco.entity.LoanTopUpHistory;
import com.minet.sacco.repository.LoanRepository;
import com.minet.sacco.repository.LoanRepaymentRepository;
import com.minet.sacco.repository.GuarantorRepository;
import com.minet.sacco.repository.LoanProductRepository;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.AccountRepository;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.repository.LoanTopUpHistoryRepository;
import com.minet.sacco.service.LoanService;
import com.minet.sacco.service.UserService;
import com.minet.sacco.service.GuarantorValidationService;
import com.minet.sacco.service.GuarantorApprovalService;
import com.minet.sacco.service.GuarantorTrackingService;
import com.minet.sacco.service.NotificationService;
import com.minet.sacco.service.EligibilityCalculationService;
import jakarta.validation.Valid;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

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
    private EligibilityCalculationService eligibilityCalculationService;

    @Autowired
    private LoanProductRepository loanProductRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GuarantorApprovalService guarantorApprovalService;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanRepaymentRepository loanRepaymentRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private GuarantorTrackingService guarantorTrackingService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanTopUpHistoryRepository loanTopUpHistoryRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<Object>> getAllLoans(
            @RequestParam(required = false, defaultValue = "false") boolean paginated,
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "50") int size) {
        
        if (paginated) {
            // Return paginated response
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, size, 
                    org.springframework.data.domain.Sort.by("createdAt").descending());
            org.springframework.data.domain.Page<Loan> loanPage = loanService.getAllLoansPaginated(pageable);
            
            // Build loan maps with guarantors for the current page
            List<Map<String, Object>> loansWithGuarantors = buildLoanMapsWithBatch(loanPage.getContent());
            
            // Create pagination metadata
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("content", loansWithGuarantors);
            response.put("currentPage", loanPage.getNumber());
            response.put("totalItems", loanPage.getTotalElements());
            response.put("totalPages", loanPage.getTotalPages());
            response.put("pageSize", loanPage.getSize());
            response.put("hasNext", loanPage.hasNext());
            response.put("hasPrevious", loanPage.hasPrevious());
            
            return ResponseEntity.ok()
                    .cacheControl(org.springframework.http.CacheControl.maxAge(2, java.util.concurrent.TimeUnit.MINUTES))
                    .body(ApiResponse.success("Loans retrieved successfully (paginated)", response));
        } else {
            // Return full list (cached for 3 minutes)
            List<Loan> loans = loanService.getAllLoans();
            List<Map<String, Object>> loansWithGuarantors = buildLoanMapsWithBatch(loans);
            return ResponseEntity.ok()
                    .cacheControl(org.springframework.http.CacheControl.maxAge(3, java.util.concurrent.TimeUnit.MINUTES))
                    .body(ApiResponse.success("Loans retrieved successfully", loansWithGuarantors));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLoanById(@PathVariable Long id) {
        Optional<Loan> loanOpt = loanService.getLoanById(id);
        if (!loanOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        
        Loan loan = loanOpt.get();
        Map<String, Object> loanMap = buildLoanMap(loan);
        
        return ResponseEntity.ok(ApiResponse.success("Loan found", loanMap));
    }

    /**
     * Get top-up history for a specific loan
     */
    @GetMapping("/{id}/topup-history")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLoanTopUpHistory(@PathVariable Long id) {
        List<LoanTopUpHistory> history = loanTopUpHistoryRepository.findByLoanIdOrderByTopupDateDesc(id);
        
        List<Map<String, Object>> historyList = new ArrayList<>();
        for (LoanTopUpHistory topUp : history) {
            Map<String, Object> topUpMap = new HashMap<>();
            topUpMap.put("id", topUp.getId());
            topUpMap.put("amount", topUp.getTopupAmount());
            topUpMap.put("topupDate", topUp.getTopupDate());
            topUpMap.put("outstandingBeforeTopup", topUp.getOutstandingBeforeTopup());
            topUpMap.put("outstandingAfterTopup", topUp.getOutstandingAfterTopup());
            topUpMap.put("principalPaidBeforeTopup", topUp.getPrincipalPaidBeforeTopup());
            topUpMap.put("newGuarantorsAdded", topUp.getNewGuarantorsAdded());
            topUpMap.put("purpose", topUp.getNotes());
            
            if (topUp.getProcessedBy() != null) {
                topUpMap.put("processedBy", topUp.getProcessedBy().getUsername());
            }
            
            historyList.add(topUpMap);
        }
        
        return ResponseEntity.ok(ApiResponse.success("Top-up history retrieved", historyList));
    }

    @GetMapping("/member/{memberId}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLoansByMemberId(@PathVariable Long memberId) {
        List<Loan> loans = loanService.getLoansByMemberId(memberId);
        List<Map<String, Object>> loansWithDynamicInterest = buildLoanMapsWithBatch(loans);
        return ResponseEntity.ok(ApiResponse.success("Loans retrieved successfully", loansWithDynamicInterest));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_TREASURER', 'ROLE_LOAN_OFFICER', 'ROLE_CREDIT_COMMITTEE', 'ROLE_AUDITOR')")
    @org.springframework.cache.annotation.Cacheable(value = "loansByStatusList", key = "#status", unless = "#result == null")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLoansByStatus(@PathVariable String status) {
        List<Loan> loans = loanService.getLoansByStatus(Loan.Status.valueOf(status));
        List<Map<String, Object>> loansWithDynamicInterest = buildLoanMapsWithBatch(loans);
        return ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.maxAge(3, java.util.concurrent.TimeUnit.MINUTES))
                .body(ApiResponse.success("Loans retrieved successfully", loansWithDynamicInterest));
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

    @GetMapping("/product/{productId}/available-capacity")
    @PreAuthorize("hasAnyRole('ROLE_MEMBER', 'ROLE_LOAN_OFFICER', 'ROLE_TELLER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAvailableBorrowingCapacity(
            @PathVariable Long productId,
            Authentication authentication) {
        try {
            Map<String, Object> result = new HashMap<>();

            // Get current user
            User user = userService.getUserByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Get member ID from user
            Long memberId = user.getMemberId();
            if (memberId == null) {
                throw new RuntimeException("User is not linked to a member");
            }

            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new RuntimeException("Member not found"));

            // Get loan product
            LoanProduct product = loanProductRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Loan product not found"));

            // If no max limit set, return null
            if (product.getMaxTotalBorrowingLimit() == null || 
                product.getMaxTotalBorrowingLimit().compareTo(BigDecimal.ZERO) <= 0) {
                result.put("hasLimit", false);
                result.put("availableCapacity", null);
                return ResponseEntity.ok(ApiResponse.success("No borrowing limit set for this product", result));
            }

            // Calculate current outstanding
            BigDecimal currentOutstanding = eligibilityCalculationService
                    .getOutstandingBalanceByProduct(member, productId);

            // Calculate available
            BigDecimal availableCapacity = product.getMaxTotalBorrowingLimit()
                    .subtract(currentOutstanding);

            result.put("hasLimit", true);
            result.put("maxLimit", product.getMaxTotalBorrowingLimit());
            result.put("currentOutstanding", currentOutstanding);
            result.put("availableCapacity", availableCapacity);

            return ResponseEntity.ok(ApiResponse.success("Available capacity retrieved", result));
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

    @PostMapping("/apply-on-behalf")
    @PreAuthorize("hasRole('ROLE_LOAN_OFFICER')")
    public ResponseEntity<ApiResponse<Loan>> applyForLoanOnBehalf(
            @Valid @RequestBody LoanApplicationRequest request,
            Authentication authentication) {
        try {
            User loanOfficer = userService.getUserByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Loan loan = loanService.applyForLoanOnBehalf(request, loanOfficer);
            
            return ResponseEntity.ok(ApiResponse.success("Loan application submitted successfully on behalf of member", loan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/validate-member-eligibility")
    @PreAuthorize("hasRole('ROLE_LOAN_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateMemberEligibility(
            @RequestParam Long memberId,
            @RequestParam BigDecimal loanAmount,
            @RequestParam(required = false) BigDecimal selfGuaranteeAmount) {
        try {
            Map<String, Object> validation = loanService.validateMemberEligibilityForLoanOfficer(
                memberId, loanAmount, selfGuaranteeAmount != null ? selfGuaranteeAmount : BigDecimal.ZERO);
            return ResponseEntity.ok(ApiResponse.success("Member eligibility validated", validation));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/validate-guarantor-eligibility")
    @PreAuthorize("hasRole('ROLE_LOAN_OFFICER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateGuarantorEligibility(
            @RequestParam Long guarantorMemberId,
            @RequestParam BigDecimal guaranteeAmount) {
        try {
            Map<String, Object> validation = loanService.validateGuarantorEligibilityForLoanOfficer(
                guarantorMemberId, guaranteeAmount);
            return ResponseEntity.ok(ApiResponse.success("Guarantor eligibility validated", validation));
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
        
        // Send notification to member about loan disbursement
        Optional<User> memberUserOpt = userService.getUserByMemberId(loan.getMember().getId());
        if (memberUserOpt.isPresent()) {
            String memberMessage = String.format(
                "Your loan application for KES %,.2f has been approved and disbursed to your bank account (%s - %s). Loan Number: %s",
                loan.getAmount(),
                loan.getMember().getBankName(),
                loan.getMember().getBankAccountNumber(),
                loan.getLoanNumber()
            );
            notificationService.notifyUser(
                memberUserOpt.get().getId(),
                memberMessage,
                "LOAN_DISBURSED",
                loan.getId(),
                loan.getMember().getId(),
                "LOAN_DISBURSEMENT"
            );
        }
        
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
                dto.setPreviousGuaranteeAmount(g.getPreviousGuaranteeAmount());
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

    /**
     * Replace a rejected guarantor with a new one
     * Only works when loan is in PENDING_GUARANTOR_REPLACEMENT status
     */
    @PostMapping("/{loanId}/replace-guarantor")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<Loan>> replaceGuarantor(
            @PathVariable Long loanId,
            @RequestParam Long oldGuarantorId,
            @RequestParam Long newGuarantorMemberId,
            @RequestParam BigDecimal newGuaranteeAmount,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            loanService.replaceGuarantor(loanId, oldGuarantorId, newGuarantorMemberId, 
                newGuaranteeAmount, user);
            
            Loan loan = loanService.getLoanById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
            
            return ResponseEntity.ok(ApiResponse.success("Guarantor replaced successfully", loan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Reduce loan amount when guarantor rejects
     * Loan moves back to PENDING_CREDIT_COMMITTEE for re-approval
     */
    @PostMapping("/{loanId}/reduce-amount")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<Loan>> reduceLoanAmount(
            @PathVariable Long loanId,
            @RequestParam BigDecimal newAmount,
            @RequestParam String reason,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            loanService.reduceLoanAmount(loanId, newAmount, reason, user);
            
            Loan loan = loanService.getLoanById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
            
            return ResponseEntity.ok(ApiResponse.success("Loan amount reduced successfully", loan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Withdraw loan application when guarantor rejects
     * Loan is marked as REJECTED and all guarantors marked as DECLINED
     */
    @PostMapping("/{loanId}/withdraw")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<Loan>> withdrawLoanApplication(
            @PathVariable Long loanId,
            @RequestParam String reason,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            loanService.withdrawLoanApplication(loanId, reason, user);
            
            Loan loan = loanService.getLoanById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
            
            return ResponseEntity.ok(ApiResponse.success("Loan application withdrawn successfully", loan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Reassign guarantors after loan amount reduction
     * Member provides new guarantor assignments with new guarantee amounts
     * Validates that total guarantees cover the new loan amount
     * Creates new guarantor approval requests
     */
    @PostMapping("/{loanId}/reassign-guarantors")
    @PreAuthorize("hasRole('ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<Loan>> reassignGuarantors(
            @PathVariable Long loanId,
            @RequestBody List<Map<String, Object>> guarantorAssignments,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            loanService.reassignGuarantors(loanId, guarantorAssignments, user);
            
            Loan loan = loanService.getLoanById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
            
            return ResponseEntity.ok(ApiResponse.success("Guarantors reassigned successfully", loan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * RESTRUCTURED: Treasurer sets interest rate and approves/rejects loan
     * Called when loan is in PENDING_TREASURER status
     * Only accessible by TREASURER role
     */
    @PostMapping("/{loanId}/treasurer/set-interest")
    @PreAuthorize("hasRole('ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<Loan>> treasurerSetInterest(
            @PathVariable Long loanId,
            @Valid @RequestBody com.minet.sacco.dto.TreasurerLoanApprovalRequest request,
            Authentication authentication) {
        try {
            User treasurer = userService.getUserByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Treasurer user not found"));
            
            // Ensure request has the correct loan ID
            request.setLoanId(loanId);
            
            Loan loan = loanService.treasurerSetInterestAndApprove(request, treasurer);
            
            return ResponseEntity.ok(ApiResponse.success(
                request.getApproved() ? "Loan approved with interest set" : "Loan rejected",
                loan
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Load data needed for reassigning guarantors on a loan
     * Returns: current guarantors with details, member eligibility info, and available members for selection
     * Used by UI to populate the reassign guarantors dialog
     */
    @GetMapping("/{loanId}/reassign-guarantors-data")
    @PreAuthorize("hasAnyRole('ROLE_ADMIN', 'ROLE_MEMBER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReassignGuarantorsData(
            @PathVariable Long loanId,
            Authentication authentication) {
        try {
            Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
            
            Map<String, Object> responseData = new HashMap<>();
            
            // 1. Load current guarantors with their details and amounts
            List<Guarantor> currentGuarantors = loanService.getGuarantorsForLoan(loanId);
            List<Map<String, Object>> guarantorsList = new ArrayList<>();
            BigDecimal totalCurrentGuarantee = BigDecimal.ZERO;
            
            for (Guarantor g : currentGuarantors) {
                Map<String, Object> gMap = new HashMap<>();
                gMap.put("guarantorId", g.getId());
                gMap.put("memberId", g.getMember().getId());
                gMap.put("memberNumber", g.getMember().getMemberNumber());
                gMap.put("firstName", g.getMember().getFirstName());
                gMap.put("lastName", g.getMember().getLastName());
                gMap.put("employeeId", g.getMember().getEmployeeId());
                gMap.put("status", g.getStatus().toString());
                gMap.put("guaranteeAmount", g.getGuaranteeAmount());
                gMap.put("pledgeAmount", g.getPledgeAmount());
                gMap.put("selfGuarantee", g.isSelfGuarantee());
                gMap.put("createdAt", g.getCreatedAt());
                gMap.put("approvedAt", g.getApprovedAt());
                guarantorsList.add(gMap);
                
                if (g.getGuaranteeAmount() != null) {
                    totalCurrentGuarantee = totalCurrentGuarantee.add(g.getGuaranteeAmount());
                }
            }
            
            responseData.put("currentGuarantors", guarantorsList);
            responseData.put("totalCurrentGuarantee", totalCurrentGuarantee);
            responseData.put("loanAmount", loan.getAmount());
            responseData.put("outstandingBalance", loan.getOutstandingBalance());
            
            // 2. Member eligibility info
            Map<String, Object> memberInfo = new HashMap<>();
            memberInfo.put("memberId", loan.getMember().getId());
            memberInfo.put("memberNumber", loan.getMember().getMemberNumber());
            memberInfo.put("firstName", loan.getMember().getFirstName());
            memberInfo.put("lastName", loan.getMember().getLastName());
            
            // Get member eligibility details
            try {
                com.minet.sacco.service.LoanEligibilityValidator.EligibilityResult eligibilityResult =
                    loanService.checkMemberEligibility(loan.getMember(), loan.getAmount());
                memberInfo.put("eligible", eligibilityResult.isEligible());
                memberInfo.put("errors", eligibilityResult.getErrors());
                memberInfo.put("warnings", eligibilityResult.getWarnings());
                memberInfo.put("savingsBalance", eligibilityResult.getSavingsBalance());
                memberInfo.put("sharesBalance", eligibilityResult.getSharesBalance());
                memberInfo.put("totalBalance", eligibilityResult.getTotalBalance());
                memberInfo.put("activeLoans", eligibilityResult.getActiveLoans());
            } catch (Exception e) {
                memberInfo.put("eligible", false);
                memberInfo.put("errors", List.of("Could not verify member eligibility: " + e.getMessage()));
                memberInfo.put("warnings", new ArrayList<>());
            }
            
            responseData.put("memberInfo", memberInfo);
            
            // 3. Available active members for selection (for reassigning)
            List<Member> activeMembers = memberRepository.findByStatus(Member.Status.ACTIVE);
            List<Map<String, Object>> availableMembers = new ArrayList<>();
            for (Member member : activeMembers) {
                Map<String, Object> mMap = new HashMap<>();
                mMap.put("memberId", member.getId());
                mMap.put("memberNumber", member.getMemberNumber());
                mMap.put("firstName", member.getFirstName());
                mMap.put("lastName", member.getLastName());
                mMap.put("employeeId", member.getEmployeeId());
                
                // Get member's savings balance for display
                Optional<Account> savingsAccount = 
                    accountRepository.findByMemberIdAndAccountType(member.getId(), Account.AccountType.SAVINGS);
                if (savingsAccount.isPresent()) {
                    mMap.put("savingsBalance", savingsAccount.get().getBalance());
                    mMap.put("frozenSavings", savingsAccount.get().getFrozenSavings());
                    mMap.put("availableSavings", savingsAccount.get().getBalance().subtract(savingsAccount.get().getFrozenSavings()));
                } else {
                    mMap.put("savingsBalance", BigDecimal.ZERO);
                    mMap.put("frozenSavings", BigDecimal.ZERO);
                    mMap.put("availableSavings", BigDecimal.ZERO);
                }
                
                availableMembers.add(mMap);
            }
            
            responseData.put("availableMembers", availableMembers);
            
            return ResponseEntity.ok(ApiResponse.success("Reassign guarantors data retrieved", responseData));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * PHASE A: Individual field edit (low-risk, no guarantor changes)
     * Editable fields only: loanStatus, disbursementDate, interestRate, outstandingBalance, purpose
     * This is kept completely separate from Phase B to prevent mixing concerns
     */
    @PutMapping("/{loanId}/fields/update")
    @PreAuthorize("hasRole('ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateLoanFieldsOnly(
            @PathVariable Long loanId,
            @RequestBody com.minet.sacco.dto.LoanFieldUpdateDTO fieldUpdate,
            Authentication authentication) {
        try {
            Optional<User> userOptional = userService.getUserByUsername(authentication.getName());
            if (!userOptional.isPresent()) {
                throw new RuntimeException("User not found");
            }
            User user = userOptional.get();
            Loan updatedLoan = loanService.updateLoanFieldsOnly(loanId, fieldUpdate, user);
            
            // Build response with updated loan data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", updatedLoan.getId());
            responseData.put("loanNumber", updatedLoan.getLoanNumber());
            responseData.put("status", updatedLoan.getStatus());
            responseData.put("disbursementDate", updatedLoan.getDisbursementDate());
            responseData.put("interestRate", updatedLoan.getInterestRate());
            responseData.put("outstandingBalance", updatedLoan.getOutstandingBalance());
            responseData.put("purpose", updatedLoan.getPurpose());
            responseData.put("message", "Loan fields updated successfully");
            
            return ResponseEntity.ok(ApiResponse.success("Loan fields updated successfully", responseData));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * BATCH version — fetches all guarantors and interest totals in 2 queries
     * for the entire list, instead of 2 queries per loan (N+1 fix).
     */
    private List<Map<String, Object>> buildLoanMapsWithBatch(List<Loan> loans) {
        if (loans == null || loans.isEmpty()) return new ArrayList<>();

        List<Long> loanIds = loans.stream().map(Loan::getId).collect(java.util.stream.Collectors.toList());

        // 1 query: fetch all guarantors for all loans at once
        List<Guarantor> allGuarantors = guarantorRepository.findByLoanIdIn(loanIds);
        Map<Long, List<Guarantor>> guarantorsByLoanId = allGuarantors.stream()
            .collect(java.util.stream.Collectors.groupingBy(g -> g.getLoan().getId()));

        // 1 query: fetch interest collected totals for all loans at once
        List<Object[]> interestRows = loanRepaymentRepository.getTotalInterestCollectedForLoans(loanIds);
        Map<Long, BigDecimal> interestByLoanId = new HashMap<>();
        for (Object[] row : interestRows) {
            Long loanId = ((Number) row[0]).longValue();
            BigDecimal total = (BigDecimal) row[1];
            interestByLoanId.put(loanId, total != null ? total : BigDecimal.ZERO);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Loan loan : loans) {
            Map<String, Object> loanMap = buildLoanMapCore(loan,
                guarantorsByLoanId.getOrDefault(loan.getId(), new ArrayList<>()),
                interestByLoanId.getOrDefault(loan.getId(), BigDecimal.ZERO));
            result.add(loanMap);
        }
        return result;
    }

    /**
     * Helper method to build a standardized loan map with all fields including dynamic calculations.
     * Used across all loan endpoints to ensure consistent data.
     */
    private Map<String, Object> buildLoanMap(Loan loan) {
        // Single-loan fallback: fetch per-loan (used for individual loan endpoints)
        BigDecimal postMigrationInterest = loanRepaymentRepository.getTotalInterestCollected(loan.getId());
        List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());
        return buildLoanMapCore(loan, guarantors, postMigrationInterest);
    }

    /**
     * Core build — accepts pre-fetched guarantors and interest to avoid N+1 in batch calls.
     */
    private Map<String, Object> buildLoanMapCore(Loan loan, List<Guarantor> guarantors, BigDecimal postMigrationInterest) {
        Map<String, Object> loanMap = new HashMap<>();
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
        
        // Calculate dynamic interest collected based on override flag
        BigDecimal totalInterestCollected;
        if (Boolean.TRUE.equals(loan.getInterestCollectedManualOverride())) {
            // Treasurer manually set this value - use it EXACTLY as-is
            totalInterestCollected = loan.getInterestCollected() != null ? loan.getInterestCollected() : BigDecimal.ZERO;
        } else {
            // Automatic calculation: migration snapshot + post-migration repayments
            // postMigrationInterest is passed in (pre-fetched in batch) — no extra DB call
            BigDecimal migrationInterest = loan.getInterestCollected() != null ? loan.getInterestCollected() : BigDecimal.ZERO;
            totalInterestCollected = migrationInterest.add(postMigrationInterest);
        }
        loanMap.put("interestCollected", totalInterestCollected);
        
        loanMap.put("interestRemaining", loan.getInterestRemaining());
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
        loanMap.put("migrationStatus", loan.getMigrationStatus());
        loanMap.put("createdBy", loan.getCreatedBy());
        loanMap.put("approvedBy", loan.getApprovedBy());
        loanMap.put("disbursedBy", loan.getDisbursedBy());
        
        // Top-up fields
        loanMap.put("totalTopupAmount", loan.getTotalTopupAmount() != null ? loan.getTotalTopupAmount() : BigDecimal.ZERO);
        loanMap.put("topupCount", loan.getTopupCount() != null ? loan.getTopupCount() : 0);
        loanMap.put("lastTopupDate", loan.getLastTopupDate());
        loanMap.put("principalBeforeTopup", loan.getPrincipalBeforeTopup());
        
        // Principal repaid — Check for manual override first
        BigDecimal principalRepaid;
        if (Boolean.TRUE.equals(loan.getPrincipalRepaidManualOverride()) && loan.getPrincipalRepaid() != null) {
            // Treasurer manually set this value - use it EXACTLY as-is
            principalRepaid = loan.getPrincipalRepaid();
        } else {
            // Automatic calculation: (Principal + Top-Ups) - Outstanding Balance
            BigDecimal principal = loan.getAmount() != null ? loan.getAmount() : BigDecimal.ZERO;
            BigDecimal outstanding = loan.getOutstandingBalance() != null ? loan.getOutstandingBalance() : BigDecimal.ZERO;
            BigDecimal totalTopups = loan.getTotalTopupAmount() != null ? loan.getTotalTopupAmount() : BigDecimal.ZERO;
            BigDecimal totalLoanAmount = principal.add(totalTopups);
            principalRepaid = totalLoanAmount.subtract(outstanding);
        }
        
        // Calculate repayment percentage: (Principal Repaid / Original Principal) * 100
        // Percentage is based on ORIGINAL principal, not including top-ups
        BigDecimal principal = loan.getAmount() != null ? loan.getAmount() : BigDecimal.ZERO;
        BigDecimal repaymentPercentage = BigDecimal.ZERO;
        if (principal.compareTo(BigDecimal.ZERO) > 0) {
            repaymentPercentage = principalRepaid
                    .divide(principal, 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
        }
        
        loanMap.put("principalRepaid", principalRepaid);
        loanMap.put("repaymentPercentage", repaymentPercentage);
        
        // Total Repaid = Principal Repaid + Interest Collected
        BigDecimal totalRepaid = principalRepaid.add(totalInterestCollected);
        loanMap.put("totalRepaid", totalRepaid);
        
        // Use pre-fetched guarantors (passed in from batch or single-loan fetch)
        List<Map<String, Object>> guarantorsList = new ArrayList<>();
        for (Guarantor g : guarantors) {
            Map<String, Object> gMap = new HashMap<>();
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
        
        return loanMap;
    }

    /**
     * Delete a loan - Only treasurers can delete loans
     * Handles cleanup of related entities (guarantors, transactions, repayments)
     */
    @DeleteMapping("/{loanId}")
    @PreAuthorize("hasRole('ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<String>> deleteLoan(
            @PathVariable Long loanId,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            loanService.deleteLoan(loanId, user);
            return ResponseEntity.ok(ApiResponse.success("Loan deleted successfully", "Loan has been removed from the system"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update loan principal and outstanding balance - Only treasurers can update
     * Recalculates all loan financials and updates related entities
     */
    @PutMapping("/{loanId}/update-financials")
    @PreAuthorize("hasRole('ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<Loan>> updateLoanFinancials(
            @PathVariable Long loanId,
            @RequestParam(required = false) BigDecimal principal,
            @RequestParam(required = false) BigDecimal outstandingBalance,
            @RequestParam(required = false) BigDecimal interestRate,
            @RequestParam(required = false) Integer termMonths,
            @RequestParam(required = false) BigDecimal totalInterest,
            @RequestParam(required = false) BigDecimal totalRepayable,
            @RequestParam(required = false) BigDecimal monthlyRepayment,
            @RequestParam(required = false) BigDecimal interestCollected,
            @RequestParam(required = false) BigDecimal principalRepaid,
            @RequestParam(required = false) String reason,
            Authentication authentication) {
        try {
            User user = userService.getUserByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            Loan updatedLoan = loanService.updateLoanFinancials(
                loanId, principal, outstandingBalance, interestRate, termMonths, 
                totalInterest, totalRepayable, monthlyRepayment, interestCollected, principalRepaid, reason, user
            );
            return ResponseEntity.ok(ApiResponse.success("Loan financials updated successfully", updatedLoan));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== LOAN TOP-UP ENDPOINTS ====================

    /**
     * Preview loan top-up calculation
     */
    @GetMapping("/{loanId}/topup-preview")
    @PreAuthorize("hasAnyRole('LOAN_OFFICER', 'TREASURER', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.minet.sacco.dto.LoanTopUpPreviewResponse>> previewLoanTopUp(
            @PathVariable Long loanId,
            @RequestParam BigDecimal amount) {
        try {
            com.minet.sacco.dto.LoanTopUpPreviewResponse preview = loanService.previewLoanTopUp(loanId, amount);
            return ResponseEntity.ok(ApiResponse.success("Top-up preview calculated", preview));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to preview top-up: " + e.getMessage()));
        }
    }

    /**
     * Process loan top-up
     */
    @PostMapping("/{loanId}/add-topup")
    @PreAuthorize("hasAnyRole('TREASURER', 'ADMIN')")
    public ResponseEntity<ApiResponse<com.minet.sacco.dto.LoanTopUpResponse>> addLoanTopUp(
            @PathVariable Long loanId,
            @Valid @RequestBody com.minet.sacco.dto.LoanTopUpRequest request) {
        try {
            // Get current user
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            com.minet.sacco.dto.LoanTopUpResponse response = loanService.processLoanTopUp(loanId, request, currentUser);
            return ResponseEntity.ok(ApiResponse.success("Loan top-up processed successfully", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to process top-up: " + e.getMessage()));
        }
    }

    /**
     * Get loan top-up history
     */
    /**
     * Update a loan top-up (Treasurer only)
     */
    @PutMapping("/topup/{topupId}")
    @PreAuthorize("hasRole('ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<LoanTopUpHistory>> updateLoanTopUp(
            @PathVariable Long topupId,
            @RequestParam BigDecimal topupAmount,
            @RequestParam(required = false) String purpose) {
        try {
            LoanTopUpHistory topup = loanService.updateLoanTopUp(topupId, topupAmount, purpose);
            return ResponseEntity.ok(ApiResponse.success("Top-up updated successfully", topup));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to update top-up: " + e.getMessage()));
        }
    }

    /**
     * Delete a loan top-up (Treasurer only)
     */
    @DeleteMapping("/topup/{topupId}")
    @PreAuthorize("hasRole('ROLE_TREASURER')")
    public ResponseEntity<ApiResponse<Void>> deleteLoanTopUp(@PathVariable Long topupId) {
        try {
            loanService.deleteLoanTopUp(topupId);
            return ResponseEntity.ok(ApiResponse.success("Top-up deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Failed to delete top-up: " + e.getMessage()));
        }
    }
}
