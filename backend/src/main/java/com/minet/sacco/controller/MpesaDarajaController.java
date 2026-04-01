package com.minet.sacco.controller;

import com.minet.sacco.entity.Transaction;
import com.minet.sacco.entity.Account;
import com.minet.sacco.entity.Member;
import com.minet.sacco.repository.TransactionRepository;
import com.minet.sacco.repository.AccountRepository;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.UserRepository;
import com.minet.sacco.service.MpesaStkPushService;
import com.minet.sacco.service.MpesaB2CService;
import com.minet.sacco.service.MpesaCallbackService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/mpesa")
@CrossOrigin
public class MpesaDarajaController {

    @Autowired
    private MpesaStkPushService stkPushService;

    @Autowired
    private MpesaB2CService b2cService;

    @Autowired
    private MpesaCallbackService callbackService;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get current authenticated member
     */
    private Member getCurrentMember() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        
        Optional<com.minet.sacco.entity.User> userOpt = userRepository.findByUsername(username);
        if (!userOpt.isPresent() || userOpt.get().getMemberId() == null) {
            throw new RuntimeException("Member not found");
        }
        
        Optional<Member> memberOpt = memberRepository.findById(userOpt.get().getMemberId());
        if (!memberOpt.isPresent()) {
            throw new RuntimeException("Member not found");
        }
        
        return memberOpt.get();
    }

    /**
     * Initiate M-Pesa deposit via STK Push
     * Sends prompt to member's phone to enter M-Pesa PIN
     */
    @PostMapping("/deposit/initiate")
    public ResponseEntity<?> initiateDeposit(@RequestBody Map<String, Object> request) {
        try {
            Member member = getCurrentMember();
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String phoneNumber = request.get("phoneNumber").toString();

            // Validate amount
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", "Amount must be greater than zero"
                ));
            }

            // Initiate STK Push
            JsonObject stkResponse = stkPushService.initiateStkPush(phoneNumber, amount.longValue());

            if (stkResponse.has("error") && stkResponse.get("error").getAsBoolean()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", stkResponse.get("message").getAsString()
                ));
            }

            // Check for Daraja error response
            if (stkResponse.has("ResponseCode")) {
                String responseCode = stkResponse.get("ResponseCode").getAsString();
                if (!"0".equals(responseCode)) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", true,
                        "message", stkResponse.get("ResponseDescription").getAsString()
                    ));
                }
            }

            // Get checkout request ID
            String checkoutRequestId = stkResponse.get("CheckoutRequestID").getAsString();

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "STK Push sent to " + phoneNumber + ". Please enter your M-Pesa PIN to complete the payment.",
                "checkoutRequestId", checkoutRequestId,
                "responseCode", stkResponse.get("ResponseCode").getAsString()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", true,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Initiate M-Pesa withdrawal via B2C
     * Money will be sent to member's M-Pesa account
     */
    @PostMapping("/withdraw/initiate")
    public ResponseEntity<?> initiateWithdrawal(@RequestBody Map<String, Object> request) {
        try {
            Member member = getCurrentMember();
            BigDecimal amount = new BigDecimal(request.get("amount").toString());
            String phoneNumber = request.get("phoneNumber").toString();

            // Validate amount
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", "Amount must be greater than zero"
                ));
            }

            // Get savings account
            Optional<Account> savingsAccountOpt = accountRepository.findByMemberIdAndAccountType(
                member.getId(), Account.AccountType.SAVINGS
            );

            if (!savingsAccountOpt.isPresent()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", "Savings account not found"
                ));
            }

            Account savingsAccount = savingsAccountOpt.get();

            // Check balance
            if (savingsAccount.getBalance().compareTo(amount) < 0) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", "Insufficient balance"
                ));
            }

            // Call B2C API first (before deducting balance)
            JsonObject b2cResponse = b2cService.sendB2CPayment(phoneNumber, amount.longValue(), "BusinessPayment");

            if (b2cResponse.has("error") && b2cResponse.get("error").getAsBoolean()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", b2cResponse.get("message").getAsString()
                ));
            }

            // Only deduct from account after B2C call succeeds
            savingsAccount.setBalance(savingsAccount.getBalance().subtract(amount));
            savingsAccount.setUpdatedAt(java.time.LocalDateTime.now());
            accountRepository.save(savingsAccount);

            // Create transaction only after balance is deducted
            String conversationId = b2cResponse.get("OriginatorConversationID").getAsString();
            
            Transaction transaction = new Transaction();
            transaction.setAccount(savingsAccount);
            transaction.setTransactionType(Transaction.TransactionType.WITHDRAWAL);
            transaction.setAmount(amount);
            transaction.setTransactionDate(java.time.LocalDateTime.now());
            transaction.setDescription("M-Pesa B2C - PENDING - ConversationID: " + conversationId);
            transactionRepository.save(transaction);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Withdrawal initiated. Money will be sent to " + phoneNumber,
                "conversationId", conversationId,
                "responseCode", b2cResponse.get("ResponseCode").getAsString()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", true,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Callback endpoint for STK Push (Deposit)
     * Daraja calls this when user completes payment
     */
    @PostMapping("/callback/stk")
    public ResponseEntity<?> stkCallback(@RequestBody String callbackBody) {
        try {
            System.out.println("=== STK CALLBACK RECEIVED ===");
            System.out.println("Raw body: " + callbackBody);
            
            JsonObject callbackData = JsonParser.parseString(callbackBody).getAsJsonObject();
            System.out.println("Parsed JSON: " + callbackData.toString());
            
            callbackService.handleStkPushCallback(callbackData);
            
            System.out.println("=== STK CALLBACK PROCESSED ===");
            return ResponseEntity.ok(Map.of("ResultCode", 0));
        } catch (Exception e) {
            System.err.println("Error in STK callback: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(Map.of("ResultCode", 1));
        }
    }

    /**
     * Check STK Push status and update account if payment successful
     * Member calls this after completing M-Pesa payment
     */
    @PostMapping("/deposit/confirm")
    public ResponseEntity<?> confirmDeposit(@RequestBody Map<String, Object> request) {
        try {
            Member member = getCurrentMember();
            String checkoutRequestId = request.get("checkoutRequestId").toString();

            // Query Daraja for payment status
            JsonObject statusResponse = stkPushService.queryStkPushStatus(checkoutRequestId);

            System.out.println("DEBUG: Status response: " + statusResponse.toString());

            if (statusResponse.has("error") && statusResponse.get("error").getAsBoolean()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", statusResponse.get("message").getAsString()
                ));
            }

            // Check result code
            String resultCode = statusResponse.get("ResultCode").getAsString();
            String resultDesc = statusResponse.get("ResultDesc").getAsString();

            if ("0".equals(resultCode)) {
                // Payment successful - find and update transaction
                Optional<Transaction> transactionOpt = transactionRepository.findAll().stream()
                    .filter(t -> t.getDescription() != null && 
                               t.getDescription().contains(checkoutRequestId))
                    .findFirst();

                if (transactionOpt.isPresent()) {
                    Transaction transaction = transactionOpt.get();
                    Account account = transaction.getAccount();

                    // Credit the account if not already credited
                    if (transaction.getDescription().contains("PENDING")) {
                        account.setBalance(account.getBalance().add(transaction.getAmount()));
                        account.setUpdatedAt(java.time.LocalDateTime.now());
                        accountRepository.save(account);

                        transaction.setDescription("M-Pesa deposit - COMPLETED - CheckoutID: " + checkoutRequestId);
                        transactionRepository.save(transaction);
                    }

                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Payment confirmed! KES " + transaction.getAmount() + " has been credited to your account.",
                        "newBalance", account.getBalance(),
                        "status", "COMPLETED"
                    ));
                } else {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", true,
                        "message", "Transaction not found"
                    ));
                }
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", true,
                    "message", "Payment not completed: " + resultDesc
                ));
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", true,
                "message", "Error: " + e.getMessage()
            ));
        }
    }

    /**
     * Callback endpoint for B2C (Withdrawal)
     * Daraja calls this when money is sent
     */
    @PostMapping("/callback/b2c")
    public ResponseEntity<?> b2cCallback(@RequestBody String callbackBody) {
        try {
            JsonObject callbackData = JsonParser.parseString(callbackBody).getAsJsonObject();
            callbackService.handleB2CCallback(callbackData);
            
            return ResponseEntity.ok(Map.of("ResultCode", 0));
        } catch (Exception e) {
            System.err.println("Error in B2C callback: " + e.getMessage());
            return ResponseEntity.ok(Map.of("ResultCode", 1));
        }
    }

    /**
     * Timeout callback endpoint for B2C
     * Daraja calls this if B2C times out
     */
    @PostMapping("/callback/timeout")
    public ResponseEntity<?> timeoutCallback(@RequestBody String callbackBody) {
        try {
            JsonObject callbackData = JsonParser.parseString(callbackBody).getAsJsonObject();
            callbackService.handleTimeoutCallback(callbackData);
            
            return ResponseEntity.ok(Map.of("ResultCode", 0));
        } catch (Exception e) {
            System.err.println("Error in timeout callback: " + e.getMessage());
            return ResponseEntity.ok(Map.of("ResultCode", 1));
        }
    }

    /**
     * Query STK Push status
     * Admin can check if deposit was completed
     */
    @GetMapping("/status/stk/{checkoutRequestId}")
    public ResponseEntity<?> queryStkStatus(@PathVariable String checkoutRequestId) {
        try {
            JsonObject statusResponse = stkPushService.queryStkPushStatus(checkoutRequestId);
            return ResponseEntity.ok(statusResponse);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", true,
                "message", "Error: " + e.getMessage()
            ));
        }
    }
}
