-- =====================================================
-- FIX TOBIAS MUGENDI LOAN VISIBILITY
-- Member sees loan but treasurer doesn't
-- Loan count not updating
-- =====================================================

USE minetsacco;

-- Step 1: Find Tobias Mugendi's member record
SELECT 
    id,
    member_number,
    full_name,
    employee_id,
    status,
    created_at
FROM members
WHERE full_name LIKE '%Tobias%Mugendi%'
   OR full_name LIKE '%Mugendi%Tobias%';

-- Step 2: Find ALL loans for Tobias (using member id from above)
-- Replace XXX with the member_id from Step 1
SELECT 
    l.id,
    l.loan_number,
    l.status,
    l.amount,
    l.outstanding_balance,
    l.application_date,
    l.approval_date,
    l.disbursement_date,
    l.migration_status,
    l.created_by,
    l.approved_by,
    l.disbursed_by,
    'Check if this loan is visible' AS note
FROM loans l
WHERE l.member_id IN (
    SELECT id FROM members 
    WHERE full_name LIKE '%Tobias%Mugendi%'
       OR full_name LIKE '%Mugendi%Tobias%'
)
ORDER BY l.id DESC;

-- Step 3: Check the specific details that might hide the loan from treasurer
SELECT 
    l.id,
    l.loan_number,
    m.full_name,
    m.employee_id,
    l.status,
    l.amount,
    l.outstanding_balance,
    l.migration_status,
    l.disbursement_date,
    CASE 
        WHEN l.status IS NULL THEN '✗ Status is NULL'
        WHEN l.status = 'PENDING' THEN '✗ Still PENDING - needs to be DISBURSED'
        WHEN l.status = 'APPROVED' THEN '✗ Still APPROVED - needs to be DISBURSED'
        WHEN l.status = 'PENDING_DISBURSEMENT' THEN '✗ Still PENDING_DISBURSEMENT - needs to be DISBURSED'
        WHEN l.status = 'DISBURSED' AND l.disbursement_date IS NULL THEN '✗ DISBURSED but no disbursement_date'
        WHEN l.status = 'DISBURSED' AND l.outstanding_balance = 0 THEN '⚠️ DISBURSED but balance is 0'
        WHEN l.status = 'DISBURSED' AND l.outstanding_balance > 0 THEN '✓ Should be visible'
        ELSE '? Unknown issue'
    END AS issue
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE m.full_name LIKE '%Tobias%Mugendi%'
   OR m.full_name LIKE '%Mugendi%Tobias%'
ORDER BY l.id DESC;

-- =====================================================
-- FIX THE LOAN
-- =====================================================

-- Fix 1: Set status to DISBURSED for Tobias's migrated loan
UPDATE loans l
JOIN members m ON l.member_id = m.id
SET 
    l.status = 'DISBURSED',
    l.disbursement_date = COALESCE(l.disbursement_date, l.approval_date, l.application_date, NOW())
WHERE (m.full_name LIKE '%Tobias%Mugendi%' OR m.full_name LIKE '%Mugendi%Tobias%')
  AND l.migration_status IS NOT NULL
  AND l.migration_status != ''
  AND l.status != 'DISBURSED';

-- Fix 2: Ensure disbursement_date is set
UPDATE loans l
JOIN members m ON l.member_id = m.id
SET 
    l.disbursement_date = COALESCE(l.application_date, NOW())
WHERE (m.full_name LIKE '%Tobias%Mugendi%' OR m.full_name LIKE '%Mugendi%Tobias%')
  AND l.migration_status IS NOT NULL
  AND l.migration_status != ''
  AND l.disbursement_date IS NULL;

-- =====================================================
-- VERIFY THE FIX
-- =====================================================

-- Check Tobias's loan is now correctly configured
SELECT 
    l.id,
    l.loan_number,
    m.full_name,
    m.employee_id,
    l.status,
    l.amount,
    l.outstanding_balance,
    l.disbursement_date,
    l.migration_status,
    '✓ Should now be visible to treasurer' AS result
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE m.full_name LIKE '%Tobias%Mugendi%'
   OR m.full_name LIKE '%Mugendi%Tobias%'
ORDER BY l.id DESC;

-- =====================================================
-- COUNT ALL DISBURSED LOANS
-- =====================================================

-- This should now include Tobias's loan
SELECT 
    COUNT(*) as total_disbursed_loans,
    SUM(CASE WHEN migration_status = 'MIGRATED' THEN 1 ELSE 0 END) as migrated_loans,
    SUM(CASE WHEN migration_status IS NULL OR migration_status = '' THEN 1 ELSE 0 END) as regular_loans
FROM loans
WHERE status = 'DISBURSED';

-- =====================================================
-- NOTES
-- =====================================================
-- If Tobias's loan still doesn't show:
-- 1. Check if treasurer's page filters by loan_product_id (maybe product doesn't exist)
-- 2. Check if there's a date range filter (loan might be outside date range)
-- 3. Clear browser cache and refresh
-- 4. Check backend logs for errors when loading loans list
