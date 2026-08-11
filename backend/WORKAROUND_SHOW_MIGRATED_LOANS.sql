-- =====================================================
-- WORKAROUND: Make Migrated Loans Appear in Treasurer View
-- The backend is filtering out loans with migration_status='MIGRATED'
-- Temporary fix: Clear migration_status so they appear as regular loans
-- =====================================================

USE minetsacco;

-- BEFORE: Check current state
SELECT 
    '==== BEFORE WORKAROUND ====' as status,
    migration_status,
    COUNT(*) as count
FROM loans
WHERE status = 'DISBURSED'
GROUP BY migration_status;

-- =====================================================
-- OPTION 1: Clear migration_status (makes them regular loans)
-- =====================================================
-- WARNING: This removes the "migrated" flag, so you won't know
-- which loans came from bulk upload

-- Uncomment to apply:
/*
UPDATE loans
SET migration_status = NULL
WHERE migration_status = 'MIGRATED'
  AND status = 'DISBURSED';
*/

-- =====================================================
-- OPTION 2: Change migration_status to empty string
-- =====================================================
-- This keeps the column but makes it appear as regular loan

UPDATE loans
SET migration_status = ''
WHERE migration_status = 'MIGRATED'
  AND status = 'DISBURSED';

-- AFTER: Check new state
SELECT 
    '==== AFTER WORKAROUND ====' as status,
    CASE 
        WHEN migration_status IS NULL THEN 'NULL (regular)'
        WHEN migration_status = '' THEN 'EMPTY (regular)'
        ELSE migration_status
    END as migration_status,
    COUNT(*) as count
FROM loans
WHERE status = 'DISBURSED'
GROUP BY migration_status;

-- Verify Tobias's loan
SELECT 
    'Tobias Loan After Fix' as info,
    l.loan_number,
    m.full_name,
    l.status,
    l.migration_status,
    '✓ Should now appear in treasurer view' as result
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE m.full_name LIKE '%Tobias%Mugendi%';

-- =====================================================
-- NOTES
-- =====================================================
-- This is a WORKAROUND because the backend Java code is filtering
-- out loans where migration_status = 'MIGRATED'
--
-- After this fix:
-- 1. Refresh treasurer's dashboard
-- 2. All 159 loans should now appear
-- 3. The "Active Loans" count should change from 99 to 159
--
-- PROPER FIX (requires Java code change):
-- Update the backend LoanRepository or LoanService to include
-- migrated loans in the query results
