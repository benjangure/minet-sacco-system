-- =====================================================================================
-- LOAN DATA CLEANUP SCRIPTS FOR SACCO DATABASE
-- =====================================================================================
-- Purpose: Audit incomplete loan data and clean up bad imports
-- Date: 2026-06-29
-- Use in MySQL Workbench on the production SACCO database
-- =====================================================================================

-- =====================================================================================
-- PART 1: DIAGNOSTIC QUERIES (RUN FIRST TO SEE WHAT'S IN THE DATABASE)
-- =====================================================================================

-- 1.1: Count total loans in the system
SELECT 'TOTAL LOANS' as check_type, COUNT(*) as count FROM loans;

-- 1.2: Count loans with NULL amount (missing principal) - INCOMPLETE
SELECT 'LOANS WITH NULL PRINCIPAL (amount)' as check_type, COUNT(*) as count 
FROM loans WHERE amount IS NULL;

-- 1.3: Count loans with NULL term_months - INCOMPLETE
SELECT 'LOANS WITH NULL TERM_MONTHS' as check_type, COUNT(*) as count 
FROM loans WHERE term_months IS NULL;

-- 1.4: Count loans with NULL total_interest - INCOMPLETE
SELECT 'LOANS WITH NULL TOTAL_INTEREST' as check_type, COUNT(*) as count 
FROM loans WHERE total_interest IS NULL;

-- 1.5: List all incomplete loans (missing critical data)
-- These are the ones that need to be deleted
SELECT 
    l.id,
    l.loan_number,
    m.employee_id,
    m.first_name,
    m.last_name,
    l.amount,
    l.term_months,
    l.total_interest,
    l.status,
    l.outstanding_balance,
    l.migration_status,
    l.created_at
FROM loans l
LEFT JOIN members m ON l.member_id = m.id
WHERE l.amount IS NULL 
   OR l.term_months IS NULL 
   OR l.total_interest IS NULL
ORDER BY l.created_at DESC;

-- 1.6: Get loan statistics by migration status
SELECT 
    migration_status,
    COUNT(*) as total_loans,
    SUM(CASE WHEN amount IS NULL THEN 1 ELSE 0 END) as missing_amount,
    SUM(CASE WHEN term_months IS NULL THEN 1 ELSE 0 END) as missing_term,
    SUM(CASE WHEN total_interest IS NULL THEN 1 ELSE 0 END) as missing_interest
FROM loans
GROUP BY migration_status;

-- 1.7: Get IDs of all related records for incomplete loans (for reference)
-- This helps you see what else will be deleted
SELECT 
    'Guarantors' as related_table,
    COUNT(*) as count
FROM guarantors g
WHERE g.loan_id IN (
    SELECT l.id FROM loans l 
    WHERE l.amount IS NULL OR l.term_months IS NULL OR l.total_interest IS NULL
)
UNION ALL
SELECT 
    'Loan Repayments' as related_table,
    COUNT(*) as count
FROM loan_repayments lr
WHERE lr.loan_id IN (
    SELECT l.id FROM loans l 
    WHERE l.amount IS NULL OR l.term_months IS NULL OR l.total_interest IS NULL
)
UNION ALL
SELECT 
    'Loan Repayment Requests' as related_table,
    COUNT(*) as count
FROM loan_repayment_requests lrr
WHERE lrr.loan_id IN (
    SELECT l.id FROM loans l 
    WHERE l.amount IS NULL OR l.term_months IS NULL OR l.total_interest IS NULL
);

-- =====================================================================================
-- PART 2: CLEANUP SCRIPT (RUN AFTER REVIEWING DIAGNOSTICS)
-- =====================================================================================
-- WARNING: This will DELETE incomplete loans and all related data
-- Make sure you've reviewed the diagnostic queries above first!

-- 2.1: Create a backup view of incomplete loans (optional, for record keeping)
CREATE TEMPORARY TABLE incomplete_loans_backup AS
SELECT 
    l.id,
    l.loan_number,
    m.employee_id,
    m.first_name,
    m.last_name,
    l.amount,
    l.term_months,
    l.total_interest,
    l.status,
    l.migration_status,
    l.created_at
FROM loans l
LEFT JOIN members m ON l.member_id = m.id
WHERE l.amount IS NULL 
   OR l.term_months IS NULL 
   OR l.total_interest IS NULL;

-- 2.2: Get the list of incomplete loan IDs to delete
-- Store these IDs - you'll need them in the delete statements below
SELECT 
    GROUP_CONCAT(id) as incomplete_loan_ids
FROM loans
WHERE amount IS NULL 
   OR l.term_months IS NULL 
   OR l.total_interest IS NULL;

-- 2.3: Delete loan repayment requests for incomplete loans
DELETE FROM loan_repayment_requests
WHERE loan_id IN (
    SELECT id FROM loans 
    WHERE amount IS NULL 
       OR term_months IS NULL 
       OR total_interest IS NULL
);

-- 2.4: Delete loan repayments for incomplete loans
DELETE FROM loan_repayments
WHERE loan_id IN (
    SELECT id FROM loans 
    WHERE amount IS NULL 
       OR term_months IS NULL 
       OR total_interest IS NULL
);

-- 2.5: Delete guarantors for incomplete loans
DELETE FROM guarantors
WHERE loan_id IN (
    SELECT id FROM loans 
    WHERE amount IS NULL 
       OR term_months IS NULL 
       OR total_interest IS NULL
);

-- 2.6: Delete the incomplete loans themselves
DELETE FROM loans
WHERE amount IS NULL 
   OR term_months IS NULL 
   OR total_interest IS NULL;

-- 2.7: Verify cleanup was successful
SELECT 'VERIFICATION AFTER CLEANUP' as check;
SELECT 'Remaining total loans' as check_type, COUNT(*) as count FROM loans;
SELECT 'Loans still missing principal' as check_type, COUNT(*) as count 
FROM loans WHERE amount IS NULL;
SELECT 'Loans still missing term' as check_type, COUNT(*) as count 
FROM loans WHERE term_months IS NULL;
SELECT 'Loans still missing interest' as check_type, COUNT(*) as count 
FROM loans WHERE total_interest IS NULL;

-- =====================================================================================
-- PART 3: OPTIONAL - REMOVE FROM MIGRATION ITEMS AS WELL
-- =====================================================================================
-- If incomplete loans were imported via migration, also clean up the migration records

-- 3.1: Check migration items for incomplete data
SELECT 
    lmi.id,
    lmi.row_number,
    lmi.employee_id,
    lmi.loan_product_name,
    lmi.principal_amount,
    lmi.term_months,
    lmi.status,
    lmi.loan_id,
    lmi.created_at
FROM loan_migration_items lmi
WHERE lmi.principal_amount IS NULL 
   OR lmi.term_months IS NULL
ORDER BY lmi.created_at DESC;

-- 3.2: Delete incomplete migration items (optional)
DELETE FROM loan_migration_items
WHERE principal_amount IS NULL 
   OR term_months IS NULL;

-- =====================================================================================
-- PART 4: AUDIT TRAIL - LOG DELETIONS (IF YOUR SYSTEM HAS AUDIT LOGGING)
-- =====================================================================================
-- If you need to document what was deleted, you can query the audit logs
-- (adjust table name based on your audit logging setup)

SELECT 
    'Loan Deletions' as action,
    COUNT(*) as total,
    MAX(created_at) as last_deletion
FROM audit_logs
WHERE entity_type = 'LOAN' 
  AND action_type = 'DELETE'
  AND created_at >= DATE_SUB(NOW(), INTERVAL 7 DAY);

-- =====================================================================================
-- INSTRUCTIONS FOR USE
-- =====================================================================================
/*
STEP 1: Run all diagnostic queries (Section 1) first to understand the extent of incomplete data
        - Look at query 1.2 to see how many loans have NULL amount (missing principal)
        - Look at query 1.5 to see the actual incomplete loans

STEP 2: Review the results carefully - make sure you understand what will be deleted

STEP 3: When ready, run the cleanup script (Section 2) to delete incomplete loans
        - WARNING: This is permanent! Make sure you have a database backup first
        - The script deletes related data in order (repayment requests → repayments → guarantors → loans)

STEP 4: Run Part 4 verification queries to confirm cleanup was successful

STEP 5: If loans were imported via migration, also run Section 3 to clean migration items

BEST PRACTICES:
- Always make a database backup before running delete operations
- Run diagnostic queries first to confirm what will be deleted
- Delete in order: related records first, then parent records
- Verify results after cleanup
- Consider keeping a backup table of what was deleted (see query 2.1)
*/
