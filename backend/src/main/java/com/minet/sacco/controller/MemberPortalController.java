package com.minet.sacco.controller;

import com.minet.sacco.dto.*;
import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import com.minet.sacco.security.JwtUtil;
import com.minet.sacco.service.GuarantorValidationService;
import com.minet.sacco.service.LoanService;
import com.minet.sacco.service.UserService;
import com.minet.sacco.service.NotificationService;
import com.minet.sacco.service.DepositRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.io.File;
import org.springframework.beans.factory.annotation.Value;

@RestController
@RequestMapping("/api/member")
@CrossOrigin
public class MemberPortalController {

    @Value("${deposit.upload.directory:uploads/deposits}")
    private String depositUploadDirectory;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Get current authenticated member
     */
    private Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent()) {
            throw new RuntimeException("User not found: " + username);
        }
        
        User user = userOpt.get();
        if (user.getMemberId() == null) {
            throw new RuntimeException("User account is not linked to a member. Please contact support. (User ID: " + user.getId() + ")");
        }
        
        Optional<Member> member = memberRepository.findById(user.getMemberId());
        if (!member.isPresent()) {
            throw new RuntimeException("Member record not found for ID: " + user.getMemberId());
        }
        
        return member.get();
    }

    /**
     * Get member dashboard with summary information
     */
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard() {
        try {
            Member member = getCurrentMember();
            
            // Get account balances
            BigDecimal savingsBalance = BigDecimal.ZERO;
            BigDecimal sharesBalance = BigDecimal.ZERO;
            
            Optional<Account> savingsAccount = accountRepository.findByMemberIdAndAccountType(
                member.getId(), Account.AccountType.SAVINGS
            );
            if (savingsAccount.isPresent()) {
                savingsBalance = savingsAccount.get().getBalance();
            }
            
            Optional<Account> sharesAccount = accountRepository.findByMemberIdAndAccountType(
                member.getId(), Account.AccountType.SHARES
            );
            if (sharesAccount.isPresent()) {
                sharesBalance = sharesAccount.get().getBalance();
            }
            
            BigDecimal totalBalance = savingsBalance.add(sharesBalance);
            
            // Get active loans count
            List<Loan> activeLoans = loanRepository.findByMemberIdAndStatus(
                member.getId(), Loan.Status.DISBURSED
            );
            Integer activeLoanCount = activeLoans.size();
            
            // Get total outstanding
            BigDecimal totalOutstanding = activeLoans.stream()
                .map(Loan::getOutstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            // Get pending applications
            List<Loan> pendingLoans = loanRepository.findByMemberIdAndStatus(
                member.getId(), Loan.Status.PENDING
            );
            Integer pendingCount = pendingLoans.size();
            
            // Get recent transactions (last 5)
            List<Transaction> recentTransactions = transactionRepository
                .findByAccountMemberIdOrderByTransactionDateDesc(member.getId())
                .stream()
                .limit(5)
                .collect(Collectors.toList());
            
            List<RecentTransactionDTO> transactionDTOs = recentTransactions.stream()
                .map(t -> new RecentTransactionDTO(
                    t.getId(),
                    t.getTransactionType().toString(),
                    t.getAmount(),
                    t.getDescription(),
                    t.getTransactionDate(),
                    t.getAccount().getAccountType().toString()
                ))
                .collect(Collectors.toList());
            
            MemberDashboardDTO dashboard = new MemberDashboardDTO(
                member.getMemberNumber(),
                member.getFirstName(),
                member.getLastName(),
                savingsBalance,
                sharesBalance,
                totalBalance,
                activeLoanCount,
                totalOutstanding,
                pendingCount
            );
            dashboard.setRecentTransactions(transactionDTOs);
            
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get member profile information
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile() {
        try {
            Member member = getCurrentMember();
            return ResponseEntity.ok(member);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get member's account balances
     */
    @GetMapping("/accounts")
    public ResponseEntity<?> getAccounts() {
        try {
            Member member = getCurrentMember();
            List<Account> accounts = accountRepository.findByMemberId(member.getId());
            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get member's transaction history
     */
    @GetMapping("/transactions")
    public ResponseEntity<?> getTransactions() {
        try {
            Member member = getCurrentMember();
            List<Transaction> transactions = transactionRepository
                .findByAccountMemberIdOrderByTransactionDateDesc(member.getId());
            
            List<RecentTransactionDTO> transactionDTOs = transactions.stream()
                .map(t -> new RecentTransactionDTO(
                    t.getId(),
                    t.getTransactionType().toString(),
                    t.getAmount(),
                    t.getDescription(),
                    t.getTransactionDate(),
                    t.getAccount().getAccountType().toString()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(transactionDTOs);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get member's loans
     */
    @GetMapping("/loans")
    public ResponseEntity<?> getLoans() {
        try {
            Member member = getCurrentMember();
            List<Loan> loans = loanRepository.findByMemberId(member.getId());
            return ResponseEntity.ok(loans);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get specific loan details
     */
    @GetMapping("/loans/{id}")
    public ResponseEntity<?> getLoanDetails(@PathVariable Long id) {
        try {
            Member member = getCurrentMember();
            Optional<Loan> loan = loanRepository.findById(id);
            
            if (!loan.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            // Verify member owns this loan
            if (!loan.get().getMember().getId().equals(member.getId())) {
                return ResponseEntity.status(403).body("Unauthorized");
            }
            
            return ResponseEntity.ok(loan.get());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private GuarantorValidationService guarantorValidationService;

    @Autowired
    private LoanProductRepository loanProductRepository;

    @Autowired
    private LoanService loanService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private com.minet.sacco.service.LoanEligibilityRulesService loanEligibilityRulesService;

    @Autowired
    private DepositRequestService depositRequestService;

    /**
     * Member checks their loan eligibility
     */
    @GetMapping("/loan-eligibility")
    public ResponseEntity<?> checkLoanEligibility() {
        try {
            Member member = getCurrentMember();
            
            // Get eligibility result with all calculations
            com.minet.sacco.service.LoanEligibilityValidator.EligibilityResult eligibilityResult = 
                loanService.checkMemberEligibility(member, BigDecimal.ZERO);
            
            // Determine what to show based on loan status
            BigDecimal displayAmount;
            String displayLabel;
            
            if (eligibilityResult.getTotalOutstanding().compareTo(BigDecimal.ZERO) > 0) {
                // Has outstanding loans: show REMAINING eligible
                displayAmount = eligibilityResult.getNetEligibleAmount();
                displayLabel = "Remaining Eligible";
            } else {
                // No outstanding loans: show MAX eligible
                displayAmount = eligibilityResult.getMaxEligibleAmount();
                displayLabel = "Eligible Amount";
            }
            
            java.util.Map<String, Object> eligibilityData = new java.util.HashMap<>();
            eligibilityData.put("eligible", eligibilityResult.isEligible());
            eligibilityData.put("displayAmount", displayAmount);
            eligibilityData.put("displayLabel", displayLabel);
            
            // Breakdown fields for display
            eligibilityData.put("baseSavings", eligibilityResult.getBaseSavings());
            eligibilityData.put("totalDisbursed", eligibilityResult.getTotalDisbursed());
            eligibilityData.put("trueSavings", eligibilityResult.getTrueSavings());
            eligibilityData.put("grossEligibility", eligibilityResult.getGrossEligibility());
            eligibilityData.put("totalOutstanding", eligibilityResult.getTotalOutstanding());
            eligibilityData.put("netEligibleAmount", eligibilityResult.getNetEligibleAmount());
            
            // Legacy fields for backward compatibility
            eligibilityData.put("currentSavings", eligibilityResult.getSavingsBalance());
            eligibilityData.put("sharesBalance", eligibilityResult.getSharesBalance());
            eligibilityData.put("activeLoans", eligibilityResult.getActiveLoans());
            
            eligibilityData.put("errors", eligibilityResult.getErrors());
            eligibilityData.put("warnings", eligibilityResult.getWarnings());
            
            return ResponseEntity.ok(eligibilityData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/member-by-employee-id/{employeeId}")
    public ResponseEntity<?> getMemberByEmployeeId(@PathVariable String employeeId) {
        try {
            Optional<Member> memberOpt = memberRepository.findByEmployeeId(employeeId);
            if (!memberOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Member member = memberOpt.get();
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("memberId", member.getId());
            response.put("memberNumber", member.getMemberNumber());
            response.put("employeeId", member.getEmployeeId());
            response.put("firstName", member.getFirstName());
            response.put("lastName", member.getLastName());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Search for member by member number (for guarantor lookup)
     */
    @GetMapping("/search/{memberNumber}")
    public ResponseEntity<?> searchMemberByNumber(@PathVariable String memberNumber) {
        try {
            Optional<Member> memberOpt = memberRepository.findByMemberNumber(memberNumber);
            if (!memberOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Member member = memberOpt.get();
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("id", member.getId());
            response.put("memberId", member.getId());
            response.put("memberNumber", member.getMemberNumber());
            response.put("employeeId", member.getEmployeeId());
            response.put("firstName", member.getFirstName());
            response.put("lastName", member.getLastName());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Member applies for a loan with guarantors
     */
    @PostMapping("/apply-loan")
    public ResponseEntity<?> applyForLoan(@RequestBody LoanApplicationRequest request) {
        try {
            Member member = getCurrentMember();
            
            // Set the member ID from the authenticated user
            request.setMemberId(member.getId());
            
            // Get the authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = userService.getUserByUsername(authentication.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Apply for loan
            Loan loan = loanService.applyForLoan(request, user);
            
            // Send notifications to guarantors
            if (request.getGuarantorIds() != null && !request.getGuarantorIds().isEmpty()) {
                for (Long guarantorMemberId : request.getGuarantorIds()) {
                    Optional<User> guarantorUserOpt = userService.getUserByMemberId(guarantorMemberId);
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

    /**
     * Get pending guarantor requests for current member
     */
    @GetMapping("/guarantor-requests/pending")
    public ResponseEntity<?> getPendingGuarantorRequests() {
        try {
            Member member = getCurrentMember();
            List<Guarantor> pendingRequests = guarantorRepository.findByMemberIdAndStatus(
                member.getId(), Guarantor.Status.PENDING
            );
            
            List<GuarantorRequestDTO> dtos = pendingRequests.stream()
                .map(g -> new GuarantorRequestDTO(
                    g.getId(),
                    g.getLoan().getId(),
                    g.getMember().getId(),
                    g.getMember().getFirstName() + " " + g.getMember().getLastName(),
                    g.getMember().getPhone(),
                    g.getStatus().toString(),
                    g.getPledgeAmount(),
                    g.getCreatedAt(),
                    g.getLoan().getLoanProduct().getName(),
                    g.getLoan().getAmount(),
                    g.getLoan().getMember().getFirstName() + " " + g.getLoan().getMember().getLastName()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Approve a guarantor request
     */
    @PostMapping("/guarantor-requests/{requestId}/approve")
    public ResponseEntity<?> approveGuarantorRequest(@PathVariable Long requestId) {
        try {
            Member member = getCurrentMember();
            Optional<Guarantor> guarantorOpt = guarantorRepository.findById(requestId);
            
            if (!guarantorOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Guarantor guarantor = guarantorOpt.get();
            
            // Verify this is the guarantor
            if (!guarantor.getMember().getId().equals(member.getId())) {
                return ResponseEntity.status(403).body("Unauthorized");
            }
            
            // Use LoanService to approve guarantorship (sends notifications)
            loanService.approveGuarantorship(requestId, true, null);
            
            return ResponseEntity.ok(ApiResponse.success("Guarantor request approved successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Reject a guarantor request
     */
    @PostMapping("/guarantor-requests/{requestId}/reject")
    public ResponseEntity<?> rejectGuarantorRequest(@PathVariable Long requestId, @RequestParam(required = false) String reason) {
        try {
            Member member = getCurrentMember();
            Optional<Guarantor> guarantorOpt = guarantorRepository.findById(requestId);
            
            if (!guarantorOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Guarantor guarantor = guarantorOpt.get();
            
            // Verify this is the guarantor
            if (!guarantor.getMember().getId().equals(member.getId())) {
                return ResponseEntity.status(403).body("Unauthorized");
            }
            
            // Use LoanService to reject guarantorship (sends notifications)
            loanService.approveGuarantorship(requestId, false, reason);
            
            return ResponseEntity.ok(ApiResponse.success("Guarantor request rejected successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Check if member is eligible to be a guarantor
     */
    @GetMapping("/guarantor-eligibility/{guarantorId}/{loanAmount}")
    public ResponseEntity<?> checkGuarantorEligibility(@PathVariable Long guarantorId, @PathVariable BigDecimal loanAmount) {
        try {
            Optional<Member> memberOpt = memberRepository.findById(guarantorId);
            if (!memberOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Member guarantor = memberOpt.get();
            
            // Get guarantor's accounts
            Optional<Account> savingsAccount = accountRepository.findByMemberIdAndAccountType(
                    guarantorId, Account.AccountType.SAVINGS);
            Optional<Account> sharesAccount = accountRepository.findByMemberIdAndAccountType(
                    guarantorId, Account.AccountType.SHARES);
            
            BigDecimal savingsBalance = savingsAccount.map(Account::getBalance).orElse(BigDecimal.ZERO);
            BigDecimal sharesBalance = sharesAccount.map(Account::getBalance).orElse(BigDecimal.ZERO);
            
            // Get current pledge commitments
            BigDecimal currentPledges = guarantorRepository.sumActivePledgesByMemberId(guarantorId);
            if (currentPledges == null) currentPledges = BigDecimal.ZERO;
            
            // Count active guarantorships
            long activeGuarantorships = guarantorRepository.countByMemberIdAndStatus(
                    guarantorId, Guarantor.Status.ACCEPTED);
            
            // Calculate available capacity
            BigDecimal availableCapacity = savingsBalance.subtract(currentPledges);
            
            // Validate eligibility for this specific loan
            com.minet.sacco.service.GuarantorValidationService.GuarantorValidationResult result = 
                guarantorValidationService.validateGuarantor(guarantor, loanAmount, null);
            
            // Build response with all details
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("eligible", result.isEligible());
            response.put("errors", result.getErrors());
            response.put("warnings", result.getWarnings());
            response.put("savingsBalance", savingsBalance);
            response.put("sharesBalance", sharesBalance);
            response.put("currentPledges", currentPledges);
            response.put("availableCapacity", availableCapacity);
            response.put("activeGuarantorships", activeGuarantorships);
            response.put("loanAmount", loanAmount);
            response.put("canGuarantee", availableCapacity.compareTo(loanAmount) >= 0 && result.isEligible());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Member makes a loan repayment
     * Payment method determines if savings is debited:
     * - SAVINGS_DEDUCTION: Debit savings account
     * - M-PESA, BANK_TRANSFER, CASH: No savings debit (external payment)
     */
    @PostMapping("/repay-loan")
    public ResponseEntity<?> repayLoan(@RequestBody LoanRepaymentDTO repaymentDTO) {
        try {
            Member member = getCurrentMember();
            Optional<Loan> loanOpt = loanRepository.findById(repaymentDTO.getLoanId());
            
            if (!loanOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Loan loan = loanOpt.get();
            
            // Verify member owns this loan
            if (!loan.getMember().getId().equals(member.getId())) {
                return ResponseEntity.status(403).body("Unauthorized");
            }
            
            // Validate amount
            if (repaymentDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Amount must be greater than zero");
            }
            
            if (repaymentDTO.getAmount().compareTo(loan.getOutstandingBalance()) > 0) {
                return ResponseEntity.badRequest().body("Amount exceeds outstanding balance");
            }
            
            // Create repayment record
            LoanRepayment repayment = new LoanRepayment();
            repayment.setLoan(loan);
            repayment.setAmount(repaymentDTO.getAmount());
            repayment.setRepaymentDate(java.time.LocalDateTime.now());
            repayment.setPaymentMethod(repaymentDTO.getPaymentMethod());
            repayment.setDescription(repaymentDTO.getDescription());
            loanRepaymentRepository.save(repayment);
            
            // Update loan outstanding balance
            loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(repaymentDTO.getAmount()));
            
            // Check if fully repaid
            boolean isFullyRepaid = loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0;
            if (isFullyRepaid) {
                loan.setStatus(Loan.Status.REPAID);
            }
            
            loanRepository.save(loan);
            
            // Create transaction record for audit trail
            Transaction transaction = new Transaction();
            transaction.setAccount(accountRepository.findByMemberIdAndAccountType(
                member.getId(), Account.AccountType.SAVINGS).orElse(null));
            transaction.setTransactionType(Transaction.TransactionType.LOAN_REPAYMENT);
            transaction.setAmount(repaymentDTO.getAmount());
            transaction.setDescription("Loan repayment - " + repaymentDTO.getDescription());
            transaction.setTransactionDate(java.time.LocalDateTime.now());
            transactionRepository.save(transaction);
            
            // If payment method is SAVINGS_DEDUCTION, debit the savings account
            if ("SAVINGS_DEDUCTION".equals(repaymentDTO.getPaymentMethod())) {
                Optional<Account> savingsAccount = accountRepository.findByMemberIdAndAccountType(
                    member.getId(), Account.AccountType.SAVINGS);
                
                if (savingsAccount.isPresent()) {
                    Account account = savingsAccount.get();
                    account.setBalance(account.getBalance().subtract(repaymentDTO.getAmount()));
                    accountRepository.save(account);
                }
            }
            
            // If fully repaid, release guarantor pledges
            if (isFullyRepaid) {
                List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());
                for (Guarantor guarantor : guarantors) {
                    guarantor.setStatus(Guarantor.Status.RELEASED);
                    guarantorRepository.save(guarantor);
                }
            }
            
            return ResponseEntity.ok("Loan repayment processed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Member submits a bank transfer repayment request with proof document
     */
    @PostMapping(value = "/request-loan-repayment", consumes = "multipart/form-data")
    public ResponseEntity<?> requestLoanRepayment(
            @RequestParam Long loanId,
            @RequestParam BigDecimal amount,
            @RequestParam String paymentMethod,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile proofFile) {
        try {
            Member member = getCurrentMember();
            Optional<Loan> loanOpt = loanRepository.findById(loanId);
            
            if (!loanOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Loan loan = loanOpt.get();
            
            // Verify member owns this loan
            if (!loan.getMember().getId().equals(member.getId())) {
                return ResponseEntity.status(403).body("Unauthorized");
            }
            
            // Validate amount
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Amount must be greater than zero");
            }
            
            if (amount.compareTo(loan.getOutstandingBalance()) > 0) {
                return ResponseEntity.badRequest().body("Amount exceeds outstanding balance");
            }
            
            // Validate payment method
            if (!"BANK_TRANSFER".equals(paymentMethod)) {
                return ResponseEntity.badRequest().body("Invalid payment method for this endpoint");
            }
            
            // Validate proof file
            if (proofFile == null || proofFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Proof of payment file is required");
            }
            
            // Save proof file
            String filePath = null;
            try {
                String fileName = System.currentTimeMillis() + "_" + proofFile.getOriginalFilename();
                // Use project root directory for uploads
                String uploadDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "loan-repayments";
                java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
                
                // Create directory if it doesn't exist
                if (!java.nio.file.Files.exists(uploadPath)) {
                    java.nio.file.Files.createDirectories(uploadPath);
                }
                
                filePath = uploadDir + File.separator + fileName;
                java.io.File destFile = new java.io.File(filePath);
                proofFile.transferTo(destFile);
                System.out.println("File uploaded successfully to: " + filePath);
            } catch (Exception e) {
                System.err.println("File upload error: " + e.getMessage());
                e.printStackTrace();
                return ResponseEntity.badRequest().body(new ApiResponse(false, "Error uploading file: " + e.getMessage(), null));
            }
            
            // Create repayment request
            com.minet.sacco.entity.LoanRepaymentRequest repaymentRequest = new com.minet.sacco.entity.LoanRepaymentRequest();
            repaymentRequest.setLoan(loan);
            repaymentRequest.setMember(member);
            repaymentRequest.setAmount(amount);
            repaymentRequest.setPaymentMethod(paymentMethod);
            repaymentRequest.setDescription(description != null ? description : "Loan repayment");
            repaymentRequest.setProofFilePath(filePath);
            repaymentRequest.setProofFileName(proofFile.getOriginalFilename());
            repaymentRequest.setStatus(com.minet.sacco.entity.LoanRepaymentRequest.Status.PENDING);
            repaymentRequest.setCreatedAt(java.time.LocalDateTime.now());
            
            loanRepaymentRequestRepository.save(repaymentRequest);
            
            // Send notification to tellers
            try {
                List<com.minet.sacco.entity.User> tellers = userRepository.findByRole(com.minet.sacco.entity.User.Role.TELLER);
                for (com.minet.sacco.entity.User teller : tellers) {
                    com.minet.sacco.entity.Notification notification = new com.minet.sacco.entity.Notification();
                    notification.setUser(teller);
                    notification.setType("LOAN_REPAYMENT_REQUEST");
                    notification.setMessage("Member " + member.getFirstName() + " " + member.getLastName() + 
                        " submitted a bank transfer repayment request for " + formatCurrency(amount));
                    notification.setCategory("LOAN_REPAYMENT");
                    notification.setLoanId(loan.getId());
                    notification.setMemberId(member.getId());
                    notification.setRead(false);
                    notification.setCreatedAt(java.time.LocalDateTime.now());
                    notificationRepository.save(notification);
                }
            } catch (Exception e) {
                System.err.println("Error sending notification: " + e.getMessage());
            }
            
            // Log audit trail
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userRepository.findByUsername(authentication.getName()).orElse(null);
            auditService.logAction(
                currentUser,
                "LOAN_REPAYMENT_REQUEST_SUBMITTED",
                "LoanRepaymentRequest",
                repaymentRequest.getId(),
                null,
                "Bank transfer repayment request submitted for loan " + loan.getId(),
                "SUCCESS"
            );
            
            return ResponseEntity.ok(new ApiResponse(true, "Repayment request submitted successfully", repaymentRequest.getId()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(new ApiResponse(false, "Error: " + e.getMessage(), null));
        }
    }

    /**
     * Get pending loan repayment requests for member
     */
    @GetMapping("/loan-repayment-requests")
    public ResponseEntity<?> getLoanRepaymentRequests() {
        try {
            Member member = getCurrentMember();
            List<com.minet.sacco.entity.LoanRepaymentRequest> requests = loanRepaymentRequestRepository.findByMemberId(member.getId());
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Download proof file for loan repayment request
     */
    @GetMapping("/loan-repayment-requests/{requestId}/proof/download")
    public ResponseEntity<?> downloadRepaymentProof(@PathVariable Long requestId) {
        try {
            Member member = getCurrentMember();
            Optional<com.minet.sacco.entity.LoanRepaymentRequest> requestOpt = loanRepaymentRequestRepository.findByIdAndMemberId(requestId, member.getId());
            
            if (!requestOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            com.minet.sacco.entity.LoanRepaymentRequest repaymentRequest = requestOpt.get();
            java.io.File file = new java.io.File(repaymentRequest.getProofFilePath());
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + repaymentRequest.getProofFileName() + "\"")
                    .body(new org.springframework.core.io.FileSystemResource(file));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get rejection details for a rejected repayment request
     */
    @GetMapping("/loan-repayment-requests/{requestId}/rejection-details")
    public ResponseEntity<?> getRejectionDetails(@PathVariable Long requestId) {
        try {
            Member member = getCurrentMember();
            Optional<com.minet.sacco.entity.LoanRepaymentRequest> requestOpt = loanRepaymentRequestRepository.findByIdAndMemberId(requestId, member.getId());
            
            if (!requestOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            com.minet.sacco.entity.LoanRepaymentRequest repaymentRequest = requestOpt.get();
            
            // Verify request is rejected
            if (!repaymentRequest.getStatus().equals(com.minet.sacco.entity.LoanRepaymentRequest.Status.REJECTED)) {
                return ResponseEntity.badRequest().body("Request is not rejected");
            }
            
            java.util.Map<String, Object> details = new java.util.HashMap<>();
            details.put("requestId", repaymentRequest.getId());
            details.put("loanId", repaymentRequest.getLoan().getId());
            details.put("loanNumber", repaymentRequest.getLoan().getLoanNumber());
            details.put("requestedAmount", repaymentRequest.getAmount());
            details.put("rejectionReason", repaymentRequest.getRejectionReason());
            details.put("rejectedBy", repaymentRequest.getApprovedBy());
            details.put("rejectedAt", repaymentRequest.getApprovedAt());
            details.put("outstandingBalance", repaymentRequest.getLoan().getOutstandingBalance());
            details.put("canResubmit", true);
            
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Resubmit a rejected repayment request with new proof
     */
    @PostMapping(value = "/loan-repayment-requests/{requestId}/resubmit", consumes = "multipart/form-data")
    public ResponseEntity<?> resubmitRejectedRepayment(
            @PathVariable Long requestId,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) String description,
            @RequestParam MultipartFile proofFile) {
        try {
            Member member = getCurrentMember();
            Optional<com.minet.sacco.entity.LoanRepaymentRequest> requestOpt = loanRepaymentRequestRepository.findByIdAndMemberId(requestId, member.getId());
            
            if (!requestOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            com.minet.sacco.entity.LoanRepaymentRequest originalRequest = requestOpt.get();
            
            // Verify request is rejected
            if (!originalRequest.getStatus().equals(com.minet.sacco.entity.LoanRepaymentRequest.Status.REJECTED)) {
                return ResponseEntity.badRequest().body("Request is not rejected");
            }
            
            Loan loan = originalRequest.getLoan();
            
            // Validate amount
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Amount must be greater than zero");
            }
            
            if (amount.compareTo(loan.getOutstandingBalance()) > 0) {
                return ResponseEntity.badRequest().body("Amount exceeds outstanding balance");
            }
            
            // Validate proof file
            if (proofFile == null || proofFile.isEmpty()) {
                return ResponseEntity.badRequest().body("Proof of payment file is required");
            }
            
            // Save new proof file
            String fileName = System.currentTimeMillis() + "_" + proofFile.getOriginalFilename();
            String filePath = "uploads/loan-repayments/" + fileName;
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get("uploads/loan-repayments"));
            proofFile.transferTo(new java.io.File(filePath));
            
            // Create new repayment request (don't modify original)
            com.minet.sacco.entity.LoanRepaymentRequest newRequest = new com.minet.sacco.entity.LoanRepaymentRequest();
            newRequest.setLoan(loan);
            newRequest.setMember(member);
            newRequest.setAmount(amount);
            newRequest.setPaymentMethod("BANK_TRANSFER");
            newRequest.setDescription(description != null ? description : "Loan repayment (resubmitted)");
            newRequest.setProofFilePath(filePath);
            newRequest.setProofFileName(proofFile.getOriginalFilename());
            newRequest.setStatus(com.minet.sacco.entity.LoanRepaymentRequest.Status.PENDING);
            newRequest.setCreatedAt(java.time.LocalDateTime.now());
            
            loanRepaymentRequestRepository.save(newRequest);
            
            // Send notification to tellers
            try {
                List<com.minet.sacco.entity.User> tellers = userRepository.findByRole(com.minet.sacco.entity.User.Role.TELLER);
                for (com.minet.sacco.entity.User teller : tellers) {
                    com.minet.sacco.entity.Notification notification = new com.minet.sacco.entity.Notification();
                    notification.setUser(teller);
                    notification.setType("LOAN_REPAYMENT_REQUEST");
                    notification.setMessage("Member " + member.getFirstName() + " " + member.getLastName() + 
                        " resubmitted a bank transfer repayment request for KES " + 
                        String.format("%,.2f", amount) + " (previously rejected)");
                    notification.setCategory("LOAN_REPAYMENT");
                    notification.setLoanId(loan.getId());
                    notification.setMemberId(member.getId());
                    notification.setRead(false);
                    notification.setCreatedAt(java.time.LocalDateTime.now());
                    notificationRepository.save(notification);
                }
            } catch (Exception e) {
                System.err.println("Error sending resubmission notification: " + e.getMessage());
            }
            
            // Log audit trail
            Authentication authentication2 = SecurityContextHolder.getContext().getAuthentication();
            User currentUser2 = userRepository.findByUsername(authentication2.getName()).orElse(null);
            auditService.logAction(
                currentUser2,
                "LOAN_REPAYMENT_REQUEST_RESUBMITTED",
                "LoanRepaymentRequest",
                newRequest.getId(),
                null,
                "Bank transfer repayment request resubmitted for loan " + loan.getId() + 
                " (original request " + originalRequest.getId() + " was rejected)",
                "SUCCESS"
            );
            
            return ResponseEntity.ok(new ApiResponse(true, "Repayment request resubmitted successfully", newRequest.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    @PostMapping(value = "/deposit-requests", consumes = "multipart/form-data")
    public ResponseEntity<?> submitDepositRequest(@ModelAttribute com.minet.sacco.dto.DepositRequestFormDTO formData,
                                                   @RequestParam(required = false) MultipartFile receiptFile) {
        try {
            // Validate form data
            if (formData.getAccountId() == null || formData.getAccountId().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Account ID is required"));
            }
            if (formData.getClaimedAmount() == null || formData.getClaimedAmount().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Claimed amount is required"));
            }
            if (formData.getDescription() == null || formData.getDescription().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Description is required"));
            }
            
            Member member = getCurrentMember();
            
            // Parse accountId
            Long accountId;
            try {
                accountId = Long.parseLong(formData.getAccountId());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid account ID format"));
            }
            
            Optional<Account> accountOpt = accountRepository.findById(accountId);
            
            if (!accountOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Account account = accountOpt.get();
            
            // Verify member owns this account
            if (!account.getMember().getId().equals(member.getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Unauthorized"));
            }
            
            // Prevent deposits to SHARES account
            if (account.getAccountType() == Account.AccountType.SHARES) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Deposits to SHARES account are not allowed. This SACCO does not accept share contributions."));
            }
            
            // Parse and validate amount
            BigDecimal amount;
            try {
                amount = new BigDecimal(formData.getClaimedAmount());
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid amount format"));
            }
            
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Amount must be greater than zero"));
            }
            
            String receiptFilePath = null;
            String receiptFileName = null;
            
            // Handle file upload if provided
            if (receiptFile != null && !receiptFile.isEmpty()) {
                // Validate file
                if (receiptFile.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("File size must be less than 5MB"));
                }
                
                String[] allowedTypes = {"image/jpeg", "image/png", "application/pdf", "text/plain", 
                                        "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
                boolean isAllowed = false;
                String contentType = receiptFile.getContentType();
                if (contentType != null) {
                    for (String type : allowedTypes) {
                        if (contentType.equals(type)) {
                            isAllowed = true;
                            break;
                        }
                    }
                }
                if (!isAllowed) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("Invalid file type"));
                }
                
                // Save file
                try {
                    receiptFileName = receiptFile.getOriginalFilename();
                    // Use absolute path - resolve relative to user home or temp directory
                    String baseDir = depositUploadDirectory;
                    if (!java.nio.file.Paths.get(baseDir).isAbsolute()) {
                        baseDir = System.getProperty("user.home") + java.io.File.separator + baseDir;
                    }
                    String uploadDir = baseDir + java.io.File.separator + "member_" + member.getId();
                    java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
                    java.nio.file.Files.createDirectories(uploadPath);
                    
                    String filename = System.currentTimeMillis() + "_" + receiptFileName;
                    java.nio.file.Path filePath = uploadPath.resolve(filename);
                    receiptFile.transferTo(filePath.toFile());
                    
                    receiptFilePath = filePath.toString();
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("Failed to upload file: " + e.getMessage()));
                }
            }
            
            // Submit deposit request
            com.minet.sacco.entity.DepositRequest request = depositRequestService.submitDepositRequest(
                member, account, amount, formData.getDescription(), receiptFilePath, receiptFileName
            );
            
            return ResponseEntity.ok(ApiResponse.success("Deposit request submitted successfully", request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Member views their deposit requests
     */
    @GetMapping("/deposit-requests")
    public ResponseEntity<?> getDepositRequests() {
        try {
            Member member = getCurrentMember();
            List<com.minet.sacco.entity.DepositRequest> requests = depositRequestService.getMemberDepositRequests(member.getId());
            
            List<DepositRequestDTO> dtos = requests.stream()
                .map(r -> new DepositRequestDTO(
                    r.getId(),
                    r.getMember().getId(),
                    r.getMember().getMemberNumber(),
                    r.getMember().getFirstName() + " " + r.getMember().getLastName(),
                    r.getAccount().getId(),
                    r.getAccount().getAccountType().toString(),
                    r.getClaimedAmount(),
                    r.getConfirmedAmount(),
                    r.getDescription(),
                    r.getReceiptFileName(),
                    r.getStatus().toString(),
                    r.getApprovalNotes(),
                    r.getCreatedAt(),
                    r.getApprovedAt()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Member makes a monthly contribution
     */
    @PostMapping("/contribute")
    public ResponseEntity<?> makeContribution(@RequestBody MemberContributionDTO contributionDTO) {
        try {
            Member member = getCurrentMember();
            Optional<Account> accountOpt = accountRepository.findById(contributionDTO.getAccountId());
            
            if (!accountOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Account account = accountOpt.get();
            
            // Verify member owns this account
            if (!account.getMember().getId().equals(member.getId())) {
                return ResponseEntity.status(403).body("Unauthorized");
            }
            
            // Validate amount
            if (contributionDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Amount must be greater than zero");
            }
            
            // Create transaction
            Transaction transaction = new Transaction();
            transaction.setAccount(account);
            transaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
            transaction.setAmount(contributionDTO.getAmount());
            transaction.setTransactionDate(java.time.LocalDateTime.now());
            transaction.setDescription(contributionDTO.getDescription() != null ? 
                contributionDTO.getDescription() : "Monthly contribution");
            
            // Update account balance
            account.setBalance(account.getBalance().add(contributionDTO.getAmount()));
            
            accountRepository.save(account);
            
            return ResponseEntity.ok("Contribution processed successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Member deposits via M-Pesa (Manual - no Daraja)
     */
    @PostMapping("/mpesa-deposit")
    public ResponseEntity<?> mpesaDeposit(@RequestBody java.util.Map<String, Object> request) {
        try {
            Member member = getCurrentMember();
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String phoneNumber = request.get("phoneNumber").toString();
            
            // Validate amount
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Amount must be greater than zero");
            }
            
            // Get or create savings account
            Optional<Account> savingsAccountOpt = accountRepository.findByMemberIdAndAccountType(
                member.getId(), Account.AccountType.SAVINGS
            );
            
            Account savingsAccount;
            if (savingsAccountOpt.isPresent()) {
                savingsAccount = savingsAccountOpt.get();
            } else {
                savingsAccount = new Account();
                savingsAccount.setMember(member);
                savingsAccount.setAccountType(Account.AccountType.SAVINGS);
                savingsAccount.setBalance(BigDecimal.ZERO);
                savingsAccount.setCreatedAt(java.time.LocalDateTime.now());
                savingsAccount = accountRepository.save(savingsAccount);
            }
            
            // Create transaction with PENDING status
            Transaction transaction = new Transaction();
            transaction.setAccount(savingsAccount);
            transaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
            transaction.setAmount(amount);
            transaction.setTransactionDate(java.time.LocalDateTime.now());
            transaction.setDescription("M-Pesa deposit from " + phoneNumber + " - PENDING VERIFICATION");
            transactionRepository.save(transaction);
            
            return ResponseEntity.ok(java.util.Map.of(
                "message", "Deposit request submitted. Please send KES " + amount + " via M-Pesa to our till number.",
                "reference", "MINET" + System.currentTimeMillis(),
                "status", "PENDING"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Member withdraws via M-Pesa (Manual - no Daraja)
     */
    @PostMapping("/mpesa-withdraw")
    public ResponseEntity<?> mpesaWithdraw(@RequestBody java.util.Map<String, Object> request) {
        try {
            Member member = getCurrentMember();
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String phoneNumber = request.get("phoneNumber").toString();
            
            // Validate amount
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Amount must be greater than zero");
            }
            
            // Get savings account
            Optional<Account> savingsAccountOpt = accountRepository.findByMemberIdAndAccountType(
                member.getId(), Account.AccountType.SAVINGS
            );
            
            if (!savingsAccountOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Savings account not found");
            }
            
            Account savingsAccount = savingsAccountOpt.get();
            
            // Check balance
            if (savingsAccount.getBalance().compareTo(amount) < 0) {
                return ResponseEntity.badRequest().body("Insufficient balance");
            }
            
            // Deduct from account immediately
            savingsAccount.setBalance(savingsAccount.getBalance().subtract(amount));
            savingsAccount.setUpdatedAt(java.time.LocalDateTime.now());
            accountRepository.save(savingsAccount);
            
            // Create transaction with PENDING status
            Transaction transaction = new Transaction();
            transaction.setAccount(savingsAccount);
            transaction.setTransactionType(Transaction.TransactionType.WITHDRAWAL);
            transaction.setAmount(amount);
            transaction.setTransactionDate(java.time.LocalDateTime.now());
            transaction.setDescription("M-Pesa withdrawal to " + phoneNumber + " - PENDING PROCESSING");
            transactionRepository.save(transaction);
            
            return ResponseEntity.ok(java.util.Map.of(
                "message", "Withdrawal request submitted. We will send KES " + amount + " to " + phoneNumber + " within 24 hours.",
                "reference", "MINET" + System.currentTimeMillis(),
                "status", "PENDING"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @Autowired
    private NotificationRepository notificationRepository;

    /**
     * Get member notifications
     */
    @GetMapping("/notifications")
    public ResponseEntity<?> getNotifications() {
        try {
            Member member = getCurrentMember();
            Optional<User> userOpt = userRepository.findByMemberId(member.getId());
            if (!userOpt.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", java.util.Collections.emptyList()));
            }
            
            User user = userOpt.get();
            List<Notification> allNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
            
            // Filter notifications to only show those intended for this user's role
            List<Notification> filteredNotifications = allNotifications.stream()
                .filter(n -> n.getTargetRole() == null || n.getTargetRole().equals(user.getRole().toString()))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", filteredNotifications));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get unread notifications
     */
    @GetMapping("/notifications/unread")
    public ResponseEntity<?> getUnreadNotifications() {
        try {
            Member member = getCurrentMember();
            Optional<User> userOpt = userRepository.findByMemberId(member.getId());
            if (!userOpt.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("Unread notifications retrieved", java.util.Collections.emptyList()));
            }
            
            User user = userOpt.get();
            List<Notification> allUnread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId());
            
            // Filter notifications to only show those intended for this user's role
            List<Notification> filteredNotifications = allUnread.stream()
                .filter(n -> n.getTargetRole() == null || n.getTargetRole().equals(user.getRole().toString()))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(ApiResponse.success("Unread notifications retrieved", filteredNotifications));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get unread notification count
     */
    @GetMapping("/notifications/unread-count")
    public ResponseEntity<?> getUnreadCount() {
        try {
            Member member = getCurrentMember();
            Optional<User> userOpt = userRepository.findByMemberId(member.getId());
            if (!userOpt.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", 0L));
            }
            
            User user = userOpt.get();
            List<Notification> allUnread = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(user.getId());
            
            // Filter notifications to only count those intended for this user's role
            long count = allUnread.stream()
                .filter(n -> n.getTargetRole() == null || n.getTargetRole().equals(user.getRole().toString()))
                .count();
            
            return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", count));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Mark notification as read
     */
    @PostMapping("/notifications/{id}/read")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long id) {
        try {
            Member member = getCurrentMember();
            Optional<User> userOpt = userRepository.findByMemberId(member.getId());
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
            }
            
            Optional<Notification> notificationOpt = notificationRepository.findById(id);
            if (!notificationOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Notification notification = notificationOpt.get();
            
            // Verify user owns this notification
            if (!notification.getUser().getId().equals(userOpt.get().getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Unauthorized"));
            }
            
            notification.setRead(true);
            notificationRepository.save(notification);
            
            return ResponseEntity.ok(ApiResponse.success("Notification marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Mark all notifications as read
     */
    @PostMapping("/notifications/read-all")
    public ResponseEntity<?> markAllAsRead() {
        try {
            Member member = getCurrentMember();
            Optional<User> userOpt = userRepository.findByMemberId(member.getId());
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
            }
            
            List<Notification> unreadNotifications = notificationRepository.findByUserIdAndReadFalse(userOpt.get().getId());
            for (Notification notification : unreadNotifications) {
                notification.setRead(true);
            }
            notificationRepository.saveAll(unreadNotifications);
            
            return ResponseEntity.ok(ApiResponse.success("All notifications marked as read"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete a notification
     */
    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id) {
        try {
            Member member = getCurrentMember();
            Optional<User> userOpt = userRepository.findByMemberId(member.getId());
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("User not found"));
            }
            
            Optional<Notification> notificationOpt = notificationRepository.findById(id);
            if (!notificationOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            Notification notification = notificationOpt.get();
            
            // Verify user owns this notification
            if (!notification.getUser().getId().equals(userOpt.get().getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Unauthorized"));
            }
            
            notificationRepository.deleteById(id);
            
            return ResponseEntity.ok(ApiResponse.success("Notification deleted"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Member downloads their own deposit receipt
     */
    @GetMapping("/deposit-requests/{requestId}/receipt/download")
    public ResponseEntity<?> downloadOwnReceipt(@PathVariable Long requestId) {
        try {
            Member member = getCurrentMember();
            Optional<com.minet.sacco.entity.DepositRequest> requestOpt = depositRequestService.getDepositRequest(requestId);
            
            if (!requestOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            
            com.minet.sacco.entity.DepositRequest request = requestOpt.get();
            
            // Verify member owns this request
            if (!request.getMember().getId().equals(member.getId())) {
                return ResponseEntity.status(403).build();
            }
            
            byte[] fileContent = depositRequestService.downloadReceipt(requestId);
            String filename = request.getReceiptFileName();
            
            // Determine content type based on file extension
            org.springframework.http.MediaType contentType = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
            if (filename != null) {
                String lowerFilename = filename.toLowerCase();
                if (lowerFilename.endsWith(".pdf")) {
                    contentType = org.springframework.http.MediaType.APPLICATION_PDF;
                } else if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
                    contentType = org.springframework.http.MediaType.IMAGE_JPEG;
                } else if (lowerFilename.endsWith(".png")) {
                    contentType = org.springframework.http.MediaType.IMAGE_PNG;
                } else if (lowerFilename.endsWith(".txt")) {
                    contentType = org.springframework.http.MediaType.TEXT_PLAIN;
                } else if (lowerFilename.endsWith(".doc") || lowerFilename.endsWith(".docx")) {
                    contentType = org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
                }
            }
            
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .contentType(contentType)
                    .body(fileContent);
        } catch (java.io.IOException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("success", false, "message", "Failed to download receipt: " + e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(org.springframework.http.HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Autowired
    private com.minet.sacco.service.ReportsService reportsService;

    @Autowired
    private LoanRepaymentRepository loanRepaymentRepository;

    @Autowired
    private LoanRepaymentRequestRepository loanRepaymentRequestRepository;

    @Autowired
    private com.minet.sacco.service.AuditService auditService;
    @GetMapping("/account-statement")
    public ResponseEntity<?> getAccountStatement(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Member member = getCurrentMember();
            
            java.time.LocalDate start = startDate != null ? 
                java.time.LocalDate.parse(startDate) : 
                java.time.LocalDate.now().minusMonths(3);
            
            java.time.LocalDate end = endDate != null ? 
                java.time.LocalDate.parse(endDate) : 
                java.time.LocalDate.now();
            
            // Generate PDF
            byte[] pdfContent = generateAccountStatementPDF(member, start, end);
            
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"account-statement-" + member.getMemberNumber() + ".pdf\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfContent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private byte[] generateAccountStatementPDF(Member member, java.time.LocalDate start, java.time.LocalDate end) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
        com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDoc);
        
        // Title
        document.add(new com.itextpdf.layout.element.Paragraph("ACCOUNT STATEMENT")
            .setFontSize(18)
            .setBold()
            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        
        // Member info
        document.add(new com.itextpdf.layout.element.Paragraph(
            "Member: " + member.getFirstName() + " " + member.getLastName() + " (" + member.getMemberNumber() + ")")
            .setFontSize(11));
        document.add(new com.itextpdf.layout.element.Paragraph(
            "Period: " + start + " to " + end)
            .setFontSize(11));
        document.add(new com.itextpdf.layout.element.Paragraph(
            "Generated: " + java.time.LocalDateTime.now())
            .setFontSize(11));
        document.add(new com.itextpdf.layout.element.Paragraph(""));
        
        // Transactions table
        com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(
            new float[]{2, 2, 2, 2, 2});
        table.addHeaderCell("Date");
        table.addHeaderCell("Type");
        table.addHeaderCell("Account");
        table.addHeaderCell("Amount");
        table.addHeaderCell("Balance");
        
        java.time.LocalDateTime startDateTime = start.atStartOfDay();
        java.time.LocalDateTime endDateTime = end.atTime(23, 59, 59);
        
        List<Transaction> transactions = transactionRepository
            .findByAccountMemberIdOrderByTransactionDateDesc(member.getId())
            .stream()
            .filter(t -> t.getTransactionDate() != null &&
                       !t.getTransactionDate().isBefore(startDateTime) &&
                       !t.getTransactionDate().isAfter(endDateTime))
            .collect(Collectors.toList());
        
        BigDecimal balance = BigDecimal.ZERO;
        for (Transaction txn : transactions) {
            table.addCell(txn.getTransactionDate().toLocalDate().toString());
            table.addCell(txn.getTransactionType().toString());
            table.addCell(txn.getAccount().getAccountType().toString());
            table.addCell("KES " + txn.getAmount());
            balance = balance.add(txn.getAmount());
            table.addCell("KES " + balance);
        }
        
        document.add(table);
        document.close();
        
        return baos.toByteArray();
    }

    /**
     * Member downloads their loan statement
     */
    @GetMapping("/loan-statement")
    public ResponseEntity<?> getLoanStatement(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Member member = getCurrentMember();
            
            java.time.LocalDate start = startDate != null ? 
                java.time.LocalDate.parse(startDate) : 
                java.time.LocalDate.now().minusMonths(12);
            
            java.time.LocalDate end = endDate != null ? 
                java.time.LocalDate.parse(endDate) : 
                java.time.LocalDate.now();
            
            // Generate PDF
            byte[] pdfContent = generateLoanStatementPDF(member, start, end);
            
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"loan-statement-" + member.getMemberNumber() + ".pdf\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfContent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private byte[] generateLoanStatementPDF(Member member, java.time.LocalDate start, java.time.LocalDate end) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
        com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDoc);
        
        // Title
        document.add(new com.itextpdf.layout.element.Paragraph("LOAN STATEMENT")
            .setFontSize(18)
            .setBold()
            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        
        // Member info
        document.add(new com.itextpdf.layout.element.Paragraph(
            "Member: " + member.getFirstName() + " " + member.getLastName() + " (" + member.getMemberNumber() + ")")
            .setFontSize(11));
        document.add(new com.itextpdf.layout.element.Paragraph(
            "Period: " + start + " to " + end)
            .setFontSize(11));
        document.add(new com.itextpdf.layout.element.Paragraph(
            "Generated: " + java.time.LocalDateTime.now())
            .setFontSize(11));
        document.add(new com.itextpdf.layout.element.Paragraph(""));
        
        // Get member's loans
        List<Loan> loans = loanRepository.findByMemberId(member.getId());
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        
        for (Loan loan : loans) {
            document.add(new com.itextpdf.layout.element.Paragraph(
                "Loan: " + loan.getLoanNumber() + " - " + loan.getLoanProduct().getName())
                .setBold()
                .setFontSize(12));
            
            com.itextpdf.layout.element.Table loanTable = new com.itextpdf.layout.element.Table(
                new float[]{3, 2});
            loanTable.addCell("Amount");
            loanTable.addCell("KES " + loan.getAmount());
            loanTable.addCell("Interest Rate");
            loanTable.addCell(loan.getInterestRate() + "%");
            loanTable.addCell("Term (Months)");
            loanTable.addCell(loan.getTermMonths().toString());
            loanTable.addCell("Monthly Repayment");
            loanTable.addCell("KES " + loan.getMonthlyRepayment());
            loanTable.addCell("Status");
            loanTable.addCell(loan.getStatus().toString());
            loanTable.addCell("Outstanding Balance");
            loanTable.addCell("KES " + loan.getOutstandingBalance());
            document.add(loanTable);
            
            // Repayments
            List<LoanRepayment> repayments = loanRepaymentRepository.findByLoanId(loan.getId());
            if (!repayments.isEmpty()) {
                document.add(new com.itextpdf.layout.element.Paragraph("Repayments:").setFontSize(10));
                com.itextpdf.layout.element.Table repaymentTable = new com.itextpdf.layout.element.Table(
                    new float[]{2, 2, 2});
                repaymentTable.addHeaderCell("Date");
                repaymentTable.addHeaderCell("Amount");
                repaymentTable.addHeaderCell("Method");
                
                for (LoanRepayment repayment : repayments) {
                    if (repayment.getRepaymentDate().toLocalDate().isAfter(start.minusDays(1)) && 
                        repayment.getRepaymentDate().toLocalDate().isBefore(end.plusDays(1))) {
                        repaymentTable.addCell(repayment.getRepaymentDate().toLocalDate().toString());
                        repaymentTable.addCell("KES " + repayment.getAmount());
                        repaymentTable.addCell(repayment.getPaymentMethod());
                    }
                }
                document.add(repaymentTable);
            }
            
            totalOutstanding = totalOutstanding.add(loan.getOutstandingBalance());
            document.add(new com.itextpdf.layout.element.Paragraph(""));
        }
        
        document.add(new com.itextpdf.layout.element.Paragraph(
            "Total Outstanding: KES " + totalOutstanding)
            .setBold()
            .setFontSize(12));
        
        document.close();
        return baos.toByteArray();
    }

    /**
     * Member downloads their transaction history
     */
    @GetMapping("/transaction-history")
    public ResponseEntity<?> getTransactionHistory(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Member member = getCurrentMember();
            
            java.time.LocalDate start = startDate != null ? 
                java.time.LocalDate.parse(startDate) : 
                java.time.LocalDate.now().minusMonths(6);
            
            java.time.LocalDate end = endDate != null ? 
                java.time.LocalDate.parse(endDate) : 
                java.time.LocalDate.now();
            
            // Generate PDF
            byte[] pdfContent = generateTransactionHistoryPDF(member, start, end);
            
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, 
                        "attachment; filename=\"transaction-history-" + member.getMemberNumber() + ".pdf\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfContent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    private byte[] generateTransactionHistoryPDF(Member member, java.time.LocalDate start, java.time.LocalDate end) throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(baos);
        com.itextpdf.kernel.pdf.PdfDocument pdfDoc = new com.itextpdf.kernel.pdf.PdfDocument(writer);
        com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdfDoc);
        
        // Title
        document.add(new com.itextpdf.layout.element.Paragraph("TRANSACTION HISTORY")
            .setFontSize(18)
            .setBold()
            .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
        
        // Member info
        document.add(new com.itextpdf.layout.element.Paragraph(
            "Member: " + member.getFirstName() + " " + member.getLastName() + " (" + member.getMemberNumber() + ")")
            .setFontSize(11));
        document.add(new com.itextpdf.layout.element.Paragraph(
            "Period: " + start + " to " + end)
            .setFontSize(11));
        document.add(new com.itextpdf.layout.element.Paragraph(
            "Generated: " + java.time.LocalDateTime.now())
            .setFontSize(11));
        document.add(new com.itextpdf.layout.element.Paragraph(""));
        
        java.time.LocalDateTime startDateTime = start.atStartOfDay();
        java.time.LocalDateTime endDateTime = end.atTime(23, 59, 59);
        
        // Get all transactions for member
        List<Transaction> transactions = transactionRepository
            .findByAccountMemberIdOrderByTransactionDateDesc(member.getId())
            .stream()
            .filter(t -> t.getTransactionDate() != null &&
                       !t.getTransactionDate().isBefore(startDateTime) &&
                       !t.getTransactionDate().isAfter(endDateTime))
            .collect(Collectors.toList());
        
        // Transactions table
        com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(
            new float[]{2, 2, 2, 2, 3});
        table.addHeaderCell("Date");
        table.addHeaderCell("Type");
        table.addHeaderCell("Account");
        table.addHeaderCell("Amount");
        table.addHeaderCell("Description");
        
        BigDecimal totalDeposits = BigDecimal.ZERO;
        BigDecimal totalWithdrawals = BigDecimal.ZERO;
        
        for (Transaction transaction : transactions) {
            table.addCell(transaction.getTransactionDate().toLocalDate().toString());
            table.addCell(transaction.getTransactionType().toString());
            table.addCell(transaction.getAccount().getAccountType().toString());
            table.addCell("KES " + transaction.getAmount());
            table.addCell(transaction.getDescription() != null ? transaction.getDescription() : "");
            
            if (transaction.getTransactionType() == Transaction.TransactionType.DEPOSIT ||
                transaction.getTransactionType() == Transaction.TransactionType.LOAN_DISBURSEMENT) {
                totalDeposits = totalDeposits.add(transaction.getAmount());
            } else if (transaction.getTransactionType() == Transaction.TransactionType.WITHDRAWAL) {
                totalWithdrawals = totalWithdrawals.add(transaction.getAmount());
            }
        }
        
        document.add(table);
        document.add(new com.itextpdf.layout.element.Paragraph(""));
        
        // Summary
        document.add(new com.itextpdf.layout.element.Paragraph("SUMMARY")
            .setBold()
            .setFontSize(12));
        
        com.itextpdf.layout.element.Table summaryTable = new com.itextpdf.layout.element.Table(
            new float[]{3, 2});
        summaryTable.addCell("Total Deposits");
        summaryTable.addCell("KES " + totalDeposits);
        summaryTable.addCell("Total Withdrawals");
        summaryTable.addCell("KES " + totalWithdrawals);
        summaryTable.addCell("Net Cash Flow");
        summaryTable.addCell("KES " + totalDeposits.subtract(totalWithdrawals));
        document.add(summaryTable);
        
        document.close();
        return baos.toByteArray();
    }

    /**
     * Helper method to format currency for notifications
     */
    private String formatCurrency(BigDecimal amount) {
        return "KES " + String.format("%,.2f", amount);
    }
}
