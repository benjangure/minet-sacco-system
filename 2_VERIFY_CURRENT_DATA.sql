-- ========================================
-- SCRIPT 2: VERIFY CURRENT DATA
-- ========================================
-- This script documents the current state of financial data
-- before deletion. Run this and save the output!
-- ========================================

-- Use the correct database
USE sacco_db;

-- Display header
SELECT '================================================' AS '';
SELECT 'MINET SACCO - CURRENT DATA VERIFICATION' AS '';
SELECT 'Date/Time:' AS 'INFO', NOW() AS 'VALUE';
SELECT '================================================' AS '';
SELECT '' AS '';

-- ========================================
-- 1. RECORD COUNTS
-- ========================================
SELECT '1. RECORD COUNTS IN EACH TABLE' AS '';
SELECT '---------------------------------------' AS '';

SELECT 'loans' AS table_name, COUNT(*) AS record_count FROM loans
UNION ALL
SELECT 'loan_repayments', COUNT(*) FROM loan_repayments
UNION ALL
SELECT 'transactions', COUNT(*) FROM transactions
UNION ALL
SELECT 'accounts', COUNT(*) FROM accounts
UNION ALL
SELECT 'loan_topup_history', COUNT(*) FROM loan_topup_history
UNION ALL
SELECT 'guarantors', COUNT(*) FROM guarantors;

SELECT '' AS '';

-- ========================================
-- 2. LOAN FINANCIAL SUMMARY
-- ========================================
SELECT '2. LOANS - FINANCIAL SUMMARY' AS '';
SELECT '---------------------------------------' AS '';

SELECT 
    COUNT(*) AS total_loans,
    COUNT(DISTINCT member_id) AS unique_members_with_loans,
    COALESCE(SUM(amount), 0) AS total_principal_amount,
    COALESCE(SUM(total_interest), 0) AS total_interest,
    COALESCE(SUM(total_repayable), 0) AS total_repayable,
    COALESCE(SUM(outstanding_balance), 0) AS total_outstanding_balance,
    COALESCE(SUM(interest_collected), 0) AS total_interest_collected,
    COALESCE(SUM(principal_repaid), 0) AS total_principal_repaid
FROM loans;

SELECT '' AS '';

-- ========================================
-- 3. LOANS BY STATUS
-- ========================================
SELECT '3. LOANS - BREAKDOWN BY STATUS' AS '';
SELECT '---------------------------------------' AS '';

SELECT 
    status,
    COUNT(*) AS count,
    COALESCE(SUM(amount), 0) AS total_amount,
    COALESCE(SUM(outstanding_balance), 0) AS total_outstanding
FROM loans
GROUP BY status
ORDER BY count DESC;

SELECT '' AS '';

-- ========================================
-- 4. REPAYMENTS SUMMARY
-- ========================================
SELECT '4. LOAN REPAYMENTS - SUMMARY' AS '';
SELECT '---------------------------------------' AS '';

SELECT 
    COUNT(*) AS total_repayments,
    COUNT(DISTINCT loan_id) AS loans_with_repayments,
    COALESCE(SUM(amount), 0) AS total_repayment_amount,
    COALESCE(SUM(principal_amount), 0) AS total_principal_paid,
    COALESCE(SUM(interest_amount), 0) AS total_interest_paid,
    MIN(repayment_date) AS earliest_repayment,
    MAX(repayment_date) AS latest_repayment
FROM loan_repayments;

SELECT '' AS '';

-- ========================================
-- 5. TRANSACTIONS SUMMARY
-- ========================================
SELECT '5. TRANSACTIONS - SUMMARY' AS '';
SELECT '---------------------------------------' AS '';

SELECT 
    COUNT(*) AS total_transactions,
    COALESCE(SUM(amount), 0) AS total_transaction_amount
FROM transactions;

SELECT '' AS '';

-- Transactions by type
SELECT '5b. TRANSACTIONS - BY TYPE' AS '';
SELECT '---------------------------------------' AS '';

SELECT 
    transaction_type,
    COUNT(*) AS count,
    COALESCE(SUM(amount), 0) AS total_amount
FROM transactions
GROUP BY transaction_type
ORDER BY count DESC;

SELECT '' AS '';

-- ========================================
-- 6. ACCOUNTS SUMMARY
-- ========================================
SELECT '6. ACCOUNTS - SUMMARY' AS '';
SELECT '---------------------------------------' AS '';

SELECT 
    COUNT(*) AS total_accounts,
    COUNT(DISTINCT member_id) AS unique_members,
    COALESCE(SUM(balance), 0) AS total_balance_all_accounts
FROM accounts;

SELECT '' AS '';

-- Accounts by type
SELECT '6b. ACCOUNTS - BY TYPE' AS '';
SELECT '---------------------------------------' AS '';

SELECT 
    account_type,
    COUNT(*) AS count,
    COALESCE(SUM(balance), 0) AS total_balance
FROM accounts
GROUP BY account_type;

SELECT '' AS '';

-- ========================================
-- 7. TOP-UP HISTORY
-- ========================================
SELECT '7. LOAN TOP-UP HISTORY' AS '';
SELECT '---------------------------------------' AS '';

SELECT 
    COUNT(*) AS total_topups,
    COUNT(DISTINCT loan_id) AS loans_with_topups,
    COALESCE(SUM(topup_amount), 0) AS total_topup_amount
FROM loan_topup_history;

SELECT '' AS '';

-- ========================================
-- 8. SAMPLE LOAN DETAILS (First 5 loans)
-- ========================================
SELECT '8. SAMPLE LOAN DETAILS (First 5 loans)' AS '';
SELECT '---------------------------------------' AS '';

SELECT 
    id,
    loan_number,
    member_id,
    amount AS principal,
    interest_rate,
    term_months,
    monthly_repayment,
    total_interest,
    outstanding_balance,
    interest_collected,
    principal_repaid,
    status,
    application_date,
    disbursement_date
FROM loans
ORDER BY id
LIMIT 5;

SELECT '' AS '';

-- ========================================
-- 9. DATA INTEGRITY CHECKS
-- ========================================
SELECT '9. DATA INTEGRITY CHECKS' AS '';
SELECT '---------------------------------------' AS '';

-- Check for loans with NULL financial values
SELECT '9a. Loans with NULL financial values:' AS '';
SELECT 
    COUNT(*) AS loans_with_null_values
FROM loans
WHERE amount IS NULL 
   OR interest_rate IS NULL 
   OR outstanding_balance IS NULL;

SELECT '' AS '';

-- Check for orphaned repayments
SELECT '9b. Orphaned repayments (no matching loan):' AS '';
SELECT 
    COUNT(*) AS orphaned_repayments
FROM loan_repayments lr
LEFT JOIN loans l ON lr.loan_id = l.id
WHERE l.id IS NULL;

SELECT '' AS '';

-- Check for orphaned transactions
SELECT '9c. Orphaned transactions (no matching account):' AS '';
SELECT 
    COUNT(*) AS orphaned_transactions
FROM transactions t
LEFT JOIN accounts a ON t.account_id = a.id
WHERE a.id IS NULL;

SELECT '' AS '';

-- ========================================
-- 10. GUARANTORS SUMMARY
-- ========================================
SELECT '10. GUARANTORS - SUMMARY' AS '';
SELECT '---------------------------------------' AS '';

SELECT 
    COUNT(*) AS total_guarantors,
    COUNT(DISTINCT loan_id) AS loans_with_guarantors,
    COUNT(DISTINCT guarantor_member_id) AS unique_guarantor_members,
    COALESCE(SUM(guaranteed_amount), 0) AS total_guaranteed_amount
FROM guarantors;

SELECT '' AS '';

-- ========================================
-- FINAL SUMMARY
-- ========================================
SELECT '================================================' AS '';
SELECT 'VERIFICATION COMPLETE' AS '';
SELECT '================================================' AS '';
SELECT '' AS '';
SELECT '⚠️  IMPORTANT: Save this output before proceeding!' AS '';
SELECT '   You will need it to verify data after import.' AS '';
SELECT '' AS '';
SELECT 'Next Step: Provide your Excel file with correct data' AS '';
SELECT '================================================' AS '';
