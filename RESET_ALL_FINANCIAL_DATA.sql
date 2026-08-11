-- =====================================================
-- RESET ALL FINANCIAL DATA (INCLUDING SAVINGS)
-- Date: August 10, 2026
-- =====================================================
-- This script resets ALL financial data to ZERO/EMPTY
-- 
-- WILL BE DELETED:
-- - Loan data (amounts, repayments)
-- - Deposits/Savings
-- - Withdrawals
-- - All financial transactions
-- - Guarantor amounts (relationships kept)
--
-- WILL BE PRESERVED:
-- - Members and their details
-- - Users and authentication
-- - Guarantor relationships (only amounts reset)
-- - Loan products and configuration
-- - System settings
--
-- ⚠️ CRITICAL: BACKUP YOUR DATABASE BEFORE RUNNING THIS!
-- ⚠️ THIS WILL DELETE ALL FINANCIAL HISTORY!
-- =====================================================

-- Disable safe update mode temporarily
SET SQL_SAFE_UPDATES = 0;

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
    interest_collected_manual_override = 0,
    principal_repaid = 0,
    principal_repaid_manual_override = 0,
    total_repayable = 0,
    outstanding_balance = 0,
    monthly_repayment = 0,
    original_principal = 0,
    original_amount = 0,
    total_topup_amount = 0,
    topup_count = 0,
    principal_before_topup = 0,
    topup_additional_amount = 0
WHERE id > 0;

SELECT '✓ Loan amounts reset to zero' AS status;

-- =====================================================
-- STEP 2: Clear ALL Bulk Transaction Items
-- =====================================================
DELETE FROM bulk_transaction_items WHERE id > 0;

SELECT '✓ All bulk transaction items cleared' AS status;

-- =====================================================
-- STEP 3: Delete ALL Loan Repayments
-- =====================================================
DELETE FROM loan_repayments WHERE id > 0;

SELECT '✓ All loan repayments cleared' AS status;

-- =====================================================
-- STEP 4: Reset Loan Top-Up Requests
-- =====================================================
UPDATE loan_topup_requests SET
    requested_amount = 0,
    total_guarantee_amount = 0
WHERE id > 0;

SELECT '✓ Top-up request amounts reset' AS status;

-- =====================================================
-- STEP 5: Reset Top-Up Guarantor Amounts
-- =====================================================
UPDATE topup_guarantors SET
    guarantee_amount = 0,
    pledge_amount = 0
WHERE id > 0;

SELECT '✓ Top-up guarantor amounts reset' AS status;

-- =====================================================
-- STEP 6: Delete ALL Transactions (Loans, Deposits, Withdrawals)
-- =====================================================
DELETE FROM transactions WHERE id > 0;

SELECT '✓ ALL transactions cleared (loans, deposits, withdrawals)' AS status;

-- =====================================================
-- STEP 7: Delete ALL Mpesa Transactions
-- =====================================================
DELETE FROM mpesa_transactions WHERE id > 0;

SELECT '✓ All M-Pesa transactions cleared' AS status;

-- =====================================================
-- STEP 8: Delete Audit Logs for Financial Transactions
-- =====================================================
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
    '=== LOANS (Should be 0) ===' AS section,
    COUNT(*) as total_loans,
    SUM(amount) as total_loan_amount,
    SUM(outstanding_balance) as total_outstanding,
    SUM(principal_repaid) as total_principal_repaid,
    SUM(interest_collected) as total_interest_collected
FROM loans;

-- Check loan repayments are cleared
SELECT 
    '=== LOAN REPAYMENTS (Should be 0) ===' AS section,
    COUNT(*) as remaining_loan_repayments 
FROM loan_repayments;

-- Check ALL transactions are cleared
SELECT 
    '=== TRANSACTIONS (Should be 0) ===' AS section,
    COUNT(*) as total_transactions,
    SUM(amount) as total_amount
FROM transactions;

-- Check M-Pesa transactions are cleared
SELECT 
    '=== MPESA TRANSACTIONS (Should be 0) ===' AS section,
    COUNT(*) as total_mpesa_transactions
FROM mpesa_transactions;

-- Check bulk items are cleared
SELECT 
    '=== BULK TRANSACTION ITEMS (Should be 0) ===' AS section,
    COUNT(*) as total_bulk_items
FROM bulk_transaction_items;

-- Verify members still exist
SELECT 
    '=== MEMBERS (Should be unchanged) ===' AS section,
    COUNT(*) as total_members 
FROM members;

-- Verify users still exist
SELECT 
    '=== USERS (Should be unchanged) ===' AS section,
    COUNT(*) as total_users 
FROM users;

-- =====================================================
-- FINAL SUMMARY
-- =====================================================
SELECT '
╔══════════════════════════════════════════════════╗
║  ALL FINANCIAL DATA RESET COMPLETE               ║
╚══════════════════════════════════════════════════╝

DELETED (Set to 0 or removed):
✓ All loan amounts and repayments
✓ All deposit transactions
✓ All withdrawal transactions
✓ All M-Pesa transactions
✓ All bulk transaction items
✓ All financial audit logs
✓ Guarantor pledge amounts

PRESERVED (NOT TOUCHED):
✓ Members and their details
✓ Users and authentication
✓ Guarantor relationships
✓ Loan products
✓ System configuration

CRITICAL CHECKS:
1. Verify loans = 0 ✓
2. Verify transactions = 0 ✓
3. Verify mpesa_transactions = 0 ✓
4. Verify members COUNT > 0 ✓
5. Verify users COUNT > 0 ✓

Next Steps:
1. Review ALL verification queries above
2. Confirm ALL financial data is cleared
3. Confirm members/users are preserved
4. If satisfied: COMMIT
5. If not satisfied: ROLLBACK

⚠️ WARNING: This deletes ALL financial history!
Once committed, this cannot be undone!

' AS Summary;

-- =====================================================
-- COMMIT OR ROLLBACK
-- =====================================================
-- Review the output above, then UNCOMMENT ONE:

-- COMMIT;  -- ⚠️ UNCOMMENT THIS TO APPLY CHANGES PERMANENTLY

-- ROLLBACK;  -- ⚠️ UNCOMMENT THIS TO UNDO ALL CHANGES

-- Re-enable safe update mode
SET SQL_SAFE_UPDATES = 1;

-- =====================================================
-- END OF SCRIPT
-- =====================================================

/*
USAGE INSTRUCTIONS FOR WINDOWS SERVER:

⚠️ THIS DELETES ALL FINANCIAL DATA INCLUDING SAVINGS! ⚠️

1. BACKUP DATABASE FIRST (ABSOLUTELY MANDATORY):
   Use MySQL Workbench: Server → Data Export → Select minetsacco → Export

2. RUN THIS SCRIPT:
   Open this file in MySQL Workbench and click Execute (⚡)

3. REVIEW THE OUTPUT VERY CAREFULLY:
   CHECK THESE MUST BE TRUE:
   ✓ Loans = 0
   ✓ Loan repayments = 0
   ✓ Transactions = 0 (ALL GONE!)
   ✓ M-Pesa transactions = 0
   ✓ Members COUNT > 0 (preserved)
   ✓ Users COUNT > 0 (preserved)

4. DECIDE TO COMMIT OR ROLLBACK:
   If ALL checks pass: Run "COMMIT;" 
   If ANYTHING is wrong: Run "ROLLBACK;" immediately

5. VERIFY IN APPLICATION:
   - Login to admin portal
   - Check ALL balances show zero
   - Verify members still exist
   - Confirm users can still login

⚠️ CRITICAL WARNING:
This deletes ALL financial history!
Loans, deposits, withdrawals, everything!
Members and users are preserved.
Always keep your backup safe!
*/
