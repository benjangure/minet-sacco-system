package com.minet.sacco.service;

import com.minet.sacco.entity.Transaction;
import com.minet.sacco.entity.Account;
import com.minet.sacco.repository.TransactionRepository;
import com.minet.sacco.repository.AccountRepository;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Optional;

@Service
public class MpesaCallbackService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    /**
     * Handle STK Push callback (Deposit)
     * Called when user completes M-Pesa payment
     */
    public void handleStkPushCallback(JsonObject callbackData) {
        try {
            System.out.println("DEBUG: STK Callback received: " + callbackData.toString());
            
            // Extract callback data
            JsonObject body = callbackData.getAsJsonObject("Body");
            JsonObject stkCallback = body.getAsJsonObject("stkCallback");
            
            int resultCode = stkCallback.get("ResultCode").getAsInt();
            String resultDesc = stkCallback.get("ResultDesc").getAsString();
            String checkoutRequestId = stkCallback.get("CheckoutRequestID").getAsString();

            System.out.println("DEBUG: Result Code: " + resultCode + ", Desc: " + resultDesc + ", CheckoutID: " + checkoutRequestId);

            // Find transaction by checkout request ID
            Optional<Transaction> transactionOpt = transactionRepository.findAll().stream()
                .filter(t -> t.getDescription() != null && 
                           t.getDescription().contains(checkoutRequestId))
                .findFirst();

            if (!transactionOpt.isPresent()) {
                System.err.println("Transaction not found for checkout ID: " + checkoutRequestId);
                return;
            }

            Transaction transaction = transactionOpt.get();

            if (resultCode == 0) {
                // Payment successful
                JsonObject callbackMetadata = stkCallback.getAsJsonObject("CallbackMetadata");
                var itemArray = callbackMetadata.getAsJsonArray("Item");
                
                // Extract amount and receipt number from metadata
                long amount = 0;
                String receiptNumber = "";
                
                for (int i = 0; i < itemArray.size(); i++) {
                    JsonObject item = itemArray.get(i).getAsJsonObject();
                    String name = item.get("Name").getAsString();
                    
                    if ("Amount".equals(name)) {
                        amount = item.get("Value").getAsLong();
                    } else if ("MpesaReceiptNumber".equals(name)) {
                        receiptNumber = item.get("Value").getAsString();
                    }
                }

                System.out.println("DEBUG: Payment successful - Amount: " + amount + ", Receipt: " + receiptNumber);

                // Credit the account
                Account account = transaction.getAccount();
                account.setBalance(account.getBalance().add(new BigDecimal(amount)));
                account.setUpdatedAt(java.time.LocalDateTime.now());
                accountRepository.save(account);

                // Update transaction
                transaction.setDescription("M-Pesa deposit - COMPLETED - Receipt: " + receiptNumber + " - Amount: " + amount);
                transactionRepository.save(transaction);

                System.out.println("Deposit successful: " + amount + " for account: " + account.getId());
            } else {
                // Payment failed
                transaction.setDescription("M-Pesa deposit - FAILED - " + resultDesc);
                transactionRepository.save(transaction);

                System.out.println("Deposit failed: " + resultDesc);
            }
        } catch (Exception e) {
            System.err.println("Error processing STK callback: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle B2C callback (Withdrawal)
     * Called when money is sent to member's M-Pesa
     */
    public void handleB2CCallback(JsonObject callbackData) {
        try {
            System.out.println("DEBUG: B2C Callback received: " + callbackData.toString());
            
            // Extract callback data
            JsonObject result = callbackData.getAsJsonObject("Result");
            
            int resultCode = result.get("ResultCode").getAsInt();
            String resultDesc = result.get("ResultDesc").getAsString();
            String conversationId = result.get("OriginatorConversationID").getAsString();

            System.out.println("DEBUG: B2C Result Code: " + resultCode + ", Desc: " + resultDesc + ", ConversationID: " + conversationId);

            // Find transaction by conversation ID
            Optional<Transaction> transactionOpt = transactionRepository.findAll().stream()
                .filter(t -> t.getDescription() != null && 
                           t.getDescription().contains(conversationId))
                .findFirst();

            if (!transactionOpt.isPresent()) {
                System.err.println("Transaction not found for conversation ID: " + conversationId);
                return;
            }

            Transaction transaction = transactionOpt.get();

            if (resultCode == 0) {
                // Withdrawal successful
                transaction.setDescription("M-Pesa withdrawal - COMPLETED - Conversation: " + conversationId);
                transactionRepository.save(transaction);

                System.out.println("Withdrawal successful: " + transaction.getAmount());
            } else {
                // Withdrawal failed - restore balance
                Account account = transaction.getAccount();
                account.setBalance(account.getBalance().add(transaction.getAmount()));
                account.setUpdatedAt(java.time.LocalDateTime.now());
                accountRepository.save(account);

                transaction.setDescription("M-Pesa withdrawal - FAILED - " + resultDesc);
                transactionRepository.save(transaction);

                System.out.println("Withdrawal failed: " + resultDesc);
            }
        } catch (Exception e) {
            System.err.println("Error processing B2C callback: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Handle timeout callback
     * Called when B2C payment times out
     */
    public void handleTimeoutCallback(JsonObject callbackData) {
        try {
            JsonObject result = callbackData.getAsJsonObject("Result");
            String conversationId = result.get("OriginatorConversationID").getAsString();

            Optional<Transaction> transactionOpt = transactionRepository.findAll().stream()
                .filter(t -> t.getDescription() != null && 
                           t.getDescription().contains(conversationId))
                .findFirst();

            if (transactionOpt.isPresent()) {
                Transaction transaction = transactionOpt.get();
                
                // Restore balance for withdrawal timeout
                if (transaction.getTransactionType().equals(Transaction.TransactionType.WITHDRAWAL)) {
                    Account account = transaction.getAccount();
                    account.setBalance(account.getBalance().add(transaction.getAmount()));
                    account.setUpdatedAt(java.time.LocalDateTime.now());
                    accountRepository.save(account);
                }

                transaction.setDescription("M-Pesa transaction - TIMEOUT");
                transactionRepository.save(transaction);
            }
        } catch (Exception e) {
            System.err.println("Error processing timeout callback: " + e.getMessage());
        }
    }
}
