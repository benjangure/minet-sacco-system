-- ========================================
-- SCRIPT 3: DELETE FINANCIAL DATA
-- ========================================
-- ⚠️  CRITICAL WARNING ⚠️
-- This script will DELETE ALL financial data!
-- DO NOT RUN without:
--   1. ✅ Full database backup
--   2. ✅ Saved output from verification script
--   3. ✅ Excel file ready with correct data
--   4. ✅ Authorization to proceed
-- ========================================

-- Use the correct database
USE sacco_db;

-- Display warning
SELECT '================================================' AS '';
SELECT '⚠️  WARNING: FINANCIAL DATA DELETION' AS '';
SELECT '================================================' AS '';
SELECT '' AS '';
SELECT 'This script will delete ALL financial data!' AS '';
SELECT 'Press Ctrl+C NOW if you are not ready!' AS '';
SELECT '' AS '';
SELECT 'Waiting 5 seconds before proceeding...' AS '';
-- Note: In MySQL, you cannot pause execution. 
-- Run this manually and wait before continuing.

-- ========================================
-- STEP 1: Create a snapshot of current state
-- ========================================
SELECT '================================================' AS '';
SELECT 'STEP 1: Creating deletion snapshot' AS '';
SELECT '================================================' AS '';

-- Record counts before deletion
SELECT 'Before deletion - Record counts:' AS '';
SELECT 
    'loans' AS table_name, 
    COUNT(*) AS records_before_deletion 
FROM loans
UNION ALL
SELECT 'loan_repayments', COUNT(*) FROM loan_repayments
UNION ALL
SELECT 'transactions', COUNT(*) FROM transactions
UNION ALL
SELECT 'loan_topup_history', COUNT(*) FROM loan_topup_history;

-- ========================================
-- STEP 2: Disable foreign key checks
-- ========================================
SELECT '' AS '';
SELECT 'STEP 2: Disabling foreign key checks' AS '';
SET FOREIGN_KEY_CHECKS = 0;
SELECT '   ✅ Foreign key checks disabled' AS '';

-- ========================================
-- STEP 3: Delete loan repayments
-- ========================================
SELECT '' AS '';
SELECT 'STEP 3: Deleting loan repayments' AS '';
SELECT '   Records to delete:' AS '', COUNT(*) AS count FROM loan_repayments;

DELETE FROM loan_repayments;

SELECT '   ✅ Loan repayments deleted' AS '';
SELECT '   Remaining records:' AS '', COUNT(*) AS count FROM loan_repayments;

-- ========================================
-- STEP 4: Delete loan top-up history
-- ========================================
SELECT '' AS '';
SELECT 'STEP 4: Deleting loan top-up history' AS '';
SELECT '   Records to delete:' AS '', COUNT(*) AS count FROM loan_topup_history;

DELETE FROM loan_topup_history;

SELECT '   ✅ Loan top-up history deleted' AS '';
SELECT '   Remaining records:' AS '', COUNT(*) AS count FROM loan_topup_history;

-- ========================================
-- STEP 5: Delete all transactions
-- ========================================
SELECT '' AS '';
SELECT 'STEP 5: Deleting transactions' AS '';
SELECT '   Records to delete:' AS '', COUNT(*) AS count FROM transactions;

DELETE FROM transactions;

SELECT '   ✅ Transactions deleted' AS '';
SELECT '   Remaining records:' AS '', COUNT(*) AS count FROM transactions;

-- ========================================
-- STEP 6: Reset loan financial fields
-- ========================================
SELECT '' AS '';
SELECT 'STEP 6: Resetting loan financial fields' AS '';
SELECT '   Loans to update:' AS '', COUNT(*) AS count FROM loans;

UPDATE loans
SET 
    amount = 0.00,
    interest_rate = 0.00,
    term_months = 0,
    monthly_repayment = 0.00,
    total_interest = 0.00,
    interest_collected = 0.00,
    total_repayable = 0.00,
    outstanding_balance = 0.00,
    principal_repaid = 0.00,
    interest_remaining = 0.00,
    original_principal = 0.00,
    total_topup_amount = 0.00,
    topup_count = 0,
    last_topup_date = NULL,
    principal_before_topup = NULL,
    interest_collected_manual_override = 0,
    principal_repaid_manual_override = 0;

SELECT '   ✅ Loan financial fields reset' AS '';

-- ========================================
-- STEP 7: Reset account balances
-- ========================================
SELECT '' AS '';
SELECT 'STEP 7: Resetting account balances' AS '';
SELECT '   Accounts to update:' AS '', COUNT(*) AS count FROM accounts;

UPDATE accounts
SET balance = 0.00;

SELECT '   ✅ Account balances reset' AS '';

-- ========================================
-- STEP 8: Re-enable foreign key checks
-- ========================================
SELECT '' AS '';
SELECT 'STEP 8: Re-enabling foreign key checks' AS '';
SET FOREIGN_KEY_CHECKS = 1;
SELECT '   ✅ Foreign key checks re-enabled' AS '';

-- ========================================
-- STEP 9: Verify deletion
-- ========================================
SELECT '' AS '';
SELECT '================================================' AS '';
SELECT 'STEP 9: Verification of deletion' AS '';
SELECT '================================================' AS '';

SELECT 'After deletion - Record counts:' AS '';
SELECT 
    'loans (should still exist but with zero values)' AS table_name, 
    COUNT(*) AS current_records 
FROM loans
UNION ALL
SELECT 'loan_repayments (should be 0)', COUNT(*) FROM loan_repayments
UNION ALL
SELECT 'transactions (should be 0)', COUNT(*) FROM transactions
UNION ALL
SELECT 'loan_topup_history (should be 0)', COUNT(*) FROM loan_topup_history;

SELECT '' AS '';

-- Verify loan values are reset
SELECT 'Sample loans after reset (first 5):' AS '';
SELECT 
    id,
    loan_number,
    member_id,
    amount,
    interest_rate,
    outstanding_balance,
    interest_collected,
    principal_repaid
FROM loans
ORDER BY id
LIMIT 5;

SELECT '' AS '';

-- Verify account balances are reset
SELECT 'Sample accounts after reset (first 5):' AS '';
SELECT 
    id,
    member_id,
    account_type,
    balance
FROM accounts
ORDER BY id
LIMIT 5;

-- ========================================
-- FINAL STATUS
-- ========================================
SELECT '' AS '';
SELECT '================================================' AS '';
SELECT '✅ FINANCIAL DATA DELETION COMPLETE' AS '';
SELECT '================================================' AS '';
SELECT '' AS '';
SELECT 'Summary:' AS '';
SELECT '  • All loan repayments deleted' AS '';
SELECT '  • All loan top-up history deleted' AS '';
SELECT '  • All transactions deleted' AS '';
SELECT '  • All loan financial values reset to zero' AS '';
SELECT '  • All account balances reset to zero' AS '';
SELECT '  • Member records preserved' AS '';
SELECT '  • User accounts preserved' AS '';
SELECT '  • Loan products preserved' AS '';
SELECT '' AS '';
SELECT 'Next Step: Import correct data from Excel' AS '';
SELECT '  Run: 4_IMPORT_FROM_EXCEL.ps1' AS '';
SELECT '' AS '';
SELECT '⚠️  Database is now in a clean state' AS '';
SELECT '   Ready for correct data import' AS '';
SELECT '================================================' AS '';

-- ========================================
-- RESET AUTO_INCREMENT (Optional)
-- ========================================
-- Uncomment these if you want to reset ID counters
-- This is optional and depends on your requirements

-- ALTER TABLE loan_repayments AUTO_INCREMENT = 1;
-- ALTER TABLE transactions AUTO_INCREMENT = 1;
-- ALTER TABLE loan_topup_history AUTO_INCREMENT = 1;

-- SELECT 'Note: Auto-increment counters NOT reset' AS '';
-- SELECT 'Uncomment the ALTER TABLE statements above if needed' AS '';
