package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.Guarantor;
import com.minet.sacco.entity.Loan;
import com.minet.sacco.entity.LoanRepayment;
import com.minet.sacco.entity.LoanRepaymentRequest;
import com.minet.sacco.entity.Transaction;
import com.minet.sacco.entity.Account;
import com.minet.sacco.entity.User;
import com.minet.sacco.entity.Notification;
import com.minet.sacco.repository.*;
import com.minet.sacco.service.AuditService;
import com.minet.sacco.service.GuarantorTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.io.File;
import java.nio.file.Files;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/teller/loan-repayments")
@CrossOrigin
public class TellerLoanRepaymentController {

    @Autowired
    private LoanRepaymentRequestRepository loanRepaymentRequestRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private LoanRepaymentRepository loanRepaymentRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private GuarantorTrackingService guarantorTrackingService;

    /**
     * Get all pending loan repayment requests
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('TELLER')")
    public ResponseEntity<?> getPendingRepaymentRequests() {
        try {
            List<LoanRepaymentRequest> requests = loanRepaymentRequestRepository.findByStatus(LoanRepaymentRequest.Status.PENDING);
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get all loan repayment requests (with filtering)
     */
    @GetMapping
    @PreAuthorize("hasRole('TELLER')")
    public ResponseEntity<?> getAllRepaymentRequests(
            @RequestParam(required = false) String status) {
        try {
            List<LoanRepaymentRequest> requests;
            if (status != null && !status.isEmpty()) {
                requests = loanRepaymentRequestRepository.findByStatus(LoanRepaymentRequest.Status.valueOf(status));
            } else {
                requests = loanRepaymentRequestRepository.findAll();
            }
            return ResponseEntity.ok(requests);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Get specific repayment request details
     */
    @GetMapping("/{requestId}")
    @PreAuthorize("hasRole('TELLER')")
    public ResponseEntity<?> getRepaymentRequest(@PathVariable Long requestId) {
        try {
            Optional<LoanRepaymentRequest> requestOpt = loanRepaymentRequestRepository.findById(requestId);
            if (!requestOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(requestOpt.get());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Approve loan repayment request
     */
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('TELLER')")
    public ResponseEntity<?> approveRepaymentRequest(
            @PathVariable Long requestId,
            @RequestParam BigDecimal confirmedAmount) {
        try {
            Optional<LoanRepaymentRequest> requestOpt = loanRepaymentRequestRepository.findById(requestId);
            if (!requestOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            LoanRepaymentRequest repaymentRequest = requestOpt.get();

            // Validate status
            if (!repaymentRequest.getStatus().equals(LoanRepaymentRequest.Status.PENDING)) {
                return ResponseEntity.badRequest().body("Request is not pending");
            }

            // Validate confirmed amount
            if (confirmedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body("Confirmed amount must be greater than zero");
            }

            if (confirmedAmount.compareTo(repaymentRequest.getAmount()) > 0) {
                return ResponseEntity.badRequest().body("Confirmed amount cannot exceed requested amount");
            }

            Loan loan = repaymentRequest.getLoan();

            // Validate loan still exists and has outstanding balance
            if (confirmedAmount.compareTo(loan.getOutstandingBalance()) > 0) {
                return ResponseEntity.badRequest().body("Confirmed amount exceeds outstanding loan balance");
            }

            // Get current user (teller)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Create repayment record
            LoanRepayment repayment = new LoanRepayment();
            repayment.setLoan(loan);
            repayment.setAmount(confirmedAmount);
            repayment.setPaymentDate(LocalDateTime.now());
            repayment.setPaymentMethod(LoanRepayment.PaymentMethod.valueOf(repaymentRequest.getPaymentMethod().toUpperCase()));
            repayment.setReferenceNumber(repaymentRequest.getDescription());
            loanRepaymentRepository.save(repayment);

            // Update loan outstanding balance
            loan.setOutstandingBalance(loan.getOutstandingBalance().subtract(confirmedAmount));

            // Check if fully repaid
            boolean isFullyRepaid = loan.getOutstandingBalance().compareTo(BigDecimal.ZERO) <= 0;
            if (isFullyRepaid) {
                loan.setStatus(Loan.Status.REPAID);
            }

            loanRepository.save(loan);

            // Create transaction record for audit trail
            Transaction transaction = new Transaction();
            transaction.setAccount(accountRepository.findByMemberIdAndAccountType(
                    repaymentRequest.getMember().getId(), Account.AccountType.SAVINGS).orElse(null));
            transaction.setTransactionType(Transaction.TransactionType.LOAN_REPAYMENT);
            transaction.setAmount(confirmedAmount);
            transaction.setDescription("Loan repayment (Bank Transfer) - " + repaymentRequest.getDescription());
            transaction.setTransactionDate(LocalDateTime.now());
            transactionRepository.save(transaction);

            // Track pledge reduction for guarantors (proportional to repayment)
            guarantorTrackingService.trackPledgeReduction(loan, confirmedAmount);

            // If fully repaid, release all guarantor pledges
            if (isFullyRepaid) {
                guarantorTrackingService.releaseAllPledges(loan);
            }

            // Update repayment request status
            repaymentRequest.setStatus(LoanRepaymentRequest.Status.APPROVED);
            repaymentRequest.setApprovedAt(LocalDateTime.now());
            repaymentRequest.setApprovedBy(username);
            repaymentRequest.setConfirmedAmount(confirmedAmount);
            loanRepaymentRequestRepository.save(repaymentRequest);

            // Log audit trail
            auditService.logAction(
                    userRepository.findByUsername(username).get(),
                    "LOAN_REPAYMENT_APPROVED",
                    "LoanRepaymentRequest",
                    repaymentRequest.getId(),
                    null,
                    "Bank transfer repayment approved for loan " + loan.getId() + " - Amount: " + confirmedAmount,
                    "SUCCESS"
            );

            return ResponseEntity.ok(new ApiResponse(true, "Repayment request approved successfully", repaymentRequest));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Reject loan repayment request
     */
    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('TELLER')")
    public ResponseEntity<?> rejectRepaymentRequest(
            @PathVariable Long requestId,
            @RequestParam String rejectionReason) {
        try {
            Optional<LoanRepaymentRequest> requestOpt = loanRepaymentRequestRepository.findById(requestId);
            if (!requestOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            LoanRepaymentRequest repaymentRequest = requestOpt.get();

            // Validate status
            if (!repaymentRequest.getStatus().equals(LoanRepaymentRequest.Status.PENDING)) {
                return ResponseEntity.badRequest().body("Request is not pending");
            }

            // Get current user (teller)
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();

            // Update repayment request status
            repaymentRequest.setStatus(LoanRepaymentRequest.Status.REJECTED);
            repaymentRequest.setApprovedAt(LocalDateTime.now());
            repaymentRequest.setApprovedBy(username);
            repaymentRequest.setRejectionReason(rejectionReason);
            loanRepaymentRequestRepository.save(repaymentRequest);

            // Send notification to member
            try {
                Optional<User> memberUserOpt = userRepository.findByMemberId(repaymentRequest.getMember().getId());
                if (memberUserOpt.isPresent()) {
                    User memberUser = memberUserOpt.get();
                    Notification notification = new Notification();
                    notification.setUser(memberUser);
                    notification.setType("LOAN_REPAYMENT_REJECTED");
                    notification.setMessage("Your bank transfer repayment request for KES " + 
                        String.format("%,.2f", repaymentRequest.getAmount()) + 
                        " has been rejected. Reason: " + rejectionReason + 
                        ". Please review and resubmit with corrected information.");
                    notification.setCategory("LOAN_REPAYMENT");
                    notification.setLoanId(repaymentRequest.getLoan().getId());
                    notification.setMemberId(repaymentRequest.getMember().getId());
                    notification.setRead(false);
                    notification.setCreatedAt(LocalDateTime.now());
                    notificationRepository.save(notification);
                }
            } catch (Exception e) {
                System.err.println("Error sending rejection notification: " + e.getMessage());
            }

            // Log audit trail
            auditService.logAction(
                    userRepository.findByUsername(username).get(),
                    "LOAN_REPAYMENT_REJECTED",
                    "LoanRepaymentRequest",
                    repaymentRequest.getId(),
                    null,
                    "Bank transfer repayment rejected for loan " + repaymentRequest.getLoan().getId() + 
                    " - Amount: " + repaymentRequest.getAmount() + " - Reason: " + rejectionReason,
                    "SUCCESS"
            );

            return ResponseEntity.ok(new ApiResponse(true, "Repayment request rejected successfully", repaymentRequest));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Download proof file for a repayment request
     */
    @GetMapping("/{requestId}/proof/download")
    @PreAuthorize("hasRole('TELLER')")
    public ResponseEntity<?> downloadProofFile(@PathVariable Long requestId) {
        try {
            Optional<LoanRepaymentRequest> requestOpt = loanRepaymentRequestRepository.findById(requestId);
            if (!requestOpt.isPresent()) {
                return ResponseEntity.notFound().build();
            }

            LoanRepaymentRequest repaymentRequest = requestOpt.get();
            String filePath = repaymentRequest.getProofFilePath();

            if (filePath == null || filePath.isEmpty()) {
                return ResponseEntity.badRequest().body("No proof file available");
            }

            File file = new File(filePath);
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = Files.readAllBytes(file.toPath());
            String fileName = repaymentRequest.getProofFileName();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                    .body(fileContent);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error downloading file: " + e.getMessage());
        }
    }
}
