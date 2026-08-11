-- =====================================================
-- FIX LOAN STATUS ERROR
-- FULLY_REPAID is not a valid status in the backend
-- Use REPAID instead
-- =====================================================

USE minetsacco;

-- Check current status distribution
SELECT 
    status,
    COUNT(*) as count
FROM loans
GROUP BY status
ORDER BY status;

-- Fix: Change invalid status to REPAID (which is valid)
-- Based on frontend code, valid statuses are:
-- PENDING, PENDING_GUARANTOR_APPROVAL, PENDING_GUARANTOR_REPLACEMENT,
-- PENDING_GUARANTOR_REASSIGNMENT, PENDING_LOAN_OFFICER_REVIEW,
-- PENDING_CREDIT_COMMITTEE, PENDING_TREASURER, APPROVED, REJECTED,
-- DISBURSED, REPAID, DEFAULTED

UPDATE loans
SET status = 'REPAID'
WHERE status = ''  -- The truncated/invalid status
  AND outstanding_balance = 0;

-- Verify the fix
SELECT 
    '=== AFTER FIX ===' as status,
    status,
    COUNT(*) as count
FROM loans
GROUP BY status
ORDER BY status;

-- Check that we now have clean data
SELECT 
    'Total DISBURSED loans (active with balance)' as metric,
    COUNT(*) as count
FROM loans
WHERE status = 'DISBURSED'
UNION ALL
SELECT 
    'Total REPAID loans (zero balance)' as metric,
    COUNT(*) as count
FROM loans
WHERE status = 'REPAID';

-- =====================================================
-- RESTART BACKEND REQUIRED
-- =====================================================
-- After running this script:
-- 1. RESTART the backend service
-- 2. Clear browser cache
-- 3. Refresh the treasurer's dashboard
-- 4. Loans should now load without errors
