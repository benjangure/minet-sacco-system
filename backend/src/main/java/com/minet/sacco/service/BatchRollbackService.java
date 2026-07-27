package com.minet.sacco.service;

import com.minet.sacco.entity.*;
import com.minet.sacco.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service to handle batch rollback operations for LOAN_MIGRATION and MONTHLY_CONTRIBUTIONS batches.
 * Supports reverting data imported via bulk batch uploads with full audit trail.
 */
@Service
public class BatchRollbackService {

    private static final Logger logger = LoggerFactory.getLogger(BatchRollbackService.class);
    
    private static final int ROLLBACK_DAYS_LIMIT = 30;
    private static final String BATCH_TYPE_LOAN_MIGRATION = "LOAN_MIGRATION";
    private static final String BATCH_TYPE_MONTHLY_CONTRIBUTIONS = "MONTHLY_CONTRIBUTIONS";
    private static final String BATCH_STATUS_COMPLETED = "COMPLETED";
    private static final String BATCH_STATUS_PARTIALLY_COMPLETED = "PARTIALLY_COMPLETED";

    @Autowired
    private BulkBatchRepository bulkBatchRepository;

    @Autowired
    private LoanMigrationItemRepository loanMigrationItemRepository;

    @Autowired
    private GuarantorRepository guarantorRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BulkTransactionItemRepository bulkTransactionItemRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private LoanRepaymentRepository loanRepaymentRepository;

    @Autowired
    private BatchDeletionAuditRepository batchDeletionAuditRepository;

    @Autowired
    private LoanMigrationSnapshotRepository loanMigrationSnapshotRepository;

    /**
     * Rollback a batch based on its type (LOAN_MIGRATION or MONTHLY_CONTRIBUTIONS).
     * Performs comprehensive safety checks before attempting rollback.
     * If rollback fails, writes audit record with FAILED status and error message.
     *
     * @param batchId ID of the batch to rollback
     * @param deletedBy User performing the rollback
     * @param reason Reason for rollback
     * @return Map containing rollback statistics (loansDeleted, guarantorsReleased, etc.)
     * @throws IllegalArgumentException if safety checks fail
     */
    @Transactional
    public Map<String, Object> rollbackBatch(Long batchId, User deletedBy, String reason) {
        int loansDeleted = 0;
        int guarantorsReleased = 0;
        int transactionsReversed = 0;
        int accountsAdjusted = 0;
        String errorMessage = null;

        try {
            // SAFETY CHECK 1: Batch must exist
            BulkBatch batch = bulkBatchRepository.findById(batchId)
                    .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + batchId));

            // SAFETY CHECK 2: Batch status must be COMPLETED or PARTIALLY_COMPLETED
            String batchStatus = batch.getStatus();
            if (!batchStatus.equals(BATCH_STATUS_COMPLETED) && !batchStatus.equals(BATCH_STATUS_PARTIALLY_COMPLETED)) {
                throw new IllegalArgumentException(
                        "Cannot rollback batch with status: " + batchStatus + 
                        ". Only COMPLETED or PARTIALLY_COMPLETED batches can be rolled back."
                );
            }

            // SAFETY CHECK 3: Only process LOAN_MIGRATION or MONTHLY_CONTRIBUTIONS
            String batchType = batch.getBatchType();
            if (!batchType.equals(BATCH_TYPE_LOAN_MIGRATION) && !batchType.equals(BATCH_TYPE_MONTHLY_CONTRIBUTIONS)) {
                throw new IllegalArgumentException(
                        "Rollback not supported for this batch type: " + batchType
                );
            }

            // SAFETY CHECK 4: Batch must not be older than 30 days
            LocalDateTime uploadedAt = batch.getUploadedAt();
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(ROLLBACK_DAYS_LIMIT);
            if (uploadedAt.isBefore(thirtyDaysAgo)) {
                throw new IllegalArgumentException(
                        "Cannot rollback batch older than 30 days. Batch was uploaded on: " + uploadedAt
                );
            }

            // Execute rollback logic based on batch type
            if (batchType.equals(BATCH_TYPE_LOAN_MIGRATION)) {
                Map<String, Integer> result = rollbackLoanMigration(batchId);
                loansDeleted = result.get("loansDeleted");
                guarantorsReleased = result.get("guarantorsReleased");
            } else if (batchType.equals(BATCH_TYPE_MONTHLY_CONTRIBUTIONS)) {
                Map<String, Integer> result = rollbackMonthlyContributions(batchId);
                transactionsReversed = result.get("transactionsReversed");
                accountsAdjusted = result.get("accountsAdjusted");
            }

            // Delete items using native query to avoid validation cascade
            // This prevents Hibernatevalidation from triggering on related Loan entities
            try {
                if (batchType.equals(BATCH_TYPE_LOAN_MIGRATION)) {
                    loanMigrationItemRepository.deleteByBatch_Id(batchId);
                } else if (batchType.equals(BATCH_TYPE_MONTHLY_CONTRIBUTIONS)) {
                    bulkTransactionItemRepository.deleteByBatch_Id(batchId);
                }
            } catch (Exception e) {
                logger.warn("Standard batch deletion had validation issues, using fallback deletion: {}", e.getMessage());
                // Fallback: delete items one-by-one (slower but avoids cascade validation)
                if (batchType.equals(BATCH_TYPE_LOAN_MIGRATION)) {
                    List<LoanMigrationItem> items = loanMigrationItemRepository.findByBatch_Id(batchId);
                    for (LoanMigrationItem item : items) {
                        loanMigrationItemRepository.deleteById(item.getId());
                    }
                } else if (batchType.equals(BATCH_TYPE_MONTHLY_CONTRIBUTIONS)) {
                    List<BulkTransactionItem> items = bulkTransactionItemRepository.findByBatch_Id(batchId);
                    for (BulkTransactionItem item : items) {
                        bulkTransactionItemRepository.deleteById(item.getId());
                    }
                }
            }

            // Write successful audit record
            BatchDeletionAudit audit = new BatchDeletionAudit();
            audit.setBatchId(batchId);
            audit.setBatchNumber(batch.getBatchNumber());
            audit.setBatchType(batchType);
            audit.setDeletedByUserId(deletedBy.getId());
            audit.setDeletedByUsername(deletedBy.getUsername());
            audit.setReason(reason);
            audit.setLoansDeleted(loansDeleted);
            audit.setGuarantorsReleased(guarantorsReleased);
            audit.setTransactionsReversed(transactionsReversed);
            audit.setAccountsAdjusted(accountsAdjusted);
            audit.setRollbackStatus("COMPLETED");
            batchDeletionAuditRepository.save(audit);

            // Delete the batch
            bulkBatchRepository.deleteById(batchId);

            logger.info("Batch {} rolled back successfully. Loans deleted: {}, Guarantors released: {}, " +
                    "Transactions reversed: {}, Accounts adjusted: {}",
                    batchId, loansDeleted, guarantorsReleased, transactionsReversed, accountsAdjusted);

        } catch (Exception e) {
            errorMessage = e.getMessage();
            logger.error("Rollback failed for batch {}: {}", batchId, errorMessage, e);

            // Write failed audit record
            try {
                BulkBatch batch = bulkBatchRepository.findById(batchId).orElse(null);
                BatchDeletionAudit audit = new BatchDeletionAudit();
                if (batch != null) {
                    audit.setBatchId(batchId);
                    audit.setBatchNumber(batch.getBatchNumber());
                    audit.setBatchType(batch.getBatchType());
                }
                audit.setDeletedByUserId(deletedBy.getId());
                audit.setDeletedByUsername(deletedBy.getUsername());
                audit.setReason(reason);
                audit.setRollbackStatus("FAILED");
                audit.setErrorMessage(errorMessage);
                batchDeletionAuditRepository.save(audit);
            } catch (Exception auditEx) {
                logger.error("Failed to write audit record for failed rollback: {}", auditEx.getMessage(), auditEx);
            }

            throw e;
        }

        // Build and return result map
        Map<String, Object> result = new HashMap<>();
        BulkBatch batch = bulkBatchRepository.findById(batchId).orElse(null);
        if (batch != null) {
            result.put("batchNumber", batch.getBatchNumber());
            result.put("batchType", batch.getBatchType());
        }
        result.put("loansDeleted", loansDeleted);
        result.put("guarantorsReleased", guarantorsReleased);
        result.put("transactionsReversed", transactionsReversed);
        result.put("accountsAdjusted", accountsAdjusted);
        return result;
    }

    /**
     * Rollback logic for LOAN_MIGRATION batches with MODE-AWARE handling.
     * 
     * CREATE mode (loan_number is blank):
     *   - Delete entire loan record
     *   - Release active guarantors and restore frozen savings
     *   - Delete all guarantor records
     *   - Delete loan repayments
     *   - Delete disbursement transactions
     * 
     * UPDATE mode (loan_number is populated):
     *   - DO NOT delete the loan
     *   - Restore loan fields from pre-update snapshot (outstanding_balance, term_months, etc.)
     *   - Handle guarantor changes based on what was snapshot
     *
     * @param batchId ID of the batch to rollback
     * @return Map with loansDeleted and guarantorsReleased counts
     */
    private Map<String, Integer> rollbackLoanMigration(Long batchId) {
        int loansDeleted = 0;
        int guarantorsReleased = 0;

        // Get all LoanMigrationItem records for this batch
        List<LoanMigrationItem> items = loanMigrationItemRepository.findByBatch_Id(batchId);

        for (LoanMigrationItem item : items) {
            Loan loan = item.getLoan();
            if (loan != null) {
                String migrationMode = item.getMigrationMode();

                if ("CREATE".equals(migrationMode)) {
                    // ============ CREATE MODE: Delete entire loan ============
                    loansDeleted += rollbackCreateModeLoan(loan);
                } else if ("UPDATE".equals(migrationMode)) {
                    // ============ UPDATE MODE: Restore from snapshot ============
                    guarantorsReleased += rollbackUpdateModeLoan(loan, item);
                }
            }
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("loansDeleted", loansDeleted);
        result.put("guarantorsReleased", guarantorsReleased);
        return result;
    }

    /**
     * Rollback CREATE mode: Delete entire loan and all related data.
     */
    private int rollbackCreateModeLoan(Loan loan) {
        Long loanId = loan.getId();
        Long memberId = loan.getMember().getId();

        // Release all active guarantors and restore frozen savings
        List<Guarantor> guarantors = guarantorRepository.findByLoanId(loanId);
        int guarantorsReleased = 0;

        for (Guarantor guarantor : guarantors) {
            if (guarantor.getStatus().name().equals("ACTIVE")) {
                // Find guarantor's savings account
                Account savingsAccount = accountRepository.findByMemberIdAndAccountType(
                        guarantor.getMember().getId(), Account.AccountType.SAVINGS
                ).orElse(null);

                if (savingsAccount != null && guarantor.getPledgeAmount() != null) {
                    // Subtract pledge amount from frozen savings (never go below zero)
                    BigDecimal newFrozenSavings = savingsAccount.getFrozenSavings()
                            .subtract(guarantor.getPledgeAmount());
                    if (newFrozenSavings.compareTo(BigDecimal.ZERO) < 0) {
                        newFrozenSavings = BigDecimal.ZERO;
                    }
                    savingsAccount.setFrozenSavings(newFrozenSavings);
                    accountRepository.save(savingsAccount);
                }
                guarantorsReleased++;
            }
        }

        // Delete all guarantors for this loan
        guarantorRepository.deleteByLoanId(loanId);

        // Delete any loan repayments associated with this loan
        List<LoanRepayment> repayments = loanRepaymentRepository.findByLoanIdOrderByPaymentDateDesc(loanId);
        for (LoanRepayment repayment : repayments) {
            loanRepaymentRepository.deleteById(repayment.getId());
        }

        // Delete LOAN_DISBURSEMENT transactions created for this member
        List<Transaction> disbursementTxns = transactionRepository.findByTransactionType(
            Transaction.TransactionType.LOAN_DISBURSEMENT
        );
        for (Transaction txn : disbursementTxns) {
            if (txn.getAccount() != null && txn.getAccount().getMember().getId().equals(memberId) &&
                txn.getDescription() != null && txn.getDescription().contains(loan.getLoanNumber())) {
                transactionRepository.deleteById(txn.getId());
            }
        }

        // Delete the loan
        loanRepository.deleteById(loanId);

        logger.info("CREATE mode loan {} rolled back (entire loan deleted)", loanId);
        return 1;
    }

    /**
     * Rollback UPDATE mode: Restore loan fields from pre-update snapshot.
     * The loan itself is NOT deleted, only reverted to pre-update state.
     */
    private int rollbackUpdateModeLoan(Loan loan, LoanMigrationItem item) {
        LoanMigrationSnapshot snapshot = item.getSnapshot();
        if (snapshot == null) {
            logger.warn("UPDATE mode loan {} has no snapshot, cannot restore", loan.getId());
            return 0; // No snapshot means nothing to restore
        }

        // Restore loan fields from snapshot
        loan.setOutstandingBalance(snapshot.getOutstandingBalance());
        loan.setTermMonths(snapshot.getTermMonths());
        loan.setInterestCollected(snapshot.getInterestCollected());
        loan.setDisbursementDate(snapshot.getDisbursementDate());

        loanRepository.save(loan);

        logger.info("UPDATE mode loan {} rolled back (restored to pre-update state)", loan.getId());
        return 0; // UPDATE mode rollback doesn't delete loans, just restores them
    }

    /**
     * Rollback logic for MONTHLY_CONTRIBUTIONS batches.
     * For each successful transaction item:
     * 1. Delete all transactions (savings, shares, fund types, or loan repayment)
     * 2. Restore the corresponding account balance or loan balance
     * 3. Clear FK references before item deletion
     *
     * @param batchId ID of the batch to rollback
     * @return Map with transactionsReversed and accountsAdjusted counts
     */
    private Map<String, Integer> rollbackMonthlyContributions(Long batchId) {
        int transactionsReversed = 0;
        int accountsAdjusted = 0;

        // Get all BulkTransactionItem records where status = SUCCESS
        List<BulkTransactionItem> items = bulkTransactionItemRepository.findByBatch_IdAndStatus(batchId, "SUCCESS");

        for (BulkTransactionItem item : items) {
            // Handle savings transaction
            if (item.getSavingsTransaction() != null) {
                Transaction transaction = item.getSavingsTransaction();
                Account account = transaction.getAccount();

                // Subtract savings amount from account balance
                account.setBalance(account.getBalance().subtract(item.getSavingsAmount()));
                accountRepository.save(account);

                // Delete the transaction
                transactionRepository.deleteById(transaction.getId());
                item.setSavingsTransaction(null);  // Clear FK reference
                transactionsReversed++;
                accountsAdjusted++;
            }

            // Handle shares transaction
            if (item.getSharesTransaction() != null) {
                Transaction transaction = item.getSharesTransaction();
                Account account = transaction.getAccount();

                // Subtract shares amount from account balance
                account.setBalance(account.getBalance().subtract(item.getSharesAmount()));
                accountRepository.save(account);

                // Delete the transaction
                transactionRepository.deleteById(transaction.getId());
                item.setSharesTransaction(null);  // Clear FK reference
                transactionsReversed++;
                accountsAdjusted++;
            }

            // Handle benevolent fund transaction
            if (item.getBenevolentFundTransaction() != null) {
                Transaction transaction = item.getBenevolentFundTransaction();
                Account account = transaction.getAccount();

                // Subtract amount from account balance
                account.setBalance(account.getBalance().subtract(item.getBenevolentFundAmount()));
                accountRepository.save(account);

                // Delete the transaction
                transactionRepository.deleteById(transaction.getId());
                item.setBenevolentFundTransaction(null);  // Clear FK reference
                transactionsReversed++;
                accountsAdjusted++;
            }

            // Handle development fund transaction
            if (item.getDevelopmentFundTransaction() != null) {
                Transaction transaction = item.getDevelopmentFundTransaction();
                Account account = transaction.getAccount();

                // Subtract amount from account balance
                account.setBalance(account.getBalance().subtract(item.getDevelopmentFundAmount()));
                accountRepository.save(account);

                // Delete the transaction
                transactionRepository.deleteById(transaction.getId());
                item.setDevelopmentFundTransaction(null);  // Clear FK reference
                transactionsReversed++;
                accountsAdjusted++;
            }

            // Handle school fees transaction
            if (item.getSchoolFeesTransaction() != null) {
                Transaction transaction = item.getSchoolFeesTransaction();
                Account account = transaction.getAccount();

                // Subtract amount from account balance
                account.setBalance(account.getBalance().subtract(item.getSchoolFeesAmount()));
                accountRepository.save(account);

                // Delete the transaction
                transactionRepository.deleteById(transaction.getId());
                item.setSchoolFeesTransaction(null);  // Clear FK reference
                transactionsReversed++;
                accountsAdjusted++;
            }

            // Handle holiday fund transaction
            if (item.getHolidayFundTransaction() != null) {
                Transaction transaction = item.getHolidayFundTransaction();
                Account account = transaction.getAccount();

                // Subtract amount from account balance
                account.setBalance(account.getBalance().subtract(item.getHolidayFundAmount()));
                accountRepository.save(account);

                // Delete the transaction
                transactionRepository.deleteById(transaction.getId());
                item.setHolidayFundTransaction(null);  // Clear FK reference
                transactionsReversed++;
                accountsAdjusted++;
            }

            // Handle emergency fund transaction
            if (item.getEmergencyFundTransaction() != null) {
                Transaction transaction = item.getEmergencyFundTransaction();
                Account account = transaction.getAccount();

                // Subtract amount from account balance
                account.setBalance(account.getBalance().subtract(item.getEmergencyFundAmount()));
                accountRepository.save(account);

                // Delete the transaction
                transactionRepository.deleteById(transaction.getId());
                item.setEmergencyFundTransaction(null);  // Clear FK reference
                transactionsReversed++;
                accountsAdjusted++;
            }

            // Handle loan repayment - restore principal AND interest separately
            if (item.getLoanRepayment() != null) {
                LoanRepayment repayment = item.getLoanRepayment();
                Loan loan = repayment.getLoan();

                // Restore outstanding balance by adding back ONLY the principal portion
                BigDecimal principalToRestore = repayment.getPrincipalAmount();
                if (principalToRestore != null && principalToRestore.compareTo(BigDecimal.ZERO) > 0) {
                    loan.setOutstandingBalance(
                            loan.getOutstandingBalance().add(principalToRestore)
                    );
                }

                // Restore interest collected by subtracting ONLY the interest portion
                // Never allow negative interest - cap at zero
                BigDecimal interestToRestore = repayment.getInterestAmount();
                if (interestToRestore != null && interestToRestore.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal currentInterestCollected = loan.getInterestCollected() != null ? 
                            loan.getInterestCollected() : BigDecimal.ZERO;
                    BigDecimal newInterestCollected = currentInterestCollected.subtract(interestToRestore);
                    // Never go below zero
                    if (newInterestCollected.compareTo(BigDecimal.ZERO) < 0) {
                        newInterestCollected = BigDecimal.ZERO;
                    }
                    loan.setInterestCollected(newInterestCollected);
                }

                loanRepository.save(loan);

                // Delete the loan repayment record
                loanRepaymentRepository.deleteById(repayment.getId());
                item.setLoanRepayment(null);  // Clear FK reference
                transactionsReversed++;
                accountsAdjusted++;
            }

            // Save the item with all FK references cleared
            bulkTransactionItemRepository.save(item);
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("transactionsReversed", transactionsReversed);
        result.put("accountsAdjusted", accountsAdjusted);
        return result;
    }
}
