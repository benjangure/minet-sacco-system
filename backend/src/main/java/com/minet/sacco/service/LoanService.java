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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
    private AuditService auditService;

    @Autowired
    private NotificationService notificationService;

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

        LoanProduct loanProduct = loanProductRepository.findById(request.getLoanProductId())
                .orElseThrow(() -> new RuntimeException("Loan product not found"));

        // Validate loan amount
        if (loanProduct.getMinAmount() != null && request.getAmount().compareTo(loanProduct.getMinAmount()) < 0) {
            throw new RuntimeException("Loan amount below minimum");
        }
        if (loanProduct.getMaxAmount() != null && request.getAmount().compareTo(loanProduct.getMaxAmount()) > 0) {
            throw new RuntimeException("Loan amount exceeds maximum");
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
                    
                    // FIX 1: Validate self-guarantor has sufficient savings
                    Account savingsAccount = accountRepository
                            .findByMemberIdAndAccountType(gReq.getGuarantorId(), Account.AccountType.SAVINGS)
                            .orElse(null);
                    
                    BigDecimal availableSavings = savingsAccount != null ? 
                            savingsAccount.getBalance() : BigDecimal.ZERO;
                    
                    if (availableSavings.compareTo(gReq.getGuaranteeAmount()) < 0) {
                        throw new RuntimeException(
                            "Self-guarantee validation failed: You must have at least KES " + 
                            gReq.getGuaranteeAmount() + " in savings to self-guarantee this loan. " +
                            "Current available savings: KES " + availableSavings);
                    }
                }
                
                // For external guarantors, validate they have sufficient savings
                if (!gReq.isSelfGuarantee()) {
                    Account guarantorSavings = accountRepository
                            .findByMemberIdAndAccountType(gReq.getGuarantorId(), Account.AccountType.SAVINGS)
                            .orElse(null);
                    
                    BigDecimal guarantorAvailableSavings = guarantorSavings != null ? 
                            guarantorSavings.getBalance() : BigDecimal.ZERO;
                    
                    if (guarantorAvailableSavings.compareTo(gReq.getGuaranteeAmount()) < 0) {
                        throw new RuntimeException(
                            "Guarantor " + guarantor.getFirstName() + " " + guarantor.getLastName() + 
                            " does not have sufficient savings to guarantee KES " + gReq.getGuaranteeAmount() + 
                            ". Available savings: KES " + guarantorAvailableSavings);
                    }
                }
            }
            
            // Validate total guarantee amount equals loan amount
            if (totalGuaranteeAmount.compareTo(request.getAmount()) != 0) {
                throw new RuntimeException("Total guarantee amount (" + totalGuaranteeAmount + ") must equal loan amount (" + request.getAmount() + ")");
            }
        } else if (!hasSelfGuarantee) {
            // If no guarantors provided and no self-guarantee, loan cannot proceed
            throw new RuntimeException("Loan must have at least one guarantor or member must self-guarantee");
        }

        // Calculate loan financials
        BigDecimal principal = request.getAmount();
        BigDecimal annualRate = loanProduct.getInterestRate();
        Integer termMonths = request.getTermMonths();
        
        // Simple interest calculation: Interest = Principal * Rate * Time
        BigDecimal rate = annualRate.divide(BigDecimal.valueOf(100));
        BigDecimal timeInYears = BigDecimal.valueOf(termMonths).divide(BigDecimal.valueOf(12), 4, BigDecimal.ROUND_HALF_UP);
        BigDecimal totalInterest = principal.multiply(rate).multiply(timeInYears).setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal totalRepayable = principal.add(totalInterest);
        BigDecimal monthlyRepayment = totalRepayable.divide(BigDecimal.valueOf(termMonths), 2, BigDecimal.ROUND_HALF_UP);

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
        loan.setInterestRate(annualRate);
        loan.setTermMonths(termMonths);
        loan.setMonthlyRepayment(monthlyRepayment);
        loan.setTotalInterest(totalInterest);
        loan.setTotalRepayable(totalRepayable);
        loan.setOutstandingBalance(totalRepayable);
        loan.setPurpose(request.getPurpose());
        loan.setStatus(initialStatus);
        loan.setApplicationDate(LocalDateTime.now());
        loan.setCreatedBy(createdBy);
        loan.setLoanNumber(null); // NEW: Explicitly set to null - will be assigned on disbursement

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

        // Create repayment record
        LoanRepayment repayment = new LoanRepayment();
        repayment.setLoan(loan);
        repayment.setAmount(request.getAmount());
        repayment.setRecordedBy(createdBy);
        repayment = loanRepaymentRepository.save(repayment);

        // Track pledge reduction for guarantors (proportional to repayment)
        guarantorTrackingService.trackPledgeReduction(loan, request.getAmount());

        // Check if loan is fully repaid
        BigDecimal totalRepaid = loanRepaymentRepository.getTotalRepaidAmount(loan.getId());
        BigDecimal totalDue = calculateTotalDue(loan);

        if (totalRepaid.compareTo(totalDue) >= 0) {
            loan.setStatus(Loan.Status.REPAID);
            loanRepository.save(loan);
            // Release all guarantor pledges — their savings are no longer frozen
            guarantorTrackingService.releaseAllPledges(loan);
        }

        return repayment;
    }

    public BigDecimal calculateTotalDue(Loan loan) {
        // Simple interest calculation: Principal + (Principal * Rate * Time)
        BigDecimal principal = loan.getAmount();
        BigDecimal rate = loan.getInterestRate().divide(BigDecimal.valueOf(100));
        BigDecimal time = BigDecimal.valueOf(loan.getTermMonths()).divide(BigDecimal.valueOf(12));
        BigDecimal interest = principal.multiply(rate).multiply(time);
        return principal.add(interest);
    }

    public BigDecimal getOutstandingBalance(Long loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        BigDecimal totalDue = calculateTotalDue(loan);
        BigDecimal totalRepaid = loanRepaymentRepository.getTotalRepaidAmount(loanId);

        return totalDue.subtract(totalRepaid != null ? totalRepaid : BigDecimal.ZERO);
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
            
            // Notify borrower that guarantor rejected
            Optional<User> borrowerUserOpt = userService.getUserByMemberId(borrower.getId());
            if (borrowerUserOpt.isPresent()) {
                notificationService.notifyUser(borrowerUserOpt.get().getId(),
                    "Guarantor " + guarantorMember.getFirstName() + " " + guarantorMember.getLastName() + 
                    " has rejected your loan application. Reason: " + (comments != null ? comments : "Not specified"),
                    "GUARANTOR_REJECTED", loan.getId(), borrower.getId(), "GUARANTOR_REJECTED");
            }
            
            // Notify Loan Officer that guarantor rejected
            notificationService.notifyUsersByRole("LOAN_OFFICER",
                "Guarantor " + guarantorMember.getFirstName() + " " + guarantorMember.getLastName() + 
                " has rejected the guarantee for loan application from " + borrower.getFirstName() + " " + 
                borrower.getLastName() + ". Reason: " + (comments != null ? comments : "Not specified"),
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
}
