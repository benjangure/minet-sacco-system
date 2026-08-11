package com.minet.sacco.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for broadcasting real-time notifications via WebSocket.
 * This enables instant UI updates without page refresh.
 */
@Service
public class RealtimeNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeNotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Broadcast loan creation to all connected users
     */
    public void notifyLoanCreated(Long loanId, String loanNumber, Long memberId, String memberName) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "LOAN_CREATED");
        payload.put("loanId", loanId);
        payload.put("loanNumber", loanNumber);
        payload.put("memberId", memberId);
        payload.put("memberName", memberName);
        payload.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/loans", payload);
    }

    /**
     * Broadcast loan status change to all users
     */
    public void notifyLoanStatusChanged(Long loanId, String loanNumber, String oldStatus, String newStatus, Long memberId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "LOAN_STATUS_CHANGED");
        payload.put("loanId", loanId);
        payload.put("loanNumber", loanNumber);
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);
        payload.put("memberId", memberId);
        payload.put("timestamp", System.currentTimeMillis());
        
        // Add user-friendly message for desktop notifications
        String message = buildLoanStatusMessage(loanNumber, newStatus);
        payload.put("message", message);
        
        messagingTemplate.convertAndSend("/topic/loans", payload);
        
        // Also send to specific member
        messagingTemplate.convertAndSendToUser(
            String.valueOf(memberId),
            "/queue/notifications",
            payload
        );
    }
    
    /**
     * Build user-friendly message for loan status change
     */
    private String buildLoanStatusMessage(String loanNumber, String newStatus) {
        switch (newStatus.toUpperCase()) {
            case "APPROVED":
                return "Great news! Your loan application " + loanNumber + " has been approved! 🎉";
            case "REJECTED":
                return "Your loan application " + loanNumber + " requires attention. Please contact support.";
            case "DISBURSED":
                return "Your loan " + loanNumber + " has been disbursed to your account! 💰";
            case "PENDING":
                return "Your loan application " + loanNumber + " is being reviewed.";
            case "AWAITING_GUARANTORS":
                return "Your loan " + loanNumber + " is awaiting guarantor approval.";
            case "FULLY_PAID":
                return "Congratulations! Loan " + loanNumber + " has been fully paid! 🎊";
            default:
                return "Loan " + loanNumber + " status updated to " + newStatus;
        }
    }

    /**
     * Notify specific member about guarantor request
     */
    public void notifyGuarantorRequest(Long guarantorMemberId, Long loanId, String loanNumber, String borrowerName, Double guaranteeAmount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "GUARANTOR_REQUEST");
        payload.put("loanId", loanId);
        payload.put("loanNumber", loanNumber);
        payload.put("borrowerName", borrowerName);
        payload.put("guaranteeAmount", guaranteeAmount);
        payload.put("timestamp", System.currentTimeMillis());
        
        // Add user-friendly message
        String message = String.format("%s has requested you to be a guarantor for loan %s (KES %.2f)", 
            borrowerName, loanNumber, guaranteeAmount);
        payload.put("message", message);
        payload.put("requestId", loanId);
        
        messagingTemplate.convertAndSendToUser(
            String.valueOf(guarantorMemberId),
            "/queue/notifications",
            payload
        );
    }

    /**
     * Notify about guarantor response
     */
    public void notifyGuarantorResponse(Long memberId, String guarantorName, String response, Long loanId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "GUARANTOR_RESPONSE");
        payload.put("loanId", loanId);
        payload.put("guarantorName", guarantorName);
        payload.put("response", response);
        payload.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSendToUser(
            String.valueOf(memberId),
            "/queue/notifications",
            payload
        );
    }

    /**
     * Broadcast transaction to relevant users
     */
    public void notifyTransaction(Long memberId, Long transactionId, String transactionType, 
                                   double amount, double newBalance, String accountType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "TRANSACTION");
        payload.put("transactionId", transactionId);
        payload.put("transactionType", transactionType);
        payload.put("amount", amount);
        payload.put("newBalance", newBalance);
        payload.put("accountType", accountType);
        payload.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSendToUser(
            String.valueOf(memberId),
            "/queue/notifications",
            payload
        );
        
        // Also broadcast to treasury/finance for monitoring
        messagingTemplate.convertAndSend("/topic/transactions", payload);
    }

    /**
     * Notify loan repayment
     */
    public void notifyLoanRepayment(Long memberId, Long loanId, String loanNumber, Double amount, Double newOutstanding) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "LOAN_REPAYMENT");
        payload.put("loanId", loanId);
        payload.put("loanNumber", loanNumber);
        payload.put("amount", amount);
        payload.put("newOutstanding", newOutstanding);
        payload.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSendToUser(
            String.valueOf(memberId),
            "/queue/notifications",
            payload
        );
        
        // Broadcast to treasury
        messagingTemplate.convertAndSend("/topic/repayments", payload);
    }

    /**
     * Notify loan disbursement
     */
    public void notifyLoanDisbursed(Long memberId, Long loanId, String loanNumber, Double amount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "LOAN_DISBURSED");
        payload.put("loanId", loanId);
        payload.put("loanNumber", loanNumber);
        payload.put("amount", amount);
        payload.put("timestamp", System.currentTimeMillis());
        
        // Add user-friendly message
        String message = String.format("Your loan %s of KES %.2f has been disbursed! 💰", loanNumber, amount);
        payload.put("message", message);
        
        messagingTemplate.convertAndSendToUser(
            String.valueOf(memberId),
            "/queue/notifications",
            payload
        );
        
        // Broadcast to all staff
        messagingTemplate.convertAndSend("/topic/loans", payload);
    }

    /**
     * Notify top-up request created
     */
    public void notifyTopUpRequestCreated(Long topUpId, Long loanId, String loanNumber, Long memberId, Double amount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "TOPUP_REQUEST_CREATED");
        payload.put("topUpId", topUpId);
        payload.put("loanId", loanId);
        payload.put("loanNumber", loanNumber);
        payload.put("memberId", memberId);
        payload.put("amount", amount);
        payload.put("timestamp", System.currentTimeMillis());
        
        // Notify member
        messagingTemplate.convertAndSendToUser(
            String.valueOf(memberId),
            "/queue/notifications",
            payload
        );
        
        // Broadcast to staff for review
        messagingTemplate.convertAndSend("/topic/topups", payload);
    }

    /**
     * Notify top-up status change
     */
    public void notifyTopUpStatusChanged(Long topUpId, Long loanId, String loanNumber, Long memberId, String oldStatus, String newStatus) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "TOPUP_STATUS_CHANGED");
        payload.put("topUpId", topUpId);
        payload.put("loanId", loanId);
        payload.put("loanNumber", loanNumber);
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);
        payload.put("timestamp", System.currentTimeMillis());
        
        // Notify member
        messagingTemplate.convertAndSendToUser(
            String.valueOf(memberId),
            "/queue/notifications",
            payload
        );
        
        // Broadcast to staff
        messagingTemplate.convertAndSend("/topic/topups", payload);
    }

    /**
     * Notify top-up guarantor request
     */
    public void notifyTopUpGuarantorRequest(Long guarantorMemberId, Long topUpId, Long loanId, String loanNumber, String borrowerName, Double guaranteeAmount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "TOPUP_GUARANTOR_REQUEST");
        payload.put("topUpId", topUpId);
        payload.put("loanId", loanId);
        payload.put("loanNumber", loanNumber);
        payload.put("borrowerName", borrowerName);
        payload.put("guaranteeAmount", guaranteeAmount);
        payload.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSendToUser(
            String.valueOf(guarantorMemberId),
            "/queue/notifications",
            payload
        );
    }

    /**
     * Notify member data update
     */
    public void notifyMemberUpdated(Long memberId, String updateType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "MEMBER_UPDATED");
        payload.put("memberId", memberId);
        payload.put("updateType", updateType);
        payload.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSendToUser(
            String.valueOf(memberId),
            "/queue/notifications",
            payload
        );
        
        // Broadcast to staff
        messagingTemplate.convertAndSend("/topic/members", payload);
    }

    /**
     * Broadcast general notification to all users
     */
    public void broadcastNotification(String message, String type) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "SYSTEM_NOTIFICATION");
        payload.put("message", message);
        payload.put("notificationType", type);
        payload.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSend("/topic/notifications", payload);
    }

    /**
     * Notify deposit request status change
     */
    public void notifyDepositStatusChanged(Long depositId, Long memberId, String oldStatus, String newStatus, Double amount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "DEPOSIT_STATUS_CHANGED");
        payload.put("depositId", depositId);
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);
        payload.put("amount", amount);
        payload.put("timestamp", System.currentTimeMillis());
        
        // Add user-friendly message
        String message = buildDepositStatusMessage(depositId, newStatus, amount);
        payload.put("message", message);
        
        messagingTemplate.convertAndSendToUser(
            String.valueOf(memberId),
            "/queue/notifications",
            payload
        );
    }
    
    /**
     * Build user-friendly message for deposit status change
     */
    private String buildDepositStatusMessage(Long depositId, String newStatus, Double amount) {
        switch (newStatus.toUpperCase()) {
            case "APPROVED":
                return String.format("Your deposit of KES %.2f has been approved! ✅", amount);
            case "COMPLETED":
                return String.format("Your deposit of KES %.2f has been completed! 💵", amount);
            case "REJECTED":
                return String.format("Your deposit request of KES %.2f requires attention.", amount);
            default:
                return String.format("Deposit #%d status updated to %s", depositId, newStatus);
        }
    }
    
    /**
     * Notify deposit request created
     */
    public void notifyDepositRequestCreated(Long depositId, Long memberId, double amount, String accountType) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "DEPOSIT_REQUEST_CREATED");
        payload.put("depositId", depositId);
        payload.put("amount", amount);
        payload.put("accountType", accountType);
        payload.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSendToUser(
            String.valueOf(memberId),
            "/queue/notifications",
            payload
        );
        
        // Broadcast to tellers for review
        messagingTemplate.convertAndSend("/topic/deposits", payload);
    }
    
    /**
     * Notify top-up disbursement
     */
    public void notifyTopUpDisbursed(Long memberId, Long topUpId, Long loanId, String loanNumber, double topUpAmount, double newLoanBalance) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "TOPUP_DISBURSED");
        payload.put("topUpId", topUpId);
        payload.put("loanId", loanId);
        payload.put("loanNumber", loanNumber);
        payload.put("topUpAmount", topUpAmount);
        payload.put("newLoanBalance", newLoanBalance);
        payload.put("timestamp", System.currentTimeMillis());
        
        messagingTemplate.convertAndSendToUser(
            String.valueOf(memberId),
            "/queue/notifications",
            payload
        );
        
        // Broadcast to staff
        messagingTemplate.convertAndSend("/topic/topups", payload);
    }
}
