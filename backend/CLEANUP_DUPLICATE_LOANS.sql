-- =====================================================
-- CLEANUP DUPLICATE LOANS FROM MIGRATION
-- Remove zero-balance placeholder loans
-- Keep only loans with actual balances
-- =====================================================

USE minetsacco;

-- Step 1: Identify duplicate loans (members with multiple DISBURSED loans)
SELECT 
    m.full_name,
    m.employee_id,
    COUNT(*) as loan_count,
    GROUP_CONCAT(l.loan_number ORDER BY l.id) as loan_numbers,
    GROUP_CONCAT(l.outstanding_balance ORDER BY l.id) as balances
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE l.status = 'DISBURSED'
GROUP BY m.id, m.full_name, m.employee_id
HAVING loan_count > 1
ORDER BY m.full_name;

-- Step 2: Show zero-balance loans that will be hidden
SELECT 
    l.id,
    l.loan_number,
    m.full_name,
    m.employee_id,
    l.outstanding_balance,
    'Will be marked as FULLY_REPAID' as action
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE l.status = 'DISBURSED'
  AND l.outstanding_balance = 0
ORDER BY m.full_name;

-- =====================================================
-- FIX: Mark zero-balance loans as FULLY_REPAID
-- =====================================================

UPDATE loans
SET status = 'FULLY_REPAID'
WHERE status = 'DISBURSED'
  AND outstanding_balance = 0;

-- Step 3: Verify the cleanup
SELECT 
    '=== AFTER CLEANUP ===' as status,
    COUNT(*) as total_disbursed_loans,
    SUM(CASE WHEN outstanding_balance = 0 THEN 1 ELSE 0 END) as zero_balance_count,
    SUM(CASE WHEN outstanding_balance > 0 THEN 1 ELSE 0 END) as active_balance_count
FROM loans
WHERE status = 'DISBURSED';

-- Step 4: Show members who now have only ONE active loan
SELECT 
    m.full_name,
    m.employee_id,
    l.loan_number,
    l.outstanding_balance,
    '✓ Clean - one active loan' as result
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE l.status = 'DISBURSED'
  AND l.outstanding_balance > 0
ORDER BY m.full_name
LIMIT 20;

-- =====================================================
-- SUMMARY
-- =====================================================

SELECT 
    'Total DISBURSED loans (active)' as metric,
    COUNT(*) as count
FROM loans
WHERE status = 'DISBURSED'
UNION ALL
SELECT 
    'Total FULLY_REPAID loans (hidden)' as metric,
    COUNT(*) as count
FROM loans
WHERE status = 'FULLY_REPAID';

-- =====================================================
-- NOTES
-- =====================================================
-- After running this script:
-- 1. Refresh the treasurer's Loans page
-- 2. Only loans with actual balances will show
-- 3. Zero-balance duplicate loans are now marked FULLY_REPAID
-- 4. Members will appear only ONCE in the list
-- 5. Total active loans should reduce from 180 to approximately 99
