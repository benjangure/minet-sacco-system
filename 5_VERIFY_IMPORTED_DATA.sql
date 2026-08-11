-- ========================================
-- SCRIPT 5: VERIFY IMPORTED DATA
-- ========================================
-- This script verifies that the correct financial data
-- has been imported successfully from Excel
-- ========================================

-- Use the correct database
USE sacco_db;

-- Display header
SELECT '================================================' AS '';
SELECT 'MINET SACCO - IMPORTED DATA VERIFICATION' AS '';
SELECT 'Date/Time:' AS 'INFO', NOW() AS 'VALUE';
SELECT '================================================' AS '';
SELECT '' AS '';

-- ========================================
-- 1. RECORD COUNTS AFTER IMPORT
-- ========================================
SELECT '1. RECORD COUNTS AFTER IMPORT' AS '';
SELECT '---------------------------------------' AS '';

SELECT 'loans' AS table_name, COUNT(*) AS record_count FROM loans
UNION ALL
SELECT 'loan_repayments', COUNT(*) FROM loan_repayments
UNION ALL
SELECT 'transactions', COUNT(*) FROM transactions
UNION ALL
SELECT 'accounts', COUNT(*) FROM accounts
UNION ALL
SELECT 'loan_topup_history', COUNT(*) FROM loan_topup_history;

SELECT '' AS '';

-- ========================================
-- 2. LOAN FINANCIAL SUMMARY AFTER IMPORT
-- ========================================
SELECT '2. LOANS - FINANCIAL SUMMARY (AFTER IMPORT)' AS '';
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
-- 3. LOANS WITH NON-ZERO VALUES
-- ========================================
SELECT '3. LOANS WITH NON-ZERO VALUES' AS '';
SELECT '---------------------------------------' AS '';

SELECT 
    COUNT(*) AS loans_with_data
FROM loans
WHERE amount > 0 OR outstanding_balance > 0;

SELECT '' AS '';

-- ========================================
-- 4. LOANS BY STATUS AFTER IMPORT
-- ========================================
SELECT '4. LOANS - BREAKDOWN BY STATUS (AFTER IMPORT)' AS '';
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
-- 5. SAMPLE IMPORTED LOANS
-- ========================================
SELECT '5. SAMPLE IMPORTED LOANS (First 10 with data)' AS '';
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
    total_repayable,
    outstanding_balance,
    interest_collected,
    principal_repaid,
    status
FROM loans
WHERE amount > 0
ORDER BY id
LIMIT 10;

SELECT '' AS '';

-- ========================================
-- 6. REPAYMENTS SUMMARY AFTER IMPORT
-- ========================================
SELECT '6. LOAN REPAYMENTS - SUMMARY (AFTER IMPORT)' AS '';
SELECT '---------------------------------------' AS '';

SELECT 
    COUNT(*) AS total_repayments,
    COUNT(DISTINCT loan_id) AS loans_with_repayments,
    COALESCE(SUM(amount), 0) AS total_repayment_amount,
    COALESCE(SUM(principal_amount), 0) AS total_principal_paid,
    COALESCE(SUM(interest_amount), 0) AS total_interest_paid
FROM loan_repayments;

SELECT '' AS '';

-- ========================================
-- 7. DATA QUALITY CHECKS
-- ========================================
SELECT '7. DATA QUALITY CHECKS' AS '';
SELECT '---------------------------------------' AS '';

-- Check 7a: Loans with zero or NULL amounts
SELECT '7a. Loans with zero or NULL amounts:' AS '';
SELECT 
    COUNT(*) AS loans_with_zero_amount,
    GROUP_CONCAT(id) AS loan_ids
FROM loans
WHERE amount IS NULL OR amount = 0;

SELECT '' AS '';

-- Check 7b: Loans where outstanding > amount
SELECT '7b. Loans where outstanding > principal (potential issue):' AS '';
SELECT 
    COUNT(*) AS suspicious_loans
FROM loans
WHERE outstanding_balance > amount
  AND amount > 0;

SELECT '' AS '';

-- Check 7c: Loans with negative values
SELECT '7c. Loans with negative values (should be 0):' AS '';
SELECT 
    COUNT(*) AS loans_with_negatives
FROM loans
WHERE amount < 0 
   OR outstanding_balance < 0 
   OR interest_collected < 0 
   OR principal_repaid < 0;

SELECT '' AS '';

-- Check 7d: Interest rate validation
SELECT '7d. Interest rate range:' AS '';
SELECT 
    MIN(interest_rate) AS min_rate,
    MAX(interest_rate) AS max_rate,
    AVG(interest_rate) AS avg_rate
FROM loans
WHERE amount > 0;

SELECT '' AS '';

-- Check 7e: Repayments without matching loans
SELECT '7e. Orphaned repayments (no matching loan):' AS '';
SELECT 
    COUNT(*) AS orphaned_repayments
FROM loan_repayments lr
LEFT JOIN loans l ON lr.loan_id = l.id
WHERE l.id IS NULL;

SELECT '' AS '';

-- ========================================
-- 8. FINANCIAL CALCULATIONS VALIDATION
-- ========================================
SELECT '8. FINANCIAL CALCULATIONS VALIDATION' AS '';
SELECT '---------------------------------------' AS '';

-- Check if total_repayable = amount + total_interest
SELECT '8a. Loans where total_repayable ≠ principal + interest:' AS '';
SELECT 
    COUNT(*) AS calculation_mismatches,
    GROUP_CONCAT(id) AS loan_ids
FROM loans
WHERE ABS(total_repayable - (amount + total_interest)) > 0.01
  AND amount > 0;

SELECT '' AS '';

-- Check if principal_repaid + outstanding_balance = amount
SELECT '8b. Loans where principal_repaid + outstanding ≠ principal:' AS '';
SELECT 
    COUNT(*) AS balance_mismatches
FROM loans
WHERE ABS((principal_repaid + outstanding_balance) - amount) > 0.01
  AND amount > 0;

SELECT '' AS '';

-- ========================================
-- 9. COMPARISON WITH EXPECTED VALUES
-- ========================================
SELECT '9. COMPARISON SUMMARY' AS '';
SELECT '---------------------------------------' AS '';

-- You can manually compare these with your Excel file
SELECT 
    'Total Loans with Data' AS metric,
    COUNT(*) AS value
FROM loans
WHERE amount > 0

UNION ALL

SELECT 
    'Total Principal Amount',
    COALESCE(SUM(amount), 0)
FROM loans

UNION ALL

SELECT 
    'Total Outstanding Balance',
    COALESCE(SUM(outstanding_balance), 0)
FROM loans

UNION ALL

SELECT 
    'Total Interest to Collect',
    COALESCE(SUM(total_interest), 0)
FROM loans

UNION ALL

SELECT 
    'Total Repayments',
    COUNT(*)
FROM loan_repayments;

SELECT '' AS '';

-- ========================================
-- 10. DETAILED LOAN LISTING (For Manual Review)
-- ========================================
SELECT '10. COMPLETE LOAN LISTING (For Manual Review)' AS '';
SELECT '---------------------------------------' AS '';

SELECT 
    id,
    loan_number,
    member_id,
    amount AS principal,
    interest_rate AS rate_percent,
    term_months,
    monthly_repayment,
    total_interest,
    total_repayable,
    outstanding_balance,
    interest_collected,
    principal_repaid,
    (amount - principal_repaid) AS principal_remaining,
    status,
    disbursement_date
FROM loans
WHERE amount > 0
ORDER BY id;

SELECT '' AS '';

-- ========================================
-- 11. ACCOUNT BALANCES CHECK
-- ========================================
SELECT '11. ACCOUNT BALANCES CHECK' AS '';
SELECT '---------------------------------------' AS '';

SELECT 
    account_type,
    COUNT(*) AS account_count,
    SUM(balance) AS total_balance,
    AVG(balance) AS avg_balance
FROM accounts
GROUP BY account_type;

SELECT '' AS '';

-- ========================================
-- FINAL VERIFICATION STATUS
-- ========================================
SELECT '' AS '';
SELECT '================================================' AS '';
SELECT 'VERIFICATION COMPLETE' AS '';
SELECT '================================================' AS '';
SELECT '' AS '';

-- Automated pass/fail checks
SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM loans WHERE amount < 0) > 0 
        THEN '❌ FAIL: Found loans with negative amounts'
        ELSE '✅ PASS: No negative loan amounts'
    END AS check_negative_amounts;

SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM loan_repayments WHERE amount < 0) > 0 
        THEN '❌ FAIL: Found repayments with negative amounts'
        ELSE '✅ PASS: No negative repayment amounts'
    END AS check_negative_repayments;

SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM loans WHERE amount > 0) > 0 
        THEN '✅ PASS: Found loans with imported data'
        ELSE '❌ FAIL: No loans have data imported'
    END AS check_data_imported;

SELECT '' AS '';
SELECT '================================================' AS '';
SELECT 'MANUAL VERIFICATION REQUIRED:' AS '';
SELECT '  1. Compare total amounts with Excel file' AS '';
SELECT '  2. Verify sample loans match Excel records' AS '';
SELECT '  3. Check all member IDs are correct' AS '';
SELECT '  4. Confirm interest rates and terms' AS '';
SELECT '  5. Test the application with imported data' AS '';
SELECT '================================================' AS '';
SELECT '' AS '';

-- Export data for comparison (optional)
-- Uncomment these if you want to export to CSV for comparison

-- SELECT * FROM loans WHERE amount > 0
-- INTO OUTFILE '/tmp/imported_loans.csv'
-- FIELDS TERMINATED BY ',' ENCLOSED BY '"'
-- LINES TERMINATED BY '\n';
