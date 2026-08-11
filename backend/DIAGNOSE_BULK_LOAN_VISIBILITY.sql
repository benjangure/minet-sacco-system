-- =====================================================
-- DIAGNOSE BULK UPLOAD LOAN VISIBILITY ISSUE
-- Member can see loan, but Treasurer cannot
-- =====================================================

USE minetsacco;

-- Step 1: Check loans from bulk upload (migrated loans)
SELECT 
    l.id AS loan_id,
    l.loan_number,
    l.member_id,
    m.full_name AS member_name,
    m.employee_id,
    l.status AS loan_status,
    l.amount AS principal_amount,
    l.outstanding_balance,
    l.application_date,
    l.disbursement_date,
    l.migration_status,
    l.created_by
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE l.migration_status IS NOT NULL AND l.migration_status != ''
ORDER BY l.application_date DESC
LIMIT 20;

-- Step 2: Check if there are any loans with NULL or unusual status
SELECT 
    status,
    COUNT(*) as count
FROM loans
WHERE l.migration_status IS NOT NULL AND l.migration_status != ''
GROUP BY status;

-- Step 3: Check ALL loan statuses (to see what's normal)
SELECT 
    status,
    COUNT(*) as count,
    SUM(CASE WHEN migration_status IS NOT NULL AND migration_status != '' THEN 1 ELSE 0 END) as migrated_count
FROM loans
GROUP BY status;

-- Step 4: Check guarantors for migrated loans
SELECT 
    l.loan_number,
    l.status AS loan_status,
    g.id AS guarantor_id,
    g.member_id AS guarantor_member_id,
    m.full_name AS guarantor_name,
    g.guarantee_amount,
    g.approval_status,
    g.is_next_of_kin
FROM loans l
LEFT JOIN guarantors g ON l.id = g.loan_id
LEFT JOIN members m ON g.member_id = m.id
WHERE l.migration_status IS NOT NULL AND l.migration_status != ''
ORDER BY l.loan_number, g.id
LIMIT 50;

-- =====================================================
-- VERIFICATION: Check all migrated loans
-- =====================================================

SELECT 
    l.id,
    l.loan_number,
    m.full_name AS member_name,
    m.employee_id,
    l.status,
    l.amount AS principal_amount,
    l.outstanding_balance,
    l.disbursement_date,
    l.migration_status,
    CASE 
        WHEN l.status = 'DISBURSED' THEN '✓ Should be visible to treasurer'
        WHEN l.status = 'APPROVED' THEN '? May need to be DISBURSED'
        WHEN l.status = 'PENDING' THEN '? May need to be DISBURSED'
        ELSE '✗ Check this status'
    END AS visibility_status
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE l.migration_status IS NOT NULL AND l.migration_status != ''
ORDER BY l.application_date DESC
LIMIT 30;

-- =====================================================
-- NOTES
-- =====================================================
-- Migrated loans should have:
-- 1. status = 'DISBURSED' (not PENDING or APPROVED)
-- 2. disbursement_date should have a date
-- 3. migration_status should be set
-- 
-- If treasurer's dashboard filters by status, it may only show:
-- - PENDING (awaiting approval)
-- - APPROVED (awaiting disbursement)
-- 
-- But NOT show DISBURSED loans (which are already active/ongoing)
-- Member's dashboard shows DISBURSED loans (their active loans)
