package com.minet.sacco.service;

import com.minet.sacco.dto.LoanApplicationRequest;
import com.minet.sacco.dto.LoanApprovalRequest;
import com.minet.sacco.dto.LoanApprovalValidationDTO;
import com.minet.sacco.dto.LoanRepaymentRequest;
import com.minet.sacco.dto.GuarantorRequest;
import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class LoanService {

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanProductRepository loanProductRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private LoanRepaymentRepository loanRepaymentRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private GuarantorValidationService guarantorValidationService;

    @Autowired
    private LoanNumberGenerationService loanNumberGenerationService;

    @Autowired
    private MemberValidationService memberValidationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private LoanEligibilityValidator loanEligibilityValidator;

    @Autowired
    private UserService userService;

    @Autowired
    private LoanDisbursementService loanDisbursementService;

    @Autowired
    private LoanEligibilityRulesService loanEligibilityRulesService;

    @Autowired
    private GuarantorTrackingService guarantorTrackingService;

    @Autowired
    private EligibilityCalculationService eligibilityCalculationService;

    public Member getMemberById(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
    }

    public LoanEligibilityValidator.EligibilityResult checkMemberEligibility(Member member, BigDecimal amount) {
        return loanEligibilityValidator.validateMemberEligibility(member, amount);
    }

    public List<Loan> getAllLoans() {
        return loanRepository.findAll();
    }

    public Optional<Loan> getLoanById(Long id) {
        return loanRepository.findById(id);
    }

    public List<Loan> getLoansByMemberId(Long memberId) {
        return loanRepository.findByMemberId(memberId);
    }

    public List<Loan> getLoansByStatus(Loan.Status status) {
        return loanRepository.findByStatus(status);
    }

    @Transactional
    public Loan applyForLoan(LoanApplicationRequest request, User createdBy) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new RuntimeException("Member not found"));

        // CHECK FOR PENDING LOANS - Prevent multiple pending applications
        List<Loan> pendingLoans = loanRepository.findByMemberIdAndStatusIn(member.getId(), 
            java.util.Arrays.asList(
                Loan.Status.PENDING,
                Loan.Status.PENDING_GUARANTOR_APPROVAL,
                Loan.Status.PENDING_LOAN_OFFICER_REVIEW,
                Loan.Status.PENDING_CREDIT_COMMITTEE,
                Loan.Status.PENDING_TREASURER,
                Loan.Status.APPROVED
            ));
        
        if (!pendingLoans.isEmpty()) {
            throw new RuntimeException("You already have a pending loan application. Please wait for it to be processed before applying for a new loan.");
        }

        LoanProduct loanProduct = loanProductRepository.findById(request.getLoanProductId())
                .orElseThrow(() -> new RuntimeException("Loan product not found"));

        // Validate loan amount
        if (loanProduct.getMinAmount() != null && request.getAmount().compareTo(loanProduct.getMinAmount()) < 0) {
            throw new RuntimeException("Loan amount below minimum");
        }
        if (loanProduct.getMaxAmount() != null && request.getAmount().compareTo(loanProduct.getMaxAmount()) > 0) {
            throw new RuntimeException("Loan amount exceeds maximum");
        }

        // EMERGENCY LOAN CONSTRAINT: Check cumulative borrowing limit on this product
        if (loanProduct.getMaxTotalBorrowingLimit() != null && 
            loanProduct.getMaxTotalBorrowingLimit().compareTo(BigDecimal.ZERO) > 0) {
            
            BigDecimal currentOutstanding = eligibilityCalculationService
                    .getOutstandingBalanceByProduct(member, loanProduct.getId());
            BigDecimal availableCapacity = loanProduct.getMaxTotalBorrowingLimit()
                    .subtract(currentOutstanding);
            
            if (request.getAmount().compareTo(availableCapacity) > 0) {
                throw new RuntimeException(
                    "Loan amount exceeds available borrowing capacity on this product. " +
                    "Maximum allowed: KES " + loanProduct.getMaxTotalBorrowingLimit() + 
                    ", Already borrowed: KES " + currentOutstanding + 
                    ", Available to borrow: KES " + availableCapacity);
            }
        }

        // Validate term against loan product limits
        if (loanProduct.getMinTermMonths() != null && request.getTermMonths() < loanProduct.getMinTermMonths()) {
            throw new RuntimeException("Loan term below minimum of " + loanProduct.getMinTermMonths() + " months");
        }
        if (loanProduct.getMaxTermMonths() != null && request.getTermMonths() > loanProduct.getMaxTermMonths()) {
            throw new RuntimeException("Loan term exceeds maximum of " + loanProduct.getMaxTermMonths() + " months");
        }

        // Validate term against global SACCO policy (set by Admin)
        LoanEligibilityRules rules = loanEligibilityRulesService.getRules();
        int globalMax = rules.getMaxLoanTermMonths() != null ? rules.getMaxLoanTermMonths() : 72;
        if (request.getTermMonths() > globalMax) {
            throw new RuntimeException("Loan term exceeds the maximum allowed by SACCO policy (" + globalMax + " months / " + (globalMax / 12) + " years)");
        }

        // Validate guarantors exist and are ACTIVE
        BigDecimal totalGuaranteeAmount = BigDecimal.ZERO;
        boolean hasSelfGuarantee = false;
        
        if (request.getGuarantors() != null && !request.getGuarantors().isEmpty()) {
            for (com.minet.sacco.dto.GuarantorRequest gReq : request.getGuarantors()) {
                // Validate guarantor ID is not null
                if (gReq.getGuarantorId() == null) {
                    throw new RuntimeException("Guarantor ID cannot be null. For self-guarantee, please ensure the member ID is properly set.");
                }
                
                Member guarantor = memberRepository.findById(gReq.getGuarantorId())
                        .orElseThrow(() -> new RuntimeException("Guarantor member not found"));
                if (guarantor.getStatus() != Member.Status.ACTIVE) {
                    throw new RuntimeException("Guarantor is not ACTIVE: " + guarantor.getFirstName() + " " + guarantor.getLastName());
                }
                
                // Validate guarantee amount is positive
                if (gReq.getGuaranteeAmount() == null || gReq.getGuaranteeAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new RuntimeException("Guarantee amount must be greater than zero");
                }
                
                totalGuaranteeAmount = totalGuaranteeAmount.add(gReq.getGuaranteeAmount());
                
                // Check if this is self-guarantee
                if (gReq.isSelfGuarantee() && gReq.getGuarantorId().equals(request.getMemberId())) {
                    hasSelfGuarantee = true;
                    
                    // FIX 1: Validate self-guarantor has sufficient AVAILABLE savings (excluding frozen)
                    Account savingsAccount = accountRepository
                            .findByMemberIdAndAccountType(gReq.getGuarantorId(), Account.AccountType.SAVINGS)
                            .orElse(null);
                    
                    // Calculate available savings = balance - frozen_savings
                    BigDecimal totalBalance = savingsAccount != null ? 
                            savingsAccount.getBalance() : BigDecimal.ZERO;
                    BigDecimal frozenSavings = savingsAccount != null ? 
                            (savingsAccount.getFrozenSavings() != null ? savingsAccount.getFrozenSavings() : BigDecimal.ZERO) : BigDecimal.ZERO;
                    BigDecimal availableSavings = totalBalance.subtract(frozenSavings);
                    
                    if (availableSavings.compareTo(gReq.getGuaranteeAmount()) < 0) {
                        throw new RuntimeException(
                            "Self-guarantee validation failed: You must have at least KES " + 
                            gReq.getGuaranteeAmount() + " in savings to self-guarantee this loan. " +
                            "Current available savings: KES " + availableSavings);
                    }
                }
                
                // For external guarantors, validation is done at approval time (not at application)
                // This preserves data privacy - member doesn't see guarantor's financial info
                // Guarantor will see if they're eligible when they try to approve
            }
            
            // Validate total guarantee amount equals loan amount
            if (totalGuaranteeAmount.compareTo(request.getAmount()) != 0) {
                throw new RuntimeException("Total guarantee amount (" + totalGuaranteeAmount + ") must equal loan amount (" + request.getAmount() + ")");
            }
        } else if (!hasSelfGuarantee) {
            // If no guarantors provided and no self-guarantee, loan cannot proceed
            throw new RuntimeException("Loan must have at least one guarantor or member must self-guarantee");
        }

        // RESTRUCTURED: Don't calculate interest at application stage
        // Interest will be set by treasurer at PENDING_TREASURER stage
        // Leave fields null - treasurer will set them before final approval
        BigDecimal principal = request.getAmount();
        BigDecimal annualRate = loanProduct.getInterestRate();
        Integer termMonths = request.getTermMonths();

        // Determine loan status based on guarantor types
        boolean hasNonSelfGuarantors = request.getGuarantors() != null && 
            request.getGuarantors().stream().anyMatch(g -> !g.isSelfGuarantee());

        Loan.Status initialStatus;
        if (hasNonSelfGuarantors) {
            // Has external guarantors - need their approval first
            initialStatus = Loan.Status.PENDING_GUARANTOR_APPROVAL;
        } else if (request.getGuarantors() != null && !request.getGuarantors().isEmpty()) {
            // Only self-guarantors - skip to loan officer review
            initialStatus = Loan.Status.PENDING_LOAN_OFFICER_REVIEW;
        } else {
            // No guarantors at all
            initialStatus = Loan.Status.PENDING;
        }

        // Create loan
        Loan loan = new Loan();
        loan.setMember(member);
        loan.setLoanProduct(loanProduct);
        loan.setAmount(principal);
        loan.setOriginalPrincipal(principal);  // Set original principal to the requested amount
        loan.setInterestRate(annualRate);
        loan.setTermMonths(termMonths);
        // RESTRUCTURED: Don't set interest fields at application - leave them null
        loan.setMonthlyRepayment(null);  // Will be set by treasurer
        loan.setTotalInterest(null);     // Will be set by treasurer
        loan.setTotalRepayable(null);    // Will be set by treasurer
        loan.setOutstandingBalance(null); // Will be set by treasurer
        loan.setPurpose(request.getPurpose());
        loan.setStatus(initialStatus);
        loan.setApplicationDate(LocalDateTime.now());
        loan.setCreatedBy(createdBy);
        loan.setLoanNumber(null); // Will be assigned on disbursement

        // Don't call calculateRepaymentDetails() - interest not calculated at application
        loan = loanRepository.save(loan);

        // Create guarantor records with custom guarantee amounts
        if (request.getGuarantors() != null && !request.getGuarantors().isEmpty()) {
            for (com.minet.sacco.dto.GuarantorRequest gReq : request.getGuarantors()) {
                Member guarantorMember = memberRepository.findById(gReq.getGuarantorId())
                        .orElseThrow(() -> new RuntimeException("Guarantor member not found"));

                Guarantor guarantor = new Guarantor();
                guarantor.setLoan(loan);
                guarantor.setMember(guarantorMember);
                guarantor.setGuaranteeAmount(gReq.getGuaranteeAmount());  // Custom guarantee amount
                guarantor.setSelfGuarantee(gReq.isSelfGuarantee());
                
                // Auto-approve self-guarantors
                if (gReq.isSelfGuarantee()) {
                    guarantor.setStatus(Guarantor.Status.ACCEPTED);
                    guarantor.setApprovedAt(LocalDateTime.now());
                } else {
                    guarantor.setStatus(Guarantor.Status.PENDING);
                }
                
                guarantorRepository.save(guarantor);
                
                // Send notification to guarantor (skip if self-guarantee)
                if (!gReq.isSelfGuarantee()) {
                    Optional<User> guarantorUserOpt = userService.getUserByMemberId(gReq.getGuarantorId());
                    if (guarantorUserOpt.isPresent()) {
                        // Build a more detailed notification message for partial guarantees
                        String guaranteeMessage;
                        if (gReq.getGuaranteeAmount().compareTo(request.getAmount()) < 0) {
                            // Partial guarantee - specify both the total loan and their portion
                            guaranteeMessage = "You have been requested to guarantee KES " + gReq.getGuaranteeAmount() + 
                                " of a KES " + request.getAmount() + " loan application for " +
                                member.getFirstName() + " " + member.getLastName() + " (Member: " + member.getMemberNumber() + "). " +
                                "Product: " + loanProduct.getName() + ", Term: " + request.getTermMonths() + " months. " +
                                "Please review and approve or reject.";
                        } else {
                            // Full guarantee
                            guaranteeMessage = "You have been requested to guarantee a KES " + gReq.getGuaranteeAmount() + 
                                " loan application for " + member.getFirstName() + " " + member.getLastName() + 
                                " (Member: " + member.getMemberNumber() + "). " +
                                "Product: " + loanProduct.getName() + ", Term: " + request.getTermMonths() + " months. " +
                                "Please review and approve or reject.";
                        }
                        
                        notificationService.notifyUser(guarantorUserOpt.get().getId(),
                            guaranteeMessage,
                            "GUARANTOR_REQUEST", loan.getId(), gReq.getGuarantorId(), "GUARANTOR_REQUEST");
                    }
                }
            }
            
            // Check if all guarantors are approved (for mixed scenarios)
            List<Guarantor> allGuarantors = guarantorRepository.findByLoanId(loan.getId());
            boolean allApproved = allGuarantors.stream()
                .allMatch(g -> g.getStatus() == Guarantor.Status.ACCEPTED);
            
            if (allApproved && hasNonSelfGuarantors) {
                // All guarantors approved, transition to loan officer review
                loan.setStatus(Loan.Status.PENDING_LOAN_OFFICER_REVIEW);
                loan = loanRepository.save(loan);
            }
        }

        return loan;
    }

    @Transactional
    public Loan approveLoan(LoanApprovalRequest request, User approvedBy) {
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Check current status and determine next stage based on user role
        Loan.Status currentStatus = loan.getStatus();
        
        // Validate that the user's role matches the current loan status
        validateApprovalAuthorization(currentStatus, approvedBy);
        
        if (request.getApproved()) {
            // Validate member and guarantors
            LoanEligibilityValidator.EligibilityResult memberResult = 
                    loanEligibilityValidator.validateMemberEligibility(loan.getMember(), loan.getAmount());
            
            loan.setMemberEligibilityStatus(memberResult.isEligible() ? "ELIGIBLE" : "INELIGIBLE");
            if (!memberResult.getErrors().isEmpty()) {
                loan.setMemberEligibilityErrors(String.join("; ", memberResult.getErrors()));
            }
            
            if (!memberResult.isEligible()) {
                throw new RuntimeException("Member not eligible: " + memberResult.getErrors());
            }

            // Validate guarantors
            List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());
            if (!guarantors.isEmpty()) {
                List<Long> guarantorIds = guarantors.stream()
                        .map(g -> g.getMember().getId())
                        .toList();
                
                List<GuarantorValidationService.GuarantorValidationResult> validationResults = 
                        guarantorValidationService.validateAllGuarantors(guarantorIds, loan.getAmount());
                
                for (int i = 0; i < validationResults.size() && i < 3; i++) {
                    GuarantorValidationService.GuarantorValidationResult result = validationResults.get(i);
                    
                    if (i < guarantors.size()) {
                        guarantors.get(i).setStatus(result.isEligible() ? Guarantor.Status.ACCEPTED : Guarantor.Status.REJECTED);
                        guarantorRepository.save(guarantors.get(i));
                    }
                }
            }

            // Determine next status based on current status and user role
            Loan.Status nextStatus;
            String notificationMessage;
            String notificationRole;
            
            if (currentStatus == Loan.Status.PENDING_LOAN_OFFICER_REVIEW) {
                nextStatus = Loan.Status.PENDING_CREDIT_COMMITTEE;
                notificationMessage = "Loan application from " + loan.getMember().getFirstName() + " " + 
                    loan.getMember().getLastName() + " (Amount: KES " + loan.getAmount() + ") has been approved by Loan Officer. " +
                    "Please review and approve.";
                notificationRole = "CREDIT_COMMITTEE";
            } else if (currentStatus == Loan.Status.PENDING_CREDIT_COMMITTEE) {
                nextStatus = Loan.Status.PENDING_TREASURER;
                notificationMessage = "Loan application from " + loan.getMember().getFirstName() + " " + 
                    loan.getMember().getLastName() + " (Amount: KES " + loan.getAmount() + ") has been approved by Credit Committee. " +
                    "Please review and approve for disbursement.";
                notificationRole = "TREASURER";
            } else if (currentStatus == Loan.Status.PENDING_TREASURER) {
                nextStatus = Loan.Status.APPROVED;
                
                // REDUCING BALANCE: No interest calculation at approval
                // Treasurer simply approves the loan for disbursement
                // Interest will be recorded during repayments based on reducing balance method
                // Outstanding balance will be set to principal only at disbursement
                
                notificationMessage = "Your loan application for KES " + loan.getAmount() + " has been approved and is ready for disbursement.";
                notificationRole = null; // Notify member
            } else {
                // Default: move to loan officer review
                nextStatus = Loan.Status.PENDING_LOAN_OFFICER_REVIEW;
                notificationMessage = "New loan application from " + loan.getMember().getFirstName() + " " + 
                    loan.getMember().getLastName() + " (Amount: KES " + loan.getAmount() + ") is pending your review.";
                notificationRole = "LOAN_OFFICER";
            }
            
            loan.setStatus(nextStatus);
            loan.setApprovalDate(LocalDateTime.now());
            loan.setApprovedBy(approvedBy);
            
            // Send notification
            if (notificationRole != null) {
                notificationService.notifyUsersByRole(notificationRole, notificationMessage, "LOAN_APPROVAL", loan.getId(), loan.getMember().getId(), "LOAN_APPROVAL");
            } else {
                Optional<User> memberUserOpt = userService.getUserByMemberId(loan.getMember().getId());
                if (memberUserOpt.isPresent()) {
                    notificationService.notifyUser(memberUserOpt.get().getId(), notificationMessage, "LOAN_APPROVED", loan.getId(), loan.getMember().getId(), "LOAN_APPROVED");
                }
            }
        } else {
            // Determine rejection status based on current stage
            // Rejection should revert to previous stage, not go directly to REJECTED
            if (currentStatus == Loan.Status.PENDING_TREASURER) {
                // Treasurer rejects → go back to Credit Committee
                loan.setStatus(Loan.Status.PENDING_CREDIT_COMMITTEE);
                notificationService.notifyUsersByRole("CREDIT_COMMITTEE", 
                    "Loan application from " + loan.getMember().getFirstName() + " " + 
                    loan.getMember().getLastName() + " (Amount: KES " + loan.getAmount() + ") was rejected by Treasurer. " +
                    "Reason: " + request.getComments() + ". Please review and decide.",
                    "LOAN_REJECTION", loan.getId(), loan.getMember().getId(), "LOAN_REJECTION");
            } else if (currentStatus == Loan.Status.PENDING_CREDIT_COMMITTEE) {
                // Credit Committee rejects → go back to Loan Officer
                loan.setStatus(Loan.Status.PENDING_LOAN_OFFICER_REVIEW);
                notificationService.notifyUsersByRole("LOAN_OFFICER", 
                    "Loan application from " + loan.getMember().getFirstName() + " " + 
                    loan.getMember().getLastName() + " (Amount: KES " + loan.getAmount() + ") was rejected by Credit Committee. " +
                    "Reason: " + request.getComments() + ". Please review and decide.",
                    "LOAN_REJECTION", loan.getId(), loan.getMember().getId(), "LOAN_REJECTION");
            } else {
                // Loan Officer rejects → final rejection
                loan.setStatus(Loan.Status.REJECTED);
                Optional<User> memberUserOpt = userService.getUserByMemberId(loan.getMember().getId());
                if (memberUserOpt.isPresent()) {
                    notificationService.notifyUser(memberUserOpt.get().getId(), 
                        "Your loan application for KES " + loan.getAmount() + " has been rejected. Reason: " + request.getComments(),
                        "LOAN_REJECTED", loan.getId(), loan.getMember().getId(), "LOAN_REJECTED");
                }
            }
            
            loan.setRejectionReason(request.getComments());
            loan.setApprovedBy(approvedBy);
        }

        // Log audit event
        String auditAction = request.getApproved() ? "APPROVE" : "REJECT";
        String auditComments = request.getComments();
        String loanDetails = "Loan #" + loan.getLoanNumber() + " - Member: " + loan.getMember().getFirstName() + " " + 
                            loan.getMember().getLastName() + " - Amount: KES " + loan.getAmount();
        auditService.logAction(approvedBy, auditAction, "LOAN", loan.getId(), loanDetails, auditComments, "SUCCESS");

        return loanRepository.save(loan);
    }

    @Transactional
    public Loan disburseLoan(Long loanId, User disbursedBy) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Use consolidated disbursement service
        Loan disbursedLoan = loanDisbursementService.disburseLoan(loan, disbursedBy);
        disbursedLoan.setDisbursedBy(disbursedBy);
        return loanRepository.save(disbursedLoan);
    }

    @Transactional
    public LoanRepayment makeRepayment(LoanRepaymentRequest request, User createdBy) {
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        if (loan.getStatus() != Loan.Status.DISBURSED) {
            throw new RuntimeException("Loan is not in disbursed status");
        }

        BigDecimal requestAmount = request.getAmount();
        BigDecimal principalAmount = request.getPrincipalAmount() != null ? request.getPrincipalAmount().setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal interestAmount = request.getInterestAmount() != null ? request.getInterestAmount().setScale(2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

        if ((requestAmount == null || requestAmount.compareTo(BigDecimal.ZERO) == 0)
                && (principalAmount.compareTo(BigDecimal.ZERO) > 0 || interestAmount.compareTo(BigDecimal.ZERO) > 0)) {
            requestAmount = principalAmount.add(interestAmount);
        }

        if (requestAmount == null || requestAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Repayment amount must be greater than zero");
        }

        if (principalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Principal amount cannot be negative");
        }

        if (interestAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Interest amount cannot be negative");
        }

        // Auto-calculate missing split — only total is mandatory
        // Scenario 1: Only total provided → all goes to principal
        // Scenario 2: Total + interest provided → calculate principal
        // Scenario 3: Total + principal provided → calculate interest
        // Scenario 4: Principal + interest provided → total auto-summed (lines 507-510), then validate
        // Scenario 5: All three provided → validate they add up correctly
        if (principalAmount.compareTo(BigDecimal.ZERO) == 0 && interestAmount.compareTo(BigDecimal.ZERO) == 0) {
            // Only total provided → all goes to principal
            principalAmount = requestAmount;
        } else if (principalAmount.compareTo(BigDecimal.ZERO) == 0 && interestAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Total + interest provided → calculate principal
            principalAmount = requestAmount.subtract(interestAmount);
        } else if (interestAmount.compareTo(BigDecimal.ZERO) == 0 && principalAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Total + principal provided → calculate interest
            interestAmount = requestAmount.subtract(principalAmount);
        }
        // Note: Scenarios 4 & 5 (principal + interest provided) skip auto-calc here
        // because requestAmount was already auto-calculated from principal + interest (lines 507-510)
        // The final validation below ensures all amounts are consistent

        // Validate no negatives after calculation
        if (principalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Principal amount cannot be negative — interest exceeds total repayment");
        }

        if (interestAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Interest amount cannot be negative — principal exceeds total repayment");
        }

        // Final check: principal + interest must equal total
        if (principalAmount.add(interestAmount).compareTo(requestAmount) != 0) {
            throw new RuntimeException("Repayment total must equal principal plus interest");
        }

        // Validate repayment amount
        // Round both to 2 decimal places to avoid floating point precision issues
        BigDecimal outstandingBefore = getOutstandingBalance(loan.getId())
                .setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal requestAmountRounded = requestAmount.setScale(2, java.math.RoundingMode.HALF_UP);
        if (requestAmountRounded.compareTo(outstandingBefore) > 0) {
            throw new RuntimeException("Repayment amount cannot exceed outstanding balance of KES " + outstandingBefore);
        }

        // Create repayment record
        LoanRepayment repayment = new LoanRepayment();
        repayment.setLoan(loan);
        repayment.setAmount(requestAmount);
        repayment.setPrincipalAmount(principalAmount);
        repayment.setInterestAmount(interestAmount);
        repayment.setRecordedBy(createdBy);
        repayment.setPaymentDate(LocalDateTime.now());
        
        // Set payment method from request if provided
        if (request.getPaymentMethod() != null && !request.getPaymentMethod().trim().isEmpty()) {
            String normalizedMethod = request.getPaymentMethod().trim().toUpperCase().replace(" ", "_");
            // Common aliases that should map to the canonical enum value
            if (normalizedMethod.equals("SALARY")) {
                normalizedMethod = "SALARY_DEDUCTION";
            }
            try {
                repayment.setPaymentMethod(LoanRepayment.PaymentMethod.valueOf(normalizedMethod));
                System.out.println("[MAKE_REPAYMENT] Payment method set to: " + repayment.getPaymentMethod());
            } catch (IllegalArgumentException e) {
                System.out.println("[MAKE_REPAYMENT] WARNING: Invalid payment method '" + request.getPaymentMethod() + "', defaulting to CASH. Valid values: SALARY_DEDUCTION, CASH, MPESA, BANK_TRANSFER, CHEQUE, OTHER");
                repayment.setPaymentMethod(LoanRepayment.PaymentMethod.CASH);
            }
        } else {
            System.out.println("[MAKE_REPAYMENT] No payment method provided, using default: CASH");
        }
        
        // Set transaction reference from request if provided
        if (request.getTransactionReference() != null && !request.getTransactionReference().trim().isEmpty()) {
            repayment.setReferenceNumber(request.getTransactionReference());
            System.out.println("[MAKE_REPAYMENT] Reference number set to: " + repayment.getReferenceNumber());
        } else {
            System.out.println("[MAKE_REPAYMENT] No reference number provided");
        }
        
        repayment = loanRepaymentRepository.save(repayment);

        // Create transaction records for GL accounting
        // Create final copies for use in lambda (lambda expressions require effectively final variables)
        final BigDecimal finalRequestAmount = requestAmount;
        final BigDecimal finalInterestAmount = interestAmount;
        final LoanRepayment finalRepayment = repayment;
        
        accountRepository.findByMemberIdAndAccountType(loan.getMember().getId(), Account.AccountType.SAVINGS)
                .ifPresent(account -> {
                    // Create LOAN_REPAYMENT transaction for the full amount
                    Transaction transaction = new Transaction();
                    transaction.setAccount(account);
                    transaction.setTransactionType(Transaction.TransactionType.LOAN_REPAYMENT);
                    transaction.setAmount(finalRequestAmount);
                    transaction.setDescription("Loan repayment - Loan #" + loan.getLoanNumber() +
                            " - Method: " + finalRepayment.getPaymentMethod());
                    transaction.setTransactionDate(LocalDateTime.now());
                    transaction.setCreatedBy(createdBy);
                    transactionRepository.save(transaction);

                    // Create separate INTEREST transaction if interest was paid
                    if (finalInterestAmount.compareTo(BigDecimal.ZERO) > 0) {
                        Transaction interestTransaction = new Transaction();
                        interestTransaction.setAccount(account);
                        interestTransaction.setTransactionType(Transaction.TransactionType.INTEREST);
                        interestTransaction.setAmount(finalInterestAmount);
                        interestTransaction.setDescription("Interest income - Loan #" + loan.getLoanNumber());
                        interestTransaction.setTransactionDate(LocalDateTime.now());
                        interestTransaction.setCreatedBy(createdBy);
                        transactionRepository.save(interestTransaction);
                    }
                });

        // Calculate new outstanding balance using reducing-balance method
        // Subtract current repayment's principal from existing balance
        BigDecimal newOutstandingBalance = loan.getOutstandingBalance().subtract(principalAmount);
        
        // Cap at zero (no negative balances)
        if (newOutstandingBalance.compareTo(BigDecimal.ZERO) < 0) {
            newOutstandingBalance = BigDecimal.ZERO;
        }
        
        // Update loan's outstanding balance
        loan.setOutstandingBalance(newOutstandingBalance);

        // Track pledge reduction for guarantors (proportional to repayment)
        // This also unfreezes proportional savings for self-guarantors
        guarantorTrackingService.trackPledgeReduction(loan, request.getAmount());

        // Check if loan is fully repaid
        if (newOutstandingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            loan.setStatus(Loan.Status.REPAID);
            loanRepository.save(loan);
            // Release all guarantor pledges — their savings are no longer frozen
            guarantorTrackingService.releaseAllPledges(loan);
        } else {
            loanRepository.save(loan);
        }

        return repayment;
    }

    public BigDecimal calculateTotalDue(Loan loan) {
        // Simple interest calculation: Principal + (Principal * Rate * Time)
        BigDecimal principal = loan.getAmount();
        BigDecimal rate = loan.getInterestRate().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal time = BigDecimal.valueOf(loan.getTermMonths()).divide(BigDecimal.valueOf(12), 4, RoundingMode.HALF_UP);
        BigDecimal interest = principal.multiply(rate).multiply(time).setScale(2, RoundingMode.HALF_UP);
        return principal.add(interest);
    }

    public BigDecimal getOutstandingBalance(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Return the current outstanding balance directly from the loan record
        // Do NOT recalculate from scratch using original amount - this throws away
        // pre-migration repayment history that's already reflected in the current balance
        return loan.getOutstandingBalance() != null ? loan.getOutstandingBalance() : BigDecimal.ZERO;
    }

    public List<Guarantor> getGuarantorsForLoan(Long loanId) {
        return guarantorRepository.findByLoanId(loanId);
    }

    /**
     * Validate loan approval eligibility for Credit Committee review
     * Returns comprehensive eligibility information for member and guarantors
     */
    public LoanApprovalValidationDTO validateLoanApproval(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        LoanApprovalValidationDTO dto = new LoanApprovalValidationDTO();
        dto.setLoanId(loan.getId());
        dto.setMemberNumber(loan.getMember().getMemberNumber());
        dto.setLoanProductName(loan.getLoanProduct().getName());
        dto.setLoanAmount(String.format("KES %,d", loan.getAmount().longValue()));
        dto.setPurpose(loan.getPurpose());

        // Check if product is enabled
        if (!loan.getLoanProduct().getIsActive()) {
            dto.setProductEnabled(false);
            dto.setProductError("Loan product is not enabled");
        } else {
            dto.setProductEnabled(true);
        }

        // Validate member eligibility
        LoanEligibilityValidator.EligibilityResult memberResult = 
                loanEligibilityValidator.validateMemberEligibility(loan.getMember(), loan.getAmount());
        
        LoanApprovalValidationDTO.MemberEligibilityInfo memberInfo = new LoanApprovalValidationDTO.MemberEligibilityInfo();
        memberInfo.setMemberName(loan.getMember().getFirstName() + " " + loan.getMember().getLastName());
        memberInfo.setStatus(loan.getMember().getStatus().toString());
        
        // Get member account balances
        Account savingsAccount = accountRepository.findByMemberIdAndAccountType(
                loan.getMember().getId(), Account.AccountType.SAVINGS).orElse(null);
        Account sharesAccount = accountRepository.findByMemberIdAndAccountType(
                loan.getMember().getId(), Account.AccountType.SHARES).orElse(null);
        
        BigDecimal savingsBalance = savingsAccount != null ? savingsAccount.getBalance() : BigDecimal.ZERO;
        BigDecimal sharesBalance = sharesAccount != null ? sharesAccount.getBalance() : BigDecimal.ZERO;
        // For member eligibility, only SAVINGS count (not shares)
        // Shares are capital contributions and don't count toward loan capacity
        BigDecimal totalBalance = savingsBalance;
        
        memberInfo.setSavingsBalance(String.format("KES %,d", savingsBalance.longValue()));
        memberInfo.setSharesBalance(String.format("KES %,d", sharesBalance.longValue()));
        memberInfo.setTotalBalance(String.format("KES %,d", totalBalance.longValue()));
        
        // Get outstanding loans
        List<Loan> memberLoans = loanRepository.findByMemberId(loan.getMember().getId());
        BigDecimal totalOutstanding = BigDecimal.ZERO;
        int activeLoans = 0;
        int defaultedLoans = 0;
        
        for (Loan memberLoan : memberLoans) {
            if (memberLoan.getId().equals(loan.getId())) continue; // Skip current loan
            
            if (memberLoan.getStatus() == Loan.Status.DISBURSED) {
                activeLoans++;
                totalOutstanding = totalOutstanding.add(memberLoan.getOutstandingBalance());
            } else if (memberLoan.getStatus() == Loan.Status.DEFAULTED) {
                defaultedLoans++;
            }
        }
        
        memberInfo.setTotalOutstandingBalance(String.format("KES %,d", totalOutstanding.longValue()));
        memberInfo.setActiveLoans(activeLoans);
        memberInfo.setDefaultedLoans(defaultedLoans);
        memberInfo.setIsEligible(memberResult.isEligible());
        memberInfo.setErrors(memberResult.getErrors());
        memberInfo.setWarnings(memberResult.getWarnings());
        
        dto.setMemberInfo(memberInfo);

        // Validate guarantors
        List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());
        List<LoanApprovalValidationDTO.GuarantorEligibilityInfo> guarantorInfos = new java.util.ArrayList<>();
        
        if (!guarantors.isEmpty()) {
            List<Long> guarantorIds = guarantors.stream()
                    .map(g -> g.getMember().getId())
                    .toList();
            
            List<GuarantorValidationService.GuarantorValidationResult> validationResults = 
                    guarantorValidationService.validateAllGuarantors(guarantorIds, loan.getAmount());
            
            for (GuarantorValidationService.GuarantorValidationResult result : validationResults) {
                LoanApprovalValidationDTO.GuarantorEligibilityInfo gInfo = new LoanApprovalValidationDTO.GuarantorEligibilityInfo();
                gInfo.setGuarantorId(result.getGuarantorId());
                gInfo.setGuarantorName(result.getGuarantorName());
                gInfo.setSavingsBalance(String.format("KES %,d", (result.getSavingsBalance() != null ? result.getSavingsBalance() : BigDecimal.ZERO).longValue()));
                gInfo.setSharesBalance(String.format("KES %,d", (result.getSharesBalance() != null ? result.getSharesBalance() : BigDecimal.ZERO).longValue()));
                gInfo.setTotalBalance(String.format("KES %,d", (result.getTotalBalance() != null ? result.getTotalBalance() : BigDecimal.ZERO).longValue()));
                gInfo.setOutstandingBalance(String.format("KES %,d", (result.getOutstandingBalance() != null ? result.getOutstandingBalance() : BigDecimal.ZERO).longValue()));
                gInfo.setAvailableGuaranteeCapacity(String.format("KES %,d", (result.getAvailableGuaranteeCapacity() != null ? result.getAvailableGuaranteeCapacity() : BigDecimal.ZERO).longValue()));
                gInfo.setIsEligible(result.isEligible());
                gInfo.setErrors(result.getErrors());
                gInfo.setWarnings(result.getWarnings());
                guarantorInfos.add(gInfo);
            }
        }
        
        dto.setValidationResults(guarantorInfos);
        dto.setGuarantorCount(guarantors.size());

        // Determine if can approve
        boolean memberEligible = memberResult.isEligible();
        boolean allGuarantorsEligible = guarantorInfos.stream().allMatch(LoanApprovalValidationDTO.GuarantorEligibilityInfo::getIsEligible);
        boolean productEnabled = dto.getProductEnabled() != false;
        boolean canApprove = memberEligible && allGuarantorsEligible && productEnabled;
        
        dto.setCanApprove(canApprove);
        
        if (!productEnabled) {
            dto.setDecisionReason("Loan product is not enabled");
        } else if (!memberEligible) {
            dto.setDecisionReason("Member does not meet eligibility criteria");
        } else if (!allGuarantorsEligible) {
            List<String> ineligibleGuarantors = guarantorInfos.stream()
                    .filter(g -> !g.getIsEligible())
                    .map(LoanApprovalValidationDTO.GuarantorEligibilityInfo::getGuarantorName)
                    .toList();
            dto.setDecisionReason("Guarantor(s) not eligible: " + String.join(", ", ineligibleGuarantors));
        } else {
            dto.setDecisionReason("Member and all guarantors meet eligibility criteria");
        }

        return dto;
    }

    /**
     * Guarantor approves or rejects a loan guarantee
     */
    @Transactional
    public Guarantor approveGuarantorship(Long guarantorId, boolean approved, String comments) {
        Guarantor guarantor = guarantorRepository.findById(guarantorId)
                .orElseThrow(() -> new RuntimeException("Guarantor record not found"));

        if (guarantor.getStatus() != Guarantor.Status.PENDING) {
            throw new RuntimeException("Guarantor is not in pending status");
        }

        Loan loan = guarantor.getLoan();
        Member guarantorMember = guarantor.getMember();
        Member borrower = loan.getMember();

        if (approved) {
            // Validate guarantor is still eligible using their specific guarantee amount
            BigDecimal guaranteeAmount = guarantor.getGuaranteeAmount() != null ? 
                    guarantor.getGuaranteeAmount() : loan.getAmount();
            
            GuarantorValidationService.GuarantorValidationResult validation = 
                    guarantorValidationService.validateGuarantorWithGuaranteeAmount(
                            guarantorMember, loan.getAmount(), guaranteeAmount, loan.getId());

            if (!validation.isEligible()) {
                throw new RuntimeException("Guarantor is no longer eligible: " + validation.getErrors());
            }

            guarantor.setStatus(Guarantor.Status.ACCEPTED);
            guarantor.setApprovedAt(LocalDateTime.now());
            
            // Check if all non-self guarantors have approved
            List<Guarantor> allGuarantors = guarantorRepository.findByLoanId(loan.getId());
            List<Guarantor> nonSelfGuarantors = allGuarantors.stream()
                    .filter(g -> !g.isSelfGuarantee())
                    .toList();
            
            long acceptedCount = nonSelfGuarantors.stream()
                    .filter(g -> g.getStatus() == Guarantor.Status.ACCEPTED)
                    .count();
            
            // If all non-self guarantors approved, move loan to next stage
            if (acceptedCount == nonSelfGuarantors.size() && !nonSelfGuarantors.isEmpty()) {
                loan.setStatus(Loan.Status.PENDING_LOAN_OFFICER_REVIEW);
                loanRepository.save(loan);
                
                // Notify Loan Officers that all guarantors approved
                notificationService.notifyUsersByRole("LOAN_OFFICER",
                    "All guarantors have approved the loan application from " + borrower.getFirstName() + " " + 
                    borrower.getLastName() + " (Amount: KES " + loan.getAmount() + "). Ready for review.",
                    "ALL_GUARANTORS_APPROVED", loan.getId(), borrower.getId(), "GUARANTOR_APPROVED");
            } else {
                // Notify Loan Officer that one guarantor approved (partial)
                notificationService.notifyUsersByRole("LOAN_OFFICER",
                    "Guarantor " + guarantorMember.getFirstName() + " " + guarantorMember.getLastName() + 
                    " has approved the guarantee for loan from " + borrower.getFirstName() + " " + 
                    borrower.getLastName() + " (" + acceptedCount + "/" + nonSelfGuarantors.size() + " approved)",
                    "GUARANTOR_APPROVED", loan.getId(), borrower.getId(), "GUARANTOR_APPROVED");
            }
        } else {
            guarantor.setStatus(Guarantor.Status.REJECTED);
            guarantor.setRejectionReason(comments);
            
            // Update loan status to PENDING_GUARANTOR_REPLACEMENT
            loan.setStatus(Loan.Status.PENDING_GUARANTOR_REPLACEMENT);
            loan.setRejectionReason("Guarantor " + guarantorMember.getFirstName() + " " + 
                guarantorMember.getLastName() + " rejected: " + (comments != null ? comments : "Not specified"));
            loanRepository.save(loan);
            
            // Notify borrower that guarantor rejected with action options
            Optional<User> borrowerUserOpt = userService.getUserByMemberId(borrower.getId());
            if (borrowerUserOpt.isPresent()) {
                notificationService.notifyUser(borrowerUserOpt.get().getId(),
                    "Guarantor " + guarantorMember.getFirstName() + " " + guarantorMember.getLastName() + 
                    " has rejected your loan application. Reason: " + (comments != null ? comments : "Not specified") +
                    ". You have 3 options: Replace Guarantor, Reduce Loan Amount, or Withdraw Application.",
                    "GUARANTOR_REJECTED", loan.getId(), borrower.getId(), "GUARANTOR_REJECTED");
            }
            
            // Notify Loan Officer that guarantor rejected
            notificationService.notifyUsersByRole("LOAN_OFFICER",
                "Guarantor " + guarantorMember.getFirstName() + " " + guarantorMember.getLastName() + 
                " has rejected the guarantee for loan application from " + borrower.getFirstName() + " " + 
                borrower.getLastName() + ". Reason: " + (comments != null ? comments : "Not specified") +
                ". Loan status: PENDING_GUARANTOR_REPLACEMENT",
                "GUARANTOR_REJECTED", loan.getId(), borrower.getId(), "GUARANTOR_REJECTED");
        }

        return guarantorRepository.save(guarantor);
    }

    /**
     * Validate that the user's role matches the current loan status
     * Only the appropriate staff member can approve at each stage
     */
    private void validateApprovalAuthorization(Loan.Status currentStatus, User approvedBy) {
        // Get user's role
        User.Role userRole = approvedBy.getRole();

        String errorMessage = null;

        switch (currentStatus) {
            case PENDING_LOAN_OFFICER_REVIEW:
                if (userRole != User.Role.LOAN_OFFICER) {
                    errorMessage = "Only Loan Officers can approve loans in PENDING_LOAN_OFFICER_REVIEW status";
                }
                break;

            case PENDING_CREDIT_COMMITTEE:
                if (userRole != User.Role.CREDIT_COMMITTEE) {
                    errorMessage = "Only Credit Committee members can approve loans in PENDING_CREDIT_COMMITTEE status";
                }
                break;

            case PENDING_TREASURER:
                if (userRole != User.Role.TREASURER) {
                    errorMessage = "Only Treasurers can approve loans in PENDING_TREASURER status";
                }
                break;

            case PENDING_GUARANTOR_APPROVAL:
                errorMessage = "Loans in PENDING_GUARANTOR_APPROVAL status must be approved by guarantors, not staff";
                break;

            default:
                errorMessage = "Loan cannot be approved in " + currentStatus + " status";
        }

        if (errorMessage != null) {
            throw new RuntimeException(errorMessage);
        }
    }

    /**
     * Apply for loan on behalf of a member (Loan Officer only)
     * Same logic as applyForLoan but with loan officer context
     */
    @Transactional
    public Loan applyForLoanOnBehalf(LoanApplicationRequest request, User loanOfficer) {
        // Delegate to the existing applyForLoan method
        // The loan officer is recorded as the creator
        return applyForLoan(request, loanOfficer);
    }

    /**
     * RESTRUCTURED: Treasurer sets interest and approves/rejects loan
     * Called when loan is in PENDING_TREASURER status
     * Treasurer can override product interest rate before final approval
     */
    @Transactional
    public Loan treasurerSetInterestAndApprove(com.minet.sacco.dto.TreasurerLoanApprovalRequest request, User treasurer) {
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        
        // Validate loan is in PENDING_TREASURER status
        if (loan.getStatus() != Loan.Status.PENDING_TREASURER) {
            throw new RuntimeException("Loan must be in PENDING_TREASURER status to set interest. Current status: " + loan.getStatus());
        }
        
        if (request.getApproved()) {
            // Treasurer approves - calculate interest from provided rate
            BigDecimal principal = loan.getAmount();
            BigDecimal annualRate = request.getInterestRate();
            Integer termMonths = loan.getTermMonths();
            
            // Simple interest calculation: Interest = Principal * Rate * Time
            BigDecimal rate = annualRate.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            BigDecimal timeInYears = new BigDecimal(termMonths).divide(new BigDecimal("12"), 4, RoundingMode.HALF_UP);
            BigDecimal totalInterest = principal.multiply(rate).multiply(timeInYears).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalRepayable = principal.add(totalInterest);
            BigDecimal monthlyRepayment = totalRepayable.divide(new BigDecimal(termMonths), 2, RoundingMode.HALF_UP);
            
            // Set calculated interest fields
            loan.setTotalInterest(totalInterest);
            loan.setTotalRepayable(totalRepayable);
            loan.setMonthlyRepayment(monthlyRepayment);
            loan.setOutstandingBalance(totalRepayable);
            loan.setOriginalPrincipal(principal);  // Set for future reference
            
            // Move to APPROVED status
            loan.setStatus(Loan.Status.APPROVED);
            loan.setApprovedBy(treasurer);
            loan.setApprovalDate(LocalDateTime.now());
            
            loan = loanRepository.save(loan);
            
            // Notify member that loan approved with interest details
            Optional<User> memberUserOpt = memberRepository.findById(loan.getMember().getId())
                    .flatMap(m -> {
                        // Find user by member ID through User table
                        try {
                            return userRepository.findByMemberId(m.getId());
                        } catch (Exception e) {
                            return Optional.empty();
                        }
                    });
            
            if (memberUserOpt.isPresent()) {
                String message = "Your loan application has been approved! Loan Amount: KES " + principal.toPlainString() + 
                        ", Interest Rate: " + annualRate + "%, Total Repayable: KES " + totalRepayable.toPlainString() + 
                        ", Monthly Payment: KES " + monthlyRepayment.toPlainString() + ". Your loan is ready for disbursement.";
                notificationService.notifyUser(memberUserOpt.get().getId(), message, "LOAN_APPROVED", loan.getId(), loan.getMember().getId(), "LOAN_APPROVED");
            }
        } else {
            // Treasurer rejects - revert to PENDING_CREDIT_COMMITTEE for reconsideration
            loan.setStatus(Loan.Status.PENDING_CREDIT_COMMITTEE);
            if (request.getNotes() != null) {
                loan.setRejectionReason(request.getNotes());
            }
            loan = loanRepository.save(loan);
            
            // Notify member about rejection
            Optional<User> rejectionMemberUserOpt = memberRepository.findById(loan.getMember().getId())
                    .flatMap(m -> {
                        try {
                            return userRepository.findByMemberId(m.getId());
                        } catch (Exception e) {
                            return Optional.empty();
                        }
                    });
            
            if (rejectionMemberUserOpt.isPresent()) {
                String message = "Your loan application was not approved at the final review stage. " +
                        (request.getNotes() != null ? "Reason: " + request.getNotes() : "Please contact the SACCO for details.");
                notificationService.notifyUser(rejectionMemberUserOpt.get().getId(), message, "LOAN_REJECTED", loan.getId(), loan.getMember().getId(), "LOAN_REJECTED");
            }
        }
        
        return loan;
    }

    /**
     * Validate member eligibility for loan officer
     * Returns detailed eligibility information for UI display
     */
    public java.util.Map<String, Object> validateMemberEligibilityForLoanOfficer(
            Long memberId, BigDecimal loanAmount, BigDecimal selfGuaranteeAmount) {
        
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found"));
        
        if (member.getStatus() != Member.Status.ACTIVE) {
            throw new RuntimeException("Member is not ACTIVE");
        }

        // Check for pending loans
        List<Loan> pendingLoans = loanRepository.findByMemberIdAndStatusIn(member.getId(), 
            java.util.Arrays.asList(
                Loan.Status.PENDING,
                Loan.Status.PENDING_GUARANTOR_APPROVAL,
                Loan.Status.PENDING_LOAN_OFFICER_REVIEW,
                Loan.Status.PENDING_CREDIT_COMMITTEE,
                Loan.Status.PENDING_TREASURER,
                Loan.Status.APPROVED
            ));
        
        if (!pendingLoans.isEmpty()) {
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("eligible", false);
            result.put("reason", "Member already has a pending loan application");
            return result;
        }

        // Calculate eligibility
        LoanEligibilityValidator.EligibilityResult eligibility = checkMemberEligibility(member, loanAmount);
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("eligible", eligibility.isEligible());
        result.put("netEligibleAmount", eligibility.getNetEligibleAmount());
        result.put("grossEligibility", eligibility.getGrossEligibility());
        result.put("totalOutstanding", eligibility.getTotalOutstanding());
        result.put("baseSavings", eligibility.getBaseSavings());
        result.put("trueSavings", eligibility.getTrueSavings());
        result.put("errors", eligibility.getErrors());
        result.put("warnings", eligibility.getWarnings());
        
        return result;
    }

    /**
     * Validate guarantor eligibility for loan officer
     * Returns detailed eligibility information for UI display
     */
    public java.util.Map<String, Object> validateGuarantorEligibilityForLoanOfficer(
            Long guarantorMemberId, BigDecimal guaranteeAmount) {
        
        Member guarantor = memberRepository.findById(guarantorMemberId)
                .orElseThrow(() -> new RuntimeException("Guarantor member not found"));
        
        if (guarantor.getStatus() != Member.Status.ACTIVE) {
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("eligible", false);
            result.put("reason", "Guarantor is not ACTIVE");
            return result;
        }

        // Get all accounts for the guarantor and sum up savings
        java.util.List<Account> allAccounts = accountRepository.findByMemberId(guarantorMemberId);
        
        BigDecimal totalBalance = BigDecimal.ZERO;
        BigDecimal frozenSavings = BigDecimal.ZERO;
        
        // Sum all SAVINGS type accounts
        for (Account account : allAccounts) {
            if (account.getAccountType() == Account.AccountType.SAVINGS) {
                totalBalance = totalBalance.add(account.getBalance() != null ? account.getBalance() : BigDecimal.ZERO);
                frozenSavings = frozenSavings.add(account.getFrozenSavings() != null ? account.getFrozenSavings() : BigDecimal.ZERO);
            }
        }
        
        BigDecimal availableSavings = totalBalance.subtract(frozenSavings);
        
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("eligible", availableSavings.compareTo(guaranteeAmount) >= 0);
        result.put("totalBalance", totalBalance);
        result.put("frozenSavings", frozenSavings);
        result.put("availableSavings", availableSavings);
        result.put("requiredAmount", guaranteeAmount);
        
        if (availableSavings.compareTo(guaranteeAmount) < 0) {
            result.put("reason", "Insufficient available savings. Required: KES " + guaranteeAmount + 
                    ", Available: KES " + availableSavings);
            result.put("errors", new String[]{"Insufficient available savings"});
        }
        
        return result;
    }

    /**
     * Replace a rejected guarantor with a new one
     * Only works when loan is in PENDING_GUARANTOR_REPLACEMENT status
     */
    @Transactional
    public void replaceGuarantor(Long loanId, Long oldGuarantorId, Long newGuarantorMemberId, 
                                BigDecimal newGuaranteeAmount, User requestedBy) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found"));
        
        if (loan.getStatus() != Loan.Status.PENDING_GUARANTOR_REPLACEMENT) {
            throw new RuntimeException("Loan is not in PENDING_GUARANTOR_REPLACEMENT status");
        }
        
        // Find old guarantor
        Guarantor oldGuarantor = guarantorRepository.findById(oldGuarantorId)
            .orElseThrow(() -> new RuntimeException("Guarantor not found"));
        
        if (oldGuarantor.getStatus() != Guarantor.Status.REJECTED) {
            throw new RuntimeException("Can only replace rejected guarantors");
        }
        
        // Mark old guarantor as REPLACED
        oldGuarantor.setStatus(Guarantor.Status.REPLACED);
        guarantorRepository.save(oldGuarantor);
        
        // Create new guarantor
        Member newGuarantorMember = memberRepository.findById(newGuarantorMemberId)
            .orElseThrow(() -> new RuntimeException("New guarantor member not found"));
        
        // Validate new guarantor eligibility
        GuarantorValidationService.GuarantorValidationResult eligibility = 
            guarantorValidationService.validateGuarantorWithGuaranteeAmount(
                newGuarantorMember, loan.getAmount(), newGuaranteeAmount, loan.getId());
        
        if (!eligibility.isEligible()) {
            throw new RuntimeException("New guarantor is not eligible: " + 
                String.join(", ", eligibility.getErrors()));
        }
        
        // Create new guarantor record
        Guarantor newGuarantor = new Guarantor();
        newGuarantor.setLoan(loan);
        newGuarantor.setMember(newGuarantorMember);
        newGuarantor.setGuaranteeAmount(newGuaranteeAmount);
        newGuarantor.setStatus(Guarantor.Status.PENDING);
        guarantorRepository.save(newGuarantor);
        
        // Loan stays in PENDING_GUARANTOR_REPLACEMENT until all guarantors approve
        
        // Notify new guarantor
        Optional<User> newGuarantorUser = userService.getUserByMemberId(newGuarantorMemberId);
        if (newGuarantorUser.isPresent()) {
            String message = "You have been added as a guarantor for loan #" + loan.getLoanNumber() + 
                            " for member " + loan.getMember().getFirstName() + " " +
                            loan.getMember().getLastName() + ". Guarantee amount: KES " + 
                            newGuaranteeAmount + ". Please approve or reject.";
            
            notificationService.notifyUser(newGuarantorUser.get().getId(), message,
                "GUARANTOR_REQUEST", loan.getId(), newGuarantorMemberId, "GUARANTOR_ADDED");
        }
        
        // Audit log
        auditService.logAction(requestedBy, "REPLACE", "GUARANTOR", newGuarantor.getId(),
            "Replaced guarantor for loan #" + loan.getLoanNumber(), 
            "Old: " + oldGuarantor.getMember().getFirstName() + ", New: " + 
            newGuarantorMember.getFirstName(), "SUCCESS");
    }

    /**
     * Reduce loan amount when guarantor rejects
     * Loan moves back to PENDING_CREDIT_COMMITTEE for re-approval
     */
    @Transactional
    public void reduceLoanAmount(Long loanId, BigDecimal newAmount, String reason, User requestedBy) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found"));
        
        if (loan.getStatus() != Loan.Status.PENDING_GUARANTOR_REPLACEMENT) {
            throw new RuntimeException("Loan is not in PENDING_GUARANTOR_REPLACEMENT status");
        }
        
        if (newAmount.compareTo(loan.getAmount()) >= 0) {
            throw new RuntimeException("New amount must be less than current amount");
        }
        
        // Store original amount if not already stored
        if (loan.getOriginalAmount() == null) {
            loan.setOriginalAmount(loan.getAmount());
        }
        
        // Clear all guarantor assignments for reassignment
        List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());
        for (Guarantor g : guarantors) {
            g.setPreviousGuaranteeAmount(g.getGuaranteeAmount());
            g.setGuaranteeAmount(BigDecimal.ZERO);
            g.setStatus(Guarantor.Status.PENDING_REASSIGNMENT);
            g.setReassignmentReason("Loan amount reduced from " + loan.getAmount() + " to " + newAmount);
            guarantorRepository.save(g);
        }
        
        // Calculate new repayment details
        loan.setAmount(newAmount);
        loan.calculateRepaymentDetails();
        
        // Move loan to PENDING_GUARANTOR_REASSIGNMENT for member to re-assign guarantors
        loan.setStatus(Loan.Status.PENDING_GUARANTOR_REASSIGNMENT);
        loan.setRejectionReason("Loan amount reduced from " + loan.getOriginalAmount() + 
                               " to " + newAmount + ". Reason: " + reason + ". Guarantors must be re-assigned.");
        loanRepository.save(loan);
        
        // Notify guarantors
        for (Guarantor g : guarantors) {
            Optional<User> guarantorUser = userService.getUserByMemberId(g.getMember().getId());
            if (guarantorUser.isPresent()) {
                notificationService.notifyUser(guarantorUser.get().getId(),
                    "Your guarantee amount needs to be re-assigned. Please wait for the member to assign your new guarantee amount.",
                    "GUARANTOR_REASSIGNMENT", loan.getId(), loan.getMember().getId(), "GUARANTOR_REASSIGNMENT");
            }
        }
        
        // Notify member
        Optional<User> memberUser = userService.getUserByMemberId(loan.getMember().getId());
        if (memberUser.isPresent()) {
            String message = "Your loan amount has been reduced to KES " + newAmount +
                            ". Please re-assign your guarantors with new guarantee amounts that cover the new loan amount.";
            notificationService.notifyUser(memberUser.get().getId(), message,
                "LOAN_REDUCTION", loan.getId(), loan.getMember().getId(), "LOAN_REDUCTION");
        }
        
        // Audit log
        auditService.logAction(requestedBy, "REDUCE", "LOAN", loan.getId(),
            "Loan #" + loan.getLoanNumber(), "Reduced from " + loan.getOriginalAmount() +
            " to " + newAmount + ". Guarantors cleared for reassignment.", "SUCCESS");
    }

    /**
     * Withdraw loan application when guarantor rejects
     * Loan is marked as REJECTED and all guarantors marked as DECLINED
     */
    @Transactional
    public void withdrawLoanApplication(Long loanId, String reason, User requestedBy) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found"));
        
        if (loan.getStatus() != Loan.Status.PENDING_GUARANTOR_REPLACEMENT) {
            throw new RuntimeException("Can only withdraw loans in PENDING_GUARANTOR_REPLACEMENT status");
        }
        
        // Mark loan as REJECTED
        loan.setStatus(Loan.Status.REJECTED);
        loan.setRejectionReason("Withdrawn by member: " + reason);
        loanRepository.save(loan);
        
        // Mark all guarantors as DECLINED
        List<Guarantor> guarantors = guarantorRepository.findByLoanId(loan.getId());
        for (Guarantor g : guarantors) {
            if (g.getStatus() != Guarantor.Status.REJECTED) {
                g.setStatus(Guarantor.Status.DECLINED);
                guarantorRepository.save(g);
            }
        }
        
        // Notify member
        Optional<User> memberUser = userService.getUserByMemberId(loan.getMember().getId());
        if (memberUser.isPresent()) {
            String message = "Your loan application #" + loan.getLoanNumber() + 
                            " has been withdrawn. You can reapply anytime with different guarantors.";
            notificationService.notifyUser(memberUser.get().getId(), message,
                "LOAN_WITHDRAWN", loan.getId(), loan.getMember().getId(), "LOAN_WITHDRAWN");
        }
        
        // Notify guarantors
        for (Guarantor g : guarantors) {
            Optional<User> guarantorUser = userService.getUserByMemberId(g.getMember().getId());
            if (guarantorUser.isPresent()) {
                String message = "Loan #" + loan.getLoanNumber() + " for member " +
                               loan.getMember().getFirstName() + " has been withdrawn.";
                notificationService.notifyUser(guarantorUser.get().getId(), message,
                    "LOAN_WITHDRAWN", loan.getId(), g.getMember().getId(), "LOAN_WITHDRAWN");
            }
        }
        
        // Audit log
        auditService.logAction(requestedBy, "WITHDRAW", "LOAN", loan.getId(),
            "Loan #" + loan.getLoanNumber(), reason, "SUCCESS");
    }

    /**
     * Reassign guarantors after loan amount reduction
     * Member provides new guarantor assignments with new guarantee amounts
     * Validates that total guarantees cover the new loan amount
     * Creates new guarantor approval requests
     * 
     * @param loanId ID of the loan
     * @param guarantorAssignments List of guarantor IDs and their new guarantee amounts
     * @param requestedBy User making the request
     */
    @Transactional
    public void reassignGuarantors(Long loanId, List<Map<String, Object>> guarantorAssignments, User requestedBy) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found"));
        
        if (loan.getStatus() != Loan.Status.PENDING_GUARANTOR_REASSIGNMENT) {
            throw new RuntimeException("Loan is not in PENDING_GUARANTOR_REASSIGNMENT status");
        }
        
        // Validate guarantor assignments
        BigDecimal totalGuaranteeAmount = BigDecimal.ZERO;
        List<Guarantor> guarantorsToUpdate = new ArrayList<>();
        
        for (Map<String, Object> assignment : guarantorAssignments) {
            Long guarantorId = ((Number) assignment.get("guarantorId")).longValue();
            BigDecimal guaranteeAmount = new BigDecimal(assignment.get("guaranteeAmount").toString());
            
            Guarantor guarantor = guarantorRepository.findById(guarantorId)
                .orElseThrow(() -> new RuntimeException("Guarantor not found: " + guarantorId));
            
            if (!guarantor.getLoan().getId().equals(loanId)) {
                throw new RuntimeException("Guarantor does not belong to this loan");
            }
            
            if (guaranteeAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Guarantee amount must be greater than 0");
            }
            
            // Validate individual guarantee amount does not exceed loan amount
            if (guaranteeAmount.compareTo(loan.getAmount()) > 0) {
                throw new RuntimeException("Guarantee amount for " + guarantor.getMember().getFirstName() + 
                    " (" + guaranteeAmount + ") cannot exceed loan amount (" + loan.getAmount() + ")");
            }
            
            guarantor.setGuaranteeAmount(guaranteeAmount);
            guarantor.setStatus(Guarantor.Status.PENDING);  // Reset to PENDING for new approval
            guarantorsToUpdate.add(guarantor);
            totalGuaranteeAmount = totalGuaranteeAmount.add(guaranteeAmount);
        }
        
        // Validate total guarantees cover loan amount
        if (totalGuaranteeAmount.compareTo(loan.getAmount()) < 0) {
            throw new RuntimeException("Total guarantee amount (" + totalGuaranteeAmount + 
                ") must be at least equal to loan amount (" + loan.getAmount() + ")");
        }
        
        // Save updated guarantors
        for (Guarantor g : guarantorsToUpdate) {
            guarantorRepository.save(g);
        }
        
        // Update loan status back to PENDING_GUARANTOR_APPROVAL
        loan.setStatus(Loan.Status.PENDING_GUARANTOR_APPROVAL);
        loanRepository.save(loan);
        
        // Notify guarantors of re-approval requests
        for (Guarantor g : guarantorsToUpdate) {
            Optional<User> guarantorUser = userService.getUserByMemberId(g.getMember().getId());
            if (guarantorUser.isPresent()) {
                String message = "Your guarantee amount has changed from KES " + 
                               g.getPreviousGuaranteeAmount() + " to KES " + g.getGuaranteeAmount() + 
                               ". Please review and approve or reject the new amount.";
                notificationService.notifyUser(guarantorUser.get().getId(), message,
                    "GUARANTOR_APPROVAL_REQUEST", loan.getId(), g.getMember().getId(), "GUARANTOR_APPROVAL_REQUEST");
            }
        }
        
        // Notify member
        Optional<User> memberUser = userService.getUserByMemberId(loan.getMember().getId());
        if (memberUser.isPresent()) {
            String message = "You have successfully re-assigned guarantors for loan #" + loan.getLoanNumber() + 
                           ". Total guarantee amount: KES " + totalGuaranteeAmount + 
                           ". Waiting for guarantor approvals.";
            notificationService.notifyUser(memberUser.get().getId(), message,
                "GUARANTOR_REASSIGNMENT_COMPLETE", loan.getId(), loan.getMember().getId(), "GUARANTOR_REASSIGNMENT_COMPLETE");
        }
        
        // Audit log
        auditService.logAction(requestedBy, "REASSIGN", "GUARANTORS", loan.getId(),
            "Loan #" + loan.getLoanNumber(), "Re-assigned " + guarantorsToUpdate.size() + 
            " guarantors with total guarantee amount: KES " + totalGuaranteeAmount, "SUCCESS");
    }

    /**
     * Update loan fields (UI edit feature)
     * Called from Loans page to update individual loans in DISBURSED, REPAID, or DEFAULTED status
     */
    @Transactional
    public Loan updateLoan(Long loanId, com.minet.sacco.dto.LoanUpdateRequestDTO updateRequest, User updatedBy) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));
        
        // Only allow updating loans in final states
        if (!loan.getStatus().equals(Loan.Status.DISBURSED) && 
            !loan.getStatus().equals(Loan.Status.REPAID) && 
            !loan.getStatus().equals(Loan.Status.DEFAULTED)) {
            throw new RuntimeException("Loan can only be edited in DISBURSED, REPAID, or DEFAULTED status. Current status: " + loan.getStatus());
        }
        
        // Check if at least one field is being updated
        if ((updateRequest.getDisbursementDate() == null || updateRequest.getDisbursementDate().toString().isEmpty()) &&
            updateRequest.getOutstandingBalance() == null &&
            updateRequest.getTermMonths() == null &&
            (updateRequest.getGuarantorshipType() == null || updateRequest.getGuarantors() == null || updateRequest.getGuarantors().isEmpty())) {
            throw new RuntimeException("At least one field must be updated");
        }
        
        // Update disbursement date if provided
        if (updateRequest.getDisbursementDate() != null) {
            if (updateRequest.getDisbursementDate().isAfter(java.time.LocalDate.now())) {
                throw new RuntimeException("Disbursement date cannot be in the future");
            }
            loan.setDisbursementDate(updateRequest.getDisbursementDate().atStartOfDay());
        }
        
        // Update outstanding balance if provided
        if (updateRequest.getOutstandingBalance() != null) {
            BigDecimal outstanding = updateRequest.getOutstandingBalance();
            if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Outstanding balance must be >= 0");
            }
            if (outstanding.compareTo(loan.getAmount()) > 0) {
                throw new RuntimeException("Outstanding balance cannot exceed principal (" + loan.getAmount() + ")");
            }
            if (loan.getStatus().equals(Loan.Status.REPAID) && outstanding.compareTo(BigDecimal.ZERO) != 0) {
                throw new RuntimeException("Outstanding balance must be 0 for REPAID loans");
            }
            loan.setOutstandingBalance(outstanding);
        }
        
        // Update term months if provided
        if (updateRequest.getTermMonths() != null) {
            if (updateRequest.getTermMonths() <= 0) {
                throw new RuntimeException("Term months must be > 0");
            }
            loan.setTermMonths(updateRequest.getTermMonths());
        }
        
        // Update guarantors if provided (all-or-nothing atomic replacement)
        if (updateRequest.getGuarantorshipType() != null && 
            updateRequest.getGuarantors() != null && 
            !updateRequest.getGuarantors().isEmpty()) {
            
            // DEBUG: Log what we're receiving
            System.out.println("DEBUG: updateLoan() guarantor update received");
            System.out.println("  Loan ID: " + loanId);
            System.out.println("  Guarantors count: " + updateRequest.getGuarantors().size());
            for (int i = 0; i < updateRequest.getGuarantors().size(); i++) {
                System.out.println("  Guarantor " + i + ": " + updateRequest.getGuarantors().get(i).getEmployeeId() + 
                    " -> " + updateRequest.getGuarantors().get(i).getPledgeAmount());
            }
            
            // Validate guarantorship type
            if (!updateRequest.getGuarantorshipType().equals("NORMAL") && 
                !updateRequest.getGuarantorshipType().equals("SELF")) {
                throw new RuntimeException("Invalid guarantorship type: " + updateRequest.getGuarantorshipType());
            }
            
            // Calculate total guarantee amount being offered
            BigDecimal totalNewGuarantee = BigDecimal.ZERO;
            for (com.minet.sacco.dto.LoanUpdateRequestDTO.GuarantorPairDTO g : updateRequest.getGuarantors()) {
                totalNewGuarantee = totalNewGuarantee.add(g.getPledgeAmount());
            }
            
            // Use outstanding balance if available, otherwise use loan amount
            BigDecimal guaranteeRequirement = loan.getOutstandingBalance() != null && 
                loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0 ? 
                loan.getOutstandingBalance() : loan.getAmount();
            
            // Validate total guarantees meet requirement
            if (totalNewGuarantee.compareTo(guaranteeRequirement) < 0) {
                throw new RuntimeException("Total guarantee amount (" + totalNewGuarantee + 
                    ") must be at least equal to outstanding balance (" + guaranteeRequirement + ")");
            }
            
            // Validate all new guarantors exist and are ACTIVE (or any status for migrated loans)
            List<Member> newGuarantorMembers = new ArrayList<>();
            for (com.minet.sacco.dto.LoanUpdateRequestDTO.GuarantorPairDTO guarantorPair : updateRequest.getGuarantors()) {
                Member guarantorMember = memberRepository.findByMemberNumber(guarantorPair.getEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Guarantor not found: " + guarantorPair.getEmployeeId()));
                
                // For migrated loans, allow any valid member status
                // For non-migrated loans, strictly require ACTIVE status
                boolean isMigrated = loan.getMigrationStatus() != null && !loan.getMigrationStatus().isEmpty();
                if (!isMigrated && !guarantorMember.getStatus().equals(Member.Status.ACTIVE)) {
                    throw new RuntimeException("Guarantor " + guarantorPair.getEmployeeId() + " is not ACTIVE");
                }
                
                // Check if guarantor has sufficient savings
                Optional<Account> guarantorAccount = accountRepository.findByMemberIdAndAccountType(
                    guarantorMember.getId(), Account.AccountType.SAVINGS);
                if (guarantorAccount.isEmpty() || guarantorAccount.get().getBalance().compareTo(guarantorPair.getPledgeAmount()) < 0) {
                    throw new RuntimeException("Guarantor " + guarantorPair.getEmployeeId() + 
                        " has insufficient savings for guarantee amount " + guarantorPair.getPledgeAmount());
                }
                
                newGuarantorMembers.add(guarantorMember);
            }
            
            // TARGETED GUARANTOR UPDATE - Only modify guarantors that actually changed
            // Do NOT delete and recreate all guarantors on every save
            List<Guarantor> existingGuarantors = guarantorRepository.findByLoanId(loanId);
            
            // Build a map of existing guarantors by member ID for easy lookup
            Map<Long, Guarantor> existingGuarantorsByMemberId = new HashMap<>();
            for (Guarantor existing : existingGuarantors) {
                existingGuarantorsByMemberId.put(existing.getMember().getId(), existing);
            }
            
            // Build a set of new guarantor member IDs being submitted
            Set<Long> newGuarantorMemberIds = new HashSet<>();
            for (int i = 0; i < updateRequest.getGuarantors().size(); i++) {
                newGuarantorMemberIds.add(newGuarantorMembers.get(i).getId());
            }
            
            // Step 1: Delete only guarantors that are being removed (not in the new list)
            for (Guarantor existingGuarantor : existingGuarantors) {
                if (!newGuarantorMemberIds.contains(existingGuarantor.getMember().getId())) {
                    // This guarantor is being removed - unfreeze their savings
                    Optional<Account> oldGuarantorAccount = accountRepository.findByMemberIdAndAccountType(
                        existingGuarantor.getMember().getId(), Account.AccountType.SAVINGS);
                    if (oldGuarantorAccount.isPresent() && existingGuarantor.getPledgeAmount() != null) {
                        Account account = oldGuarantorAccount.get();
                        account.setFrozenSavings(account.getFrozenSavings().subtract(existingGuarantor.getPledgeAmount()));
                        accountRepository.save(account);
                    }
                    guarantorRepository.delete(existingGuarantor);
                }
            }
            
            // Step 2: Update existing guarantors whose pledge amounts changed, or add new guarantors
            for (int i = 0; i < updateRequest.getGuarantors().size(); i++) {
                com.minet.sacco.dto.LoanUpdateRequestDTO.GuarantorPairDTO guarantorPair = updateRequest.getGuarantors().get(i);
                Member guarantorMember = newGuarantorMembers.get(i);
                BigDecimal newPledgeAmount = guarantorPair.getPledgeAmount();
                
                Guarantor guarantor = existingGuarantorsByMemberId.get(guarantorMember.getId());
                
                if (guarantor != null) {
                    // EXISTING GUARANTOR - Update only if pledge amount changed
                    BigDecimal oldPledgeAmount = guarantor.getPledgeAmount() != null ? 
                        guarantor.getPledgeAmount() : BigDecimal.ZERO;
                    
                    if (!oldPledgeAmount.equals(newPledgeAmount)) {
                        // Pledge amount changed - adjust frozen savings
                        if (loan.getStatus().equals(Loan.Status.DISBURSED)) {
                            Optional<Account> guarantorAccount = accountRepository.findByMemberIdAndAccountType(
                                guarantorMember.getId(), Account.AccountType.SAVINGS);
                            if (guarantorAccount.isPresent()) {
                                Account account = guarantorAccount.get();
                                // Unfreeze old amount
                                account.setFrozenSavings(account.getFrozenSavings().subtract(oldPledgeAmount));
                                // Freeze new amount
                                account.setFrozenSavings(account.getFrozenSavings().add(newPledgeAmount));
                                accountRepository.save(account);
                            }
                        }
                        
                        // Update the guarantor record with new pledge amount
                        // Calculate frozen amount based on outstanding/principal ratio
                        BigDecimal principal = loan.getAmount();
                        BigDecimal outstanding = loan.getOutstandingBalance() != null ? 
                            loan.getOutstandingBalance() : principal;
                        BigDecimal frozenAmount = newPledgeAmount.multiply(outstanding)
                            .divide(principal, 2, java.math.RoundingMode.HALF_UP);
                        
                        guarantor.setPledgeAmount(loan.getStatus().equals(Loan.Status.DISBURSED) ? 
                            frozenAmount : BigDecimal.ZERO);
                        guarantor.setGuaranteeAmount(newPledgeAmount);
                        guarantor.setPledgeFrozenAtFullAmount(false);  // System-calculated, will reduce with repayments
                        guarantorRepository.save(guarantor);
                    }
                    // If pledge amount didn't change, leave this guarantor completely untouched
                    // (its id, created_at, and other fields remain unchanged)
                } else {
                    // NEW GUARANTOR - Create fresh record
                    // Calculate frozen amount based on outstanding/principal ratio
                    BigDecimal principal = loan.getAmount();
                    BigDecimal outstanding = loan.getOutstandingBalance() != null ? 
                        loan.getOutstandingBalance() : principal;
                    BigDecimal frozenAmount = newPledgeAmount.multiply(outstanding)
                        .divide(principal, 2, java.math.RoundingMode.HALF_UP);
                    
                    Guarantor newGuarantor = new Guarantor();
                    newGuarantor.setLoan(loan);
                    newGuarantor.setMember(guarantorMember);
                    newGuarantor.setSelfGuarantee(false);
                    newGuarantor.setGuaranteeAmount(newPledgeAmount);
                    newGuarantor.setPledgeAmount(loan.getStatus().equals(Loan.Status.DISBURSED) ? 
                        frozenAmount : BigDecimal.ZERO);
                    newGuarantor.setPledgeFrozenAtFullAmount(false);  // System-calculated, will reduce with repayments
                    newGuarantor.setStatus(loan.getStatus().equals(Loan.Status.DISBURSED) ? 
                        Guarantor.Status.ACTIVE : Guarantor.Status.RELEASED);
                    newGuarantor.setApprovedAt(LocalDateTime.now());
                    newGuarantor.setMigrationStatus("UI_UPDATED");
                    guarantorRepository.save(newGuarantor);
                    
                    // Freeze savings for new guarantor
                    if (loan.getStatus().equals(Loan.Status.DISBURSED)) {
                        Optional<Account> guarantorAccount = accountRepository.findByMemberIdAndAccountType(
                            guarantorMember.getId(), Account.AccountType.SAVINGS);
                        if (guarantorAccount.isPresent()) {
                            Account account = guarantorAccount.get();
                            account.setFrozenSavings(account.getFrozenSavings().add(newPledgeAmount));
                            accountRepository.save(account);
                        }
                    }
                }
            }
        }
        
        // Save updated loan
        Loan updatedLoan = loanRepository.save(loan);
        
        // Log audit trail for the update
        StringBuilder auditDetails = new StringBuilder();
        auditDetails.append("Loan #").append(updatedLoan.getLoanNumber()).append(" updated: ");
        if (updateRequest.getDisbursementDate() != null) {
            auditDetails.append("Disbursement Date changed to ").append(updateRequest.getDisbursementDate()).append("; ");
        }
        if (updateRequest.getOutstandingBalance() != null) {
            auditDetails.append("Outstanding Balance changed to KES ").append(updateRequest.getOutstandingBalance()).append("; ");
        }
        if (updateRequest.getTermMonths() != null) {
            auditDetails.append("Term Months changed to ").append(updateRequest.getTermMonths()).append("; ");
        }
        if (updateRequest.getGuarantorshipType() != null && updateRequest.getGuarantors() != null && !updateRequest.getGuarantors().isEmpty()) {
            auditDetails.append("Guarantors updated - Total guarantee: KES ").append(
                updateRequest.getGuarantors().stream()
                    .map(com.minet.sacco.dto.LoanUpdateRequestDTO.GuarantorPairDTO::getPledgeAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
            ).append("; ");
        }
        
        auditService.logAction(updatedBy, "UPDATE", "LOAN", updatedLoan.getId(),
            "Loan #" + updatedLoan.getLoanNumber() + " - Member: " + updatedLoan.getMember().getFirstName() + " " + 
            updatedLoan.getMember().getLastName(), auditDetails.toString(), "SUCCESS");
        
        return updatedLoan;
    }

    /**
     * PHASE A: Update loan fields only (no guarantor data)
     * Editable fields: loanStatus, disbursementDate, interestRate, outstandingBalance, purpose
     * This endpoint is kept completely separate from Phase B (guarantor changes)
     */
    @Transactional
    public Loan updateLoanFieldsOnly(Long loanId, com.minet.sacco.dto.LoanFieldUpdateDTO fieldUpdate, User updatedBy) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));
        
        // Validation: at least one field must be provided
        if (!fieldUpdate.hasAtLeastOneField()) {
            throw new RuntimeException("At least one field must be updated");
        }
        
        // Principal lock for migrated loans: prevent editing principal after migration
        // Users should delete and re-migrate if principal is incorrect
        // NOTE: No principal field in fieldUpdate DTO - this prevents accidental editing
        // If user tries to change amount, they must do so through loan application, not migration
        
        // Update loanStatus if provided
        if (fieldUpdate.getLoanStatus() != null && !fieldUpdate.getLoanStatus().trim().isEmpty()) {
            try {
                Loan.Status newStatus = Loan.Status.valueOf(fieldUpdate.getLoanStatus());
                loan.setStatus(newStatus);
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Invalid loan status: " + fieldUpdate.getLoanStatus() + ". Valid values: " + 
                    String.join(", ", java.util.Arrays.stream(Loan.Status.values()).map(Enum::name).toArray(String[]::new)));
            }
        }
        
        // Update disbursementDate if provided
        if (fieldUpdate.getDisbursementDate() != null) {
            if (fieldUpdate.getDisbursementDate().isAfter(java.time.LocalDate.now())) {
                throw new RuntimeException("Disbursement date cannot be in the future");
            }
            loan.setDisbursementDate(fieldUpdate.getDisbursementDate().atStartOfDay());
        }
        
        // Update interestRate if provided
        if (fieldUpdate.getInterestRate() != null) {
            if (fieldUpdate.getInterestRate().compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Interest rate must be >= 0");
            }
            loan.setInterestRate(fieldUpdate.getInterestRate());
        }
        
        // Update outstandingBalance if provided
        if (fieldUpdate.getOutstandingBalance() != null) {
            BigDecimal outstanding = fieldUpdate.getOutstandingBalance();
            if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Outstanding balance must be >= 0");
            }
            if (outstanding.compareTo(loan.getAmount()) > 0) {
                throw new RuntimeException("Outstanding balance cannot exceed principal (" + loan.getAmount() + ")");
            }
            if (loan.getStatus().equals(Loan.Status.REPAID) && outstanding.compareTo(BigDecimal.ZERO) != 0) {
                throw new RuntimeException("Outstanding balance must be 0 for REPAID loans");
            }
            loan.setOutstandingBalance(outstanding);
        }
        
        // Update interestCollected if provided (for migrated loans that collected interest historically)
        if (fieldUpdate.getInterestCollected() != null) {
            BigDecimal interestCollected = fieldUpdate.getInterestCollected();
            if (interestCollected.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Interest collected must be >= 0");
            }
            // For reducing balance method, totalInterest is not pre-calculated
            // interestCollected represents historical interest for migrated loans
            // No validation against totalInterest needed since interest accrues dynamically
            loan.setInterestCollected(interestCollected);
            // interestRemaining is not applicable for reducing balance - calculated per repayment
        }
        
        // Update purpose if provided
        if (fieldUpdate.getPurpose() != null && !fieldUpdate.getPurpose().trim().isEmpty()) {
            loan.setPurpose(fieldUpdate.getPurpose());
        }
        
        // Save updated loan
        Loan updatedLoan = loanRepository.save(loan);
        
        // Log audit trail
        StringBuilder auditDetails = new StringBuilder();
        auditDetails.append("Loan #").append(updatedLoan.getLoanNumber()).append(" - Field Update (Phase A): ");
        if (fieldUpdate.getLoanStatus() != null) {
            auditDetails.append("Status changed to ").append(fieldUpdate.getLoanStatus()).append("; ");
        }
        if (fieldUpdate.getDisbursementDate() != null) {
            auditDetails.append("Disbursement Date changed to ").append(fieldUpdate.getDisbursementDate()).append("; ");
        }
        if (fieldUpdate.getInterestRate() != null) {
            auditDetails.append("Interest Rate changed to ").append(fieldUpdate.getInterestRate()).append("%; ");
        }
        if (fieldUpdate.getOutstandingBalance() != null) {
            auditDetails.append("Outstanding Balance changed to KES ").append(fieldUpdate.getOutstandingBalance()).append("; ");
        }
        if (fieldUpdate.getInterestCollected() != null) {
            auditDetails.append("Interest Collected changed to KES ").append(fieldUpdate.getInterestCollected()).append("; ");
        }
        if (fieldUpdate.getPurpose() != null) {
            auditDetails.append("Purpose changed to ").append(fieldUpdate.getPurpose()).append("; ");
        }
        
        auditService.logAction(updatedBy, "UPDATE", "LOAN_FIELDS", updatedLoan.getId(),
            "Loan #" + updatedLoan.getLoanNumber() + " - Member: " + updatedLoan.getMember().getFirstName() + " " + 
            updatedLoan.getMember().getLastName(), auditDetails.toString(), "SUCCESS");
        
        return updatedLoan;
    }
}

    /**
     * Delete a loan and all related entities (Treasurer only)
     * Handles cleanup of guarantors, transactions, repayments, and releases frozen pledges
     */
    @Transactional
    public void deleteLoan(Long loanId, User deletedBy) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Prevent deletion of disbursed loans with repayments
        if (loan.getStatus() == Loan.Status.DISBURSED) {
            BigDecimal totalRepaid = loanRepaymentRepository.getTotalRepaidAmount(loanId);
            if (totalRepaid != null && totalRepaid.compareTo(BigDecimal.ZERO) > 0) {
                throw new RuntimeException("Cannot delete loan with existing repayments. Total repaid: KES " + totalRepaid);
            }
        }

        String loanDetails = "Loan #" + loan.getLoanNumber() + " - Member: " + 
                            loan.getMember().getFirstName() + " " + loan.getMember().getLastName() + 
                            " - Amount: KES " + loan.getAmount() + " - Status: " + loan.getStatus();

        // 1. Release all guarantor pledges (unfreeze savings)
        List<Guarantor> guarantors = guarantorRepository.findByLoanId(loanId);
        for (Guarantor guarantor : guarantors) {
            if (guarantor.getPledgeAmount() != null && guarantor.getPledgeAmount().compareTo(BigDecimal.ZERO) > 0) {
                Account guarantorAccount = accountRepository
                        .findByMemberIdAndAccountType(guarantor.getMember().getId(), Account.AccountType.SAVINGS)
                        .orElse(null);
                
                if (guarantorAccount != null && guarantorAccount.getFrozenSavings() != null) {
                    BigDecimal currentFrozen = guarantorAccount.getFrozenSavings();
                    BigDecimal pledgeToRelease = guarantor.getPledgeAmount();
                    BigDecimal newFrozen = currentFrozen.subtract(pledgeToRelease);
                    
                    // Ensure frozen savings don't go negative
                    if (newFrozen.compareTo(BigDecimal.ZERO) < 0) {
                        newFrozen = BigDecimal.ZERO;
                    }
                    
                    guarantorAccount.setFrozenSavings(newFrozen);
                    accountRepository.save(guarantorAccount);
                }
            }
            
            // Notify guarantor about loan deletion
            Optional<User> guarantorUserOpt = userService.getUserByMemberId(guarantor.getMember().getId());
            if (guarantorUserOpt.isPresent() && !guarantor.isSelfGuarantee()) {
                notificationService.notifyUser(guarantorUserOpt.get().getId(),
                    "Loan " + (loan.getLoanNumber() != null ? loan.getLoanNumber() : "#" + loan.getId()) + 
                    " for " + loan.getMember().getFirstName() + " " + loan.getMember().getLastName() + 
                    " has been deleted by Treasurer. Your pledged amount has been released.",
                    "LOAN_DELETED", null, guarantor.getMember().getId(), "LOAN_DELETED");
            }
        }

        // 2. Delete guarantors
        guarantorRepository.deleteAll(guarantors);

        // 3. Delete loan repayments
        List<LoanRepayment> repayments = loanRepaymentRepository.findByLoanIdOrderByPaymentDateDesc(loanId);
        loanRepaymentRepository.deleteAll(repayments);

        // 4. Delete related transactions (loan disbursement and repayments)
        if (loan.getStatus() == Loan.Status.DISBURSED) {
            Account memberAccount = accountRepository
                    .findByMemberIdAndAccountType(loan.getMember().getId(), Account.AccountType.SAVINGS)
                    .orElse(null);
            
            if (memberAccount != null) {
                // Find transactions related to this loan by account
                List<Transaction> allTransactions = transactionRepository.findByAccountId(memberAccount.getId());
                List<Transaction> loanTransactions = new java.util.ArrayList<>();
                
                // Filter transactions related to this loan by description
                String loanRef = loan.getLoanNumber() != null ? loan.getLoanNumber() : String.valueOf(loan.getId());
                for (Transaction transaction : allTransactions) {
                    if (transaction.getDescription() != null && 
                        (transaction.getDescription().contains(loanRef) ||
                         (transaction.getTransactionType() == Transaction.TransactionType.LOAN_DISBURSEMENT && 
                          transaction.getDescription().contains("Loan disbursement")))) {
                        loanTransactions.add(transaction);
                    }
                }
                
                // Reverse the disbursement transaction
                for (Transaction transaction : loanTransactions) {
                    if (transaction.getTransactionType() == Transaction.TransactionType.LOAN_DISBURSEMENT) {
                        BigDecimal currentBalance = memberAccount.getBalance();
                        BigDecimal newBalance = currentBalance.subtract(loan.getAmount());
                        
                        // Check if member has sufficient balance for reversal
                        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
                            throw new RuntimeException("Cannot delete loan: Member account has insufficient balance to reverse disbursement. " +
                                    "Current balance: KES " + currentBalance + ", Loan amount: KES " + loan.getAmount());
                        }
                        
                        memberAccount.setBalance(newBalance);
                        accountRepository.save(memberAccount);
                        break; // Only reverse once
                    }
                }
                
                transactionRepository.deleteAll(loanTransactions);
            }
        }

        // 5. Notify member about loan deletion
        Optional<User> memberUserOpt = userService.getUserByMemberId(loan.getMember().getId());
        if (memberUserOpt.isPresent()) {
            notificationService.notifyUser(memberUserOpt.get().getId(),
                "Your loan application " + (loan.getLoanNumber() != null ? loan.getLoanNumber() : "#" + loan.getId()) + 
                " for KES " + loan.getAmount() + " has been deleted by Treasurer.",
                "LOAN_DELETED", null, loan.getMember().getId(), "LOAN_DELETED");
        }

        // 6. Log audit event
        auditService.logAction(deletedBy, "DELETE", "LOAN", loanId, loanDetails, "Loan deleted by Treasurer", "SUCCESS");

        // 7. Delete the loan
        loanRepository.delete(loan);
    }

    /**
     * Update loan principal (amount) and outstanding balance (Treasurer only)
     * Recalculates all loan financials and logs the change
     */
    @Transactional
    public Loan updateLoanFinancials(Long loanId, BigDecimal newPrincipal, BigDecimal newOutstandingBalance, String reason, User updatedBy) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        // Validate inputs
        if (newPrincipal == null || newPrincipal.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Principal amount must be greater than zero");
        }
        if (newOutstandingBalance == null || newOutstandingBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Outstanding balance cannot be negative");
        }

        // Store old values for audit
        BigDecimal oldPrincipal = loan.getAmount();
        BigDecimal oldOutstanding = loan.getOutstandingBalance();

        // Update principal
        loan.setAmount(newPrincipal);

        // Recalculate loan financials based on new principal
        if (loan.getInterestRate() != null && loan.getTermMonths() != null) {
            BigDecimal rate = loan.getInterestRate().divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP);
            BigDecimal timeInYears = new BigDecimal(loan.getTermMonths()).divide(new BigDecimal("12"), 4, java.math.RoundingMode.HALF_UP);
            BigDecimal totalInterest = newPrincipal.multiply(rate).multiply(timeInYears).setScale(2, java.math.RoundingMode.HALF_UP);
            BigDecimal totalRepayable = newPrincipal.add(totalInterest);
            BigDecimal monthlyRepayment = totalRepayable.divide(new BigDecimal(loan.getTermMonths()), 2, java.math.RoundingMode.HALF_UP);

            loan.setTotalInterest(totalInterest);
            loan.setTotalRepayable(totalRepayable);
            loan.setMonthlyRepayment(monthlyRepayment);
        }

        // Update outstanding balance
        loan.setOutstandingBalance(newOutstandingBalance);

        // Save loan
        loan = loanRepository.save(loan);

        // Prepare audit details
        String auditDetails = "Loan #" + loan.getLoanNumber() + " - Member: " + 
                             loan.getMember().getFirstName() + " " + loan.getMember().getLastName() + "\n" +
                             "Principal: KES " + oldPrincipal + " → KES " + newPrincipal + "\n" +
                             "Outstanding: KES " + oldOutstanding + " → KES " + newOutstandingBalance + "\n" +
                             "New Total Repayable: KES " + loan.getTotalRepayable() + "\n" +
                             "New Monthly Repayment: KES " + loan.getMonthlyRepayment() + "\n" +
                             "Reason: " + (reason != null ? reason : "Not specified");

        // Log audit event
        auditService.logAction(updatedBy, "UPDATE_FINANCIALS", "LOAN", loanId, auditDetails, 
                              "Loan financials updated by Treasurer", "SUCCESS");

        // Notify member about the change
        Optional<User> memberUserOpt = userService.getUserByMemberId(loan.getMember().getId());
        if (memberUserOpt.isPresent()) {
            String changeMessage = "Your loan " + loan.getLoanNumber() + " has been updated:\n" +
                                  "Principal: KES " + oldPrincipal + " → KES " + newPrincipal + "\n" +
                                  "Outstanding Balance: KES " + oldOutstanding + " → KES " + newOutstandingBalance;
            if (reason != null && !reason.trim().isEmpty()) {
                changeMessage += "\nReason: " + reason;
            }
            
            notificationService.notifyUser(memberUserOpt.get().getId(),
                changeMessage,
                "LOAN_UPDATED", loan.getId(), loan.getMember().getId(), "LOAN_FINANCIALS_UPDATED");
        }

        // Notify Loan Officer
        String staffMessage = "Loan " + loan.getLoanNumber() + " for " + 
                             loan.getMember().getFirstName() + " " + loan.getMember().getLastName() + 
                             " has been updated by Treasurer. Principal: KES " + oldPrincipal + " → KES " + newPrincipal + 
                             ", Outstanding: KES " + oldOutstanding + " → KES " + newOutstandingBalance;
        notificationService.notifyUsersByRole("LOAN_OFFICER", staffMessage, "LOAN_UPDATED", 
                                             loan.getId(), loan.getMember().getId(), "LOAN_FINANCIALS_UPDATED");

        return loan;
    }
}
