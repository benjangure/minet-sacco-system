-- =====================================================
-- RESET LOAN DATA ONLY (KEEP SAVINGS/DEPOSITS)
-- Date: August 10, 2026
-- =====================================================
-- This script resets ONLY loan-related financial data
-- 
-- WILL BE RESET:
-- - Loan amounts and balances
-- - Loan repayments
-- - Loan guarantor amounts (relationships kept)
-- - Loan-related transactions only
--
-- WILL BE PRESERVED:
-- - Members and their details
-- - Users and authentication
-- - Savings/Deposits (ALL KEPT)
-- - Withdrawals (ALL KEPT)
-- - Member account balances (ALL KEPT)
-- - Guarantor relationships (only amounts reset)
-- - Loan products and configuration
--
-- ⚠️ CRITICAL: BACKUP YOUR DATABASE BEFORE RUNNING THIS!
-- =====================================================

-- Disable safe update mode temporarily
SET SQL_SAFE_UPDATES = 0;

-- Start transaction for safety
START TRANSACTION;

-- =====================================================
-- STEP 1: Reset Loan Financial Figures ONLY
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
-- STEP 2: Clear Bulk Transaction Items (Loan-related only)
-- =====================================================
DELETE FROM bulk_transaction_items 
WHERE loan_repayment_id IS NOT NULL
   OR loan_repayment_amount > 0
   OR loan_repayment_principal_amount > 0
   OR loan_repayment_interest_amount > 0;

SELECT '✓ Loan-related bulk transaction items cleared' AS status;

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
-- STEP 6: Clear ONLY Loan-Related Transactions
-- =====================================================
DELETE FROM transactions 
WHERE transaction_type IN (
    'LOAN_DISBURSEMENT',
    'LOAN_REPAYMENT',
    'INTEREST'
);

SELECT '✓ Loan-related transactions cleared (deposit/withdrawal transactions kept)' AS status;

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

-- Check only loan transactions are cleared
SELECT 
    '=== TRANSACTIONS BY TYPE ===' AS section,
    transaction_type,
    COUNT(*) as count,
    SUM(amount) as total_amount
FROM transactions
GROUP BY transaction_type;

-- Verify DEPOSIT transactions are STILL THERE (should NOT be 0)
SELECT 
    '=== DEPOSIT TRANSACTIONS (Should NOT be 0) ===' AS section,
    COUNT(*) as total_deposit_transactions,
    SUM(amount) as total_deposit_amount
FROM transactions
WHERE transaction_type = 'DEPOSIT';

-- Verify WITHDRAWAL transactions are STILL THERE (should NOT be 0)
SELECT 
    '=== WITHDRAWAL TRANSACTIONS (Should NOT be 0) ===' AS section,
    COUNT(*) as total_withdrawal_transactions,
    SUM(amount) as total_withdrawal_amount
FROM transactions
WHERE transaction_type = 'WITHDRAWAL';

-- Verify member account balances are STILL THERE (should NOT be 0)
SELECT 
    '=== MEMBER ACCOUNTS (Should NOT be 0) ===' AS section,
    COUNT(*) as total_accounts,
    SUM(balance) as total_member_balances
FROM member_accounts
WHERE id > 0;

-- Verify members still exist
SELECT 
    '=== MEMBERS (Should be unchanged) ===' AS section,
    COUNT(*) as total_members 
FROM members;

-- Verify guarantor relationships still exist
SELECT 
    '=== GUARANTORS (Should be unchanged) ===' AS section,
    COUNT(*) as total_guarantor_relationships 
FROM loan_guarantors;

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
║  LOAN DATA RESET COMPLETE                        ║
╚══════════════════════════════════════════════════╝

RESET (Set to 0):
✓ All loan amounts
✓ All loan repayments
✓ Loan guarantor amounts (relationships kept)
✓ Top-up request amounts
✓ Loan-related transactions only

PRESERVED (NOT TOUCHED):
✓ Members and their details
✓ Users and authentication
✓ ALL Savings/Deposits ← KEPT
✓ ALL Withdrawals ← KEPT
✓ Member account balances ← KEPT
✓ Guarantor relationships ← KEPT
✓ Loan products
✓ Non-loan transactions (DEPOSIT/WITHDRAWAL)

CRITICAL CHECKS:
1. Verify loans = 0 ✓
2. Verify deposits COUNT > 0 (MUST BE NON-ZERO)
3. Verify withdrawals COUNT > 0 (MUST BE NON-ZERO)
4. Verify member_accounts balance > 0 (MUST BE NON-ZERO)
5. Verify DEPOSIT/WITHDRAWAL transactions still exist

Next Steps:
1. Review ALL verification queries above carefully
2. If deposits/withdrawals/balances are PRESERVED: COMMIT
3. If something is wrong: ROLLBACK immediately

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

1. BACKUP DATABASE FIRST (MANDATORY):
   In PowerShell:
   mysqldump -u root -p minetsacco > backup_before_loan_reset.sql

2. RUN THIS SCRIPT:
   Open this file in MySQL Workbench and click Execute (⚡)
   OR in PowerShell:
   Get-Content RESET_LOAN_DATA_ONLY.sql | mysql -u root -p minetsacco

3. REVIEW THE OUTPUT VERY CAREFULLY:
   CHECK THESE MUST BE TRUE:
   ✓ Loan amounts = 0
   ✓ Loan repayments COUNT = 0
   ✓ Deposits COUNT > 0 (NOT ZERO!)
   ✓ Withdrawals COUNT > 0 (NOT ZERO!)
   ✓ Member account balances > 0 (NOT ZERO!)
   ✓ DEPOSIT transactions still exist
   ✓ WITHDRAWAL transactions still exist

4. DECIDE TO COMMIT OR ROLLBACK:
   If ALL checks pass: Run "COMMIT;" 
   If ANYTHING is wrong: Run "ROLLBACK;" immediately

5. VERIFY IN APPLICATION:
   - Login to admin portal
   - Check loans show zero balances ✓
   - CHECK: Member savings/deposits show correct amounts ✓
   - CHECK: Withdrawal history is intact ✓
   - Verify guarantor relationships exist ✓

⚠️ CRITICAL WARNING:
This resets ONLY loan data.
Savings, deposits, withdrawals, and account balances MUST be preserved.
If verification shows deposits/withdrawals = 0, DO NOT COMMIT!
Always keep your backup safe!
*/
