package com.minet.sacco.service;

import com.minet.sacco.entity.DepositRequest;
import com.minet.sacco.entity.Account;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.Transaction;
import com.minet.sacco.entity.User;
import com.minet.sacco.repository.DepositRequestRepository;
import com.minet.sacco.repository.AccountRepository;
import com.minet.sacco.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class DepositRequestService {

    @Autowired
    private DepositRequestRepository depositRequestRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuditService auditService;

    /**
     * Member submits a deposit request with receipt
     */
    @Transactional
    public DepositRequest submitDepositRequest(Member member, Account account, BigDecimal claimedAmount, 
                                               String description, String receiptFilePath, String receiptFileName) {
        DepositRequest request = new DepositRequest();
        request.setMember(member);
        request.setAccount(account);
        request.setClaimedAmount(claimedAmount);
        request.setDescription(description);
        request.setReceiptFilePath(receiptFilePath);
        request.setReceiptFileName(receiptFileName);
        request.setStatus("PENDING");
        request.setCreatedAt(LocalDateTime.now());

        DepositRequest saved = depositRequestRepository.save(request);

        // Notify tellers
        notificationService.notifyUsersByRole("TELLER", 
            "New deposit request from member " + member.getMemberNumber() + " (" + member.getFirstName() + " " + 
            member.getLastName() + ") for KES " + claimedAmount + ". Please review and approve.", 
            "DEPOSIT_REQUEST", null, member.getId(), "DEPOSIT_REQUEST");

        return saved;
    }

    /**
     * Teller approves deposit request with confirmed amount
     */
    @Transactional
    public DepositRequest approveDepositRequest(Long requestId, BigDecimal confirmedAmount, 
                                                String approvalNotes, String tellerMessage, User approvedByUser) {
        Optional<DepositRequest> requestOpt = depositRequestRepository.findById(requestId);
        if (!requestOpt.isPresent()) {
            throw new RuntimeException("Deposit request not found");
        }

        DepositRequest request = requestOpt.get();
        
        if (!request.getStatus().equals("PENDING")) {
            throw new RuntimeException("Deposit request is not pending");
        }

        // Update request
        request.setConfirmedAmount(confirmedAmount);
        request.setApprovalNotes(approvalNotes);
        request.setTellerMessage(tellerMessage);
        request.setApprovedByUser(approvedByUser);
        request.setStatus("APPROVED");
        request.setApprovedAt(LocalDateTime.now());

        DepositRequest saved = depositRequestRepository.save(request);

        // Create transaction with confirmed amount
        Transaction transaction = new Transaction();
        transaction.setAccount(request.getAccount());
        transaction.setTransactionType(Transaction.TransactionType.DEPOSIT);
        transaction.setAmount(confirmedAmount);
        transaction.setTransactionDate(LocalDateTime.now());
        transaction.setDescription(request.getDescription() + " (Approved by " + approvedByUser.getUsername() + ")");
        transactionRepository.save(transaction);

        // Update account balance
        Account account = request.getAccount();
        account.setBalance(account.getBalance().add(confirmedAmount));
        account.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(account);

        return saved;
    }

    /**
     * Post-approval notifications and audit logging (called after transaction commits)
     */
    public void postApprovalNotificationsAndAudit(DepositRequest request, User approvedByUser, 
                                                   String tellerMessage, String approvalNotes, BigDecimal confirmedAmount) {
        try {
            System.out.println("DEBUG: postApprovalNotificationsAndAudit called for request ID=" + request.getId());
            
            // Notify ONLY the member who submitted the request
            String notificationMessage = "Your deposit request of KES " + request.getClaimedAmount() + " has been approved. " +
                "Confirmed amount: KES " + confirmedAmount;
            if (tellerMessage != null && !tellerMessage.trim().isEmpty()) {
                notificationMessage += ". Teller note: " + tellerMessage;
            }
            
            Optional<User> memberUserOpt = userService.getUserByMemberId(request.getMember().getId());
            if (memberUserOpt.isPresent()) {
                notificationService.notifyUser(memberUserOpt.get().getId(), notificationMessage, "DEPOSIT_APPROVED", null, request.getMember().getId(), "DEPOSIT_APPROVED");
            }

            // Log audit event
            String depositDetails = "Deposit Request #" + request.getId() + " - Member: " + request.getMember().getFirstName() + " " + 
                                   request.getMember().getLastName() + " - Amount: KES " + confirmedAmount;
            System.out.println("DEBUG: About to call auditService.logAction for APPROVE");
            auditService.logAction(approvedByUser, "APPROVE", "DEPOSIT_REQUEST", request.getId(), depositDetails, approvalNotes, "SUCCESS");
            System.out.println("DEBUG: auditService.logAction completed");
        } catch (Exception e) {
            // Log failures but don't fail the approval (it already succeeded)
            System.err.println("Failed to send post-approval notifications/audit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Teller rejects deposit request
     */
    @Transactional
    public DepositRequest rejectDepositRequest(Long requestId, String rejectionReason, String tellerMessage, User rejectedByUser) {
        Optional<DepositRequest> requestOpt = depositRequestRepository.findById(requestId);
        if (!requestOpt.isPresent()) {
            throw new RuntimeException("Deposit request not found");
        }

        DepositRequest request = requestOpt.get();
        
        if (!request.getStatus().equals("PENDING")) {
            throw new RuntimeException("Deposit request is not pending");
        }

        request.setStatus("REJECTED");
        request.setApprovalNotes(rejectionReason);
        request.setTellerMessage(tellerMessage);
        request.setApprovedByUser(rejectedByUser);
        request.setRejectedAt(LocalDateTime.now());

        DepositRequest saved = depositRequestRepository.save(request);

        return saved;
    }

    /**
     * Post-rejection notifications and audit logging (called after transaction commits)
     */
    public void postRejectionNotificationsAndAudit(DepositRequest request, User rejectedByUser, 
                                                    String tellerMessage, String rejectionReason) {
        try {
            System.out.println("DEBUG: postRejectionNotificationsAndAudit called for request ID=" + request.getId());
            
            // Notify member with teller message if provided
            String notificationMessage = "Your deposit request of KES " + request.getClaimedAmount() + " has been rejected. " +
                "Reason: " + rejectionReason;
            if (tellerMessage != null && !tellerMessage.trim().isEmpty()) {
                notificationMessage += ". Teller note: " + tellerMessage;
            }
            
            Optional<User> memberUserOpt = userService.getUserByMemberId(request.getMember().getId());
            if (memberUserOpt.isPresent()) {
                notificationService.notifyUser(memberUserOpt.get().getId(), notificationMessage, "DEPOSIT_REJECTED", null, request.getMember().getId(), "DEPOSIT_REJECTED");
            }

            // Log audit event
            String depositDetails = "Deposit Request #" + request.getId() + " - Member: " + request.getMember().getFirstName() + " " + 
                                   request.getMember().getLastName() + " - Amount: KES " + request.getClaimedAmount();
            System.out.println("DEBUG: About to call auditService.logAction for REJECT");
            auditService.logAction(rejectedByUser, "REJECT", "DEPOSIT_REQUEST", request.getId(), depositDetails, rejectionReason, "SUCCESS");
            System.out.println("DEBUG: auditService.logAction completed");
        } catch (Exception e) {
            // Log failures but don't fail the rejection (it already succeeded)
            System.err.println("Failed to send post-rejection notifications/audit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Get pending deposit requests (for tellers)
     */
    public List<DepositRequest> getPendingRequests() {
        return depositRequestRepository.findByStatusOrderByCreatedAtDesc("PENDING");
    }

    /**
     * Get member's deposit requests
     */
    public List<DepositRequest> getMemberDepositRequests(Long memberId) {
        return depositRequestRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    /**
     * Get deposit request by ID
     */
    public Optional<DepositRequest> getDepositRequest(Long requestId) {
        return depositRequestRepository.findById(requestId);
    }

    /**
     * Download deposit receipt
     */
    public byte[] downloadReceipt(Long requestId) throws java.io.IOException {
        DepositRequest request = depositRequestRepository.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("Deposit request not found: " + requestId));

        String filePath = request.getReceiptFilePath();
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("Receipt file not found for this request");
        }

        java.nio.file.Path path = java.nio.file.Paths.get(filePath);
        
        if (!java.nio.file.Files.exists(path)) {
            throw new IllegalArgumentException("Receipt file does not exist at: " + filePath);
        }

        return java.nio.file.Files.readAllBytes(path);
    }
}
