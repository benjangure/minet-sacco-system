-- =====================================================
-- RESET FINANCIAL FIGURES ONLY
-- Date: August 10, 2026
-- =====================================================
-- This script resets all financial amounts and balances to ZERO
-- while preserving:
-- - Members and their details
-- - Users and authentication
-- - Guarantors and guarantor relationships
-- - Loan products and configuration
-- - System settings
--
-- ⚠️ CRITICAL: BACKUP YOUR DATABASE BEFORE RUNNING THIS!
-- =====================================================

-- Start transaction for safety
START TRANSACTION;

-- =====================================================
-- STEP 1: Reset Loan Financial Figures
-- =====================================================
UPDATE loans SET
    amount = 0,
    total_interest = 0,
    interest_remaining = 0,
    interest_collected = 0,
    interest_collected_manual_override = NULL,
    principal_repaid = 0,
    principal_repaid_manual_override = NULL,
    total_repayable = 0,
    outstanding_balance = 0,
    monthly_repayment = 0,
    original_principal = 0,
    original_amount = 0,
    total_topup_amount = 0,
    topup_count = 0,
    principal_before_topup = 0
WHERE id > 0;

SELECT '✓ Loan amounts reset to zero' AS status;

-- =====================================================
-- STEP 2: Clear Bulk Transaction Items First (Foreign Key Dependency)
-- =====================================================
DELETE FROM bulk_transaction_items WHERE id > 0;

SELECT '✓ Bulk transaction items cleared' AS status;

-- =====================================================
-- STEP 3: Reset Loan Repayments
-- =====================================================
DELETE FROM loan_repayments WHERE id > 0;

SELECT '✓ Loan repayments cleared' AS status;

-- =====================================================
-- STEP 4: Reset Loan Top-Up Requests
-- =====================================================
UPDATE loan_topup_requests SET
    requested_amount = 0,
    approved_amount = 0
WHERE id > 0;

SELECT '✓ Top-up request amounts reset' AS status;

-- =====================================================
-- STEP 4: Reset Member Account Balances
-- =====================================================
UPDATE member_accounts SET
    balance = 0,
    available_balance = 0
WHERE id > 0;

SELECT '✓ Member account balances reset to zero' AS status;

-- =====================================================
-- STEP 5: Reset Savings/Deposits
-- =====================================================
DELETE FROM deposits WHERE id > 0;

SELECT '✓ Deposits/savings cleared' AS status;

-- =====================================================
-- STEP 6: Reset Withdrawals
-- =====================================================
DELETE FROM withdrawals WHERE id > 0;

SELECT '✓ Withdrawals cleared' AS status;

-- =====================================================
-- STEP 7: Reset Transactions
-- =====================================================
DELETE FROM transactions WHERE id > 0;

SELECT '✓ All transactions cleared' AS status;

-- =====================================================
-- STEP 8: Reset Mpesa Transactions
-- =====================================================
DELETE FROM mpesa_transactions WHERE id > 0;

SELECT '✓ M-Pesa transactions cleared' AS status;

-- =====================================================
-- STEP 9: Reset Guarantor Pledge Amounts (Keep Relationships)
-- =====================================================
UPDATE loan_guarantors SET
    guarantee_amount = 0,
    amount_pledged = 0
WHERE id > 0;

SELECT '✓ Guarantor pledge amounts reset (relationships preserved)' AS status;

-- =====================================================
-- STEP 10: Reset GL Manual Entries
-- =====================================================
DELETE FROM gl_manual_entries WHERE id > 0;

SELECT '✓ GL manual entries cleared' AS status;

-- =====================================================
-- STEP 11: Reset Audit Logs Related to Financial Transactions
-- =====================================================
-- Optional: Keep audit logs for compliance, or clear financial transaction logs only
DELETE FROM audit_logs WHERE action IN (
    'LOAN_REPAYMENT', 
    'DEPOSIT', 
    'WITHDRAWAL', 
    'MPESA_PAYMENT',
    'LOAN_DISBURSEMENT',
    'TOP_UP_APPROVED'
) AND id > 0;

SELECT '✓ Financial transaction audit logs cleared' AS status;

-- =====================================================
-- VERIFICATION QUERIES
-- =====================================================

-- Check loan amounts are zero
SELECT 
    COUNT(*) as total_loans,
    SUM(amount) as total_amount,
    SUM(outstanding_balance) as total_outstanding
FROM loans;

-- Check member account balances are zero
SELECT 
    COUNT(*) as total_accounts,
    SUM(balance) as total_balance
FROM member_accounts;

-- Check repayments are cleared
SELECT COUNT(*) as remaining_repayments FROM loan_repayments;

-- Check transactions are cleared
SELECT COUNT(*) as remaining_transactions FROM transactions;

-- Verify members still exist
SELECT COUNT(*) as total_members FROM members;

-- Verify guarantors still exist
SELECT COUNT(*) as total_guarantor_relationships FROM loan_guarantors;

-- Verify users still exist
SELECT COUNT(*) as total_users FROM users;

-- =====================================================
-- FINAL SUMMARY
-- =====================================================
SELECT '
╔══════════════════════════════════════════════════╗
║  FINANCIAL DATA RESET COMPLETE                   ║
╚══════════════════════════════════════════════════╝

✓ All loan amounts set to 0
✓ All repayments cleared
✓ All member balances set to 0
✓ All deposits/withdrawals cleared
✓ All transactions cleared
✓ Guarantor amounts reset (relationships kept)

PRESERVED:
✓ Members and their details
✓ Users and authentication
✓ Guarantor relationships
✓ Loan products
✓ System configuration
✓ Member credentials

Next Steps:
1. Review the verification queries above
2. If satisfied, COMMIT the transaction
3. If not satisfied, ROLLBACK

' AS Summary;

-- =====================================================
-- COMMIT OR ROLLBACK
-- =====================================================
-- UNCOMMENT ONE OF THE FOLLOWING:

-- COMMIT;  -- ⚠️ UNCOMMENT THIS TO APPLY CHANGES PERMANENTLY

-- ROLLBACK;  -- ⚠️ UNCOMMENT THIS TO UNDO ALL CHANGES

-- =====================================================
-- END OF SCRIPT
-- =====================================================

/*
USAGE INSTRUCTIONS FOR WINDOWS SERVER:

1. BACKUP DATABASE FIRST:
   mysqldump -u root -p minetsacco > backup_before_reset.sql

2. RUN THIS SCRIPT:
   mysql -u root -p minetsacco < RESET_FINANCIAL_FIGURES_ONLY.sql

3. REVIEW THE OUTPUT:
   - Check all verification queries
   - Ensure counts look correct

4. CHOOSE TO COMMIT OR ROLLBACK:
   - If everything looks good: Run "COMMIT;" in MySQL Workbench
   - If something is wrong: Run "ROLLBACK;" immediately

5. VERIFY IN APPLICATION:
   - Login to admin portal
   - Check member accounts show zero balances
   - Verify guarantor relationships still exist
   - Confirm members can still login

⚠️ WARNING:
This is a PERMANENT operation once committed.
Always keep your backup safe!
*/
