-- =====================================================
-- FIX HANNAH NYAMBURA LOAN VISIBILITY ISSUE
-- Loan shows 0.00 balance instead of actual balance
-- =====================================================

USE minetsacco;

-- Step 1: Check Hannah's loans (employee_id 4033)
SELECT 
    l.id,
    l.loan_number,
    m.full_name,
    m.employee_id,
    l.status,
    l.amount AS principal,
    l.outstanding_balance,
    l.application_date,
    l.disbursement_date,
    l.migration_status,
    l.original_amount,
    l.original_principal
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE m.employee_id = '4033'
ORDER BY l.id;

-- Step 2: Check if there are duplicate loans for same members
-- (one with 0.00 balance, another with actual balance)
SELECT 
    m.employee_id,
    m.full_name,
    COUNT(*) as loan_count,
    GROUP_CONCAT(l.loan_number ORDER BY l.id) as loan_numbers,
    GROUP_CONCAT(l.outstanding_balance ORDER BY l.id) as balances
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE l.migration_status = 'MIGRATED'
GROUP BY m.employee_id, m.full_name
HAVING loan_count > 1
ORDER BY m.full_name;

-- Step 3: Find loans with 0.00 balance that should have actual balance
-- These are the "placeholder" loans that need to be hidden or updated
SELECT 
    l.id,
    l.loan_number,
    m.full_name,
    m.employee_id,
    l.outstanding_balance,
    l.amount,
    'This loan has 0 balance - may be duplicate' AS issue
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE l.migration_status = 'MIGRATED'
  AND l.outstanding_balance = 0.00
  AND l.status = 'DISBURSED'
ORDER BY l.loan_number;

-- =====================================================
-- SOLUTION OPTIONS
-- =====================================================

-- Option 1: Mark zero-balance migrated loans as FULLY_REPAID or CLOSED
-- This will hide them from treasurer's active loans view

UPDATE loans
SET 
    status = 'FULLY_REPAID'
WHERE migration_status = 'MIGRATED'
  AND outstanding_balance = 0.00
  AND status = 'DISBURSED';

-- Check result
SELECT 
    'After marking zero-balance loans as FULLY_REPAID' AS action,
    COUNT(*) as affected_loans
FROM loans
WHERE migration_status = 'MIGRATED'
  AND outstanding_balance = 0.00
  AND status = 'FULLY_REPAID';

-- Verify Hannah's loan is now marked correctly
SELECT 
    l.id,
    l.loan_number,
    m.full_name,
    m.employee_id,
    l.status,
    l.outstanding_balance,
    CASE 
        WHEN l.outstanding_balance > 0 THEN '✓ Will show to treasurer (has balance)'
        WHEN l.status = 'FULLY_REPAID' THEN '✓ Correctly marked as repaid'
        ELSE '? Check this loan'
    END AS visibility
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE m.employee_id = '4033'
ORDER BY l.id;

-- =====================================================
-- FINAL VERIFICATION
-- =====================================================

-- Show all migrated loans that SHOULD be visible to treasurer
-- (DISBURSED status AND outstanding_balance > 0)
SELECT 
    l.loan_number,
    m.full_name,
    m.employee_id,
    l.status,
    l.outstanding_balance,
    '✓ Visible to treasurer' AS visibility
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE l.migration_status = 'MIGRATED'
  AND l.status = 'DISBURSED'
  AND l.outstanding_balance > 0
ORDER BY m.full_name;
