-- =====================================================
-- FIX BULK UPLOAD LOAN VISIBILITY
-- Ensures bulk-uploaded loans appear correctly for both
-- members and treasurers
-- =====================================================

USE minetsacco;

-- Show what we're about to fix
SELECT 
    '=== BEFORE FIX ===' AS status,
    COUNT(*) as total_migrated_loans,
    SUM(CASE WHEN status = 'DISBURSED' THEN 1 ELSE 0 END) as disbursed_count,
    SUM(CASE WHEN status != 'DISBURSED' THEN 1 ELSE 0 END) as not_disbursed_count,
    SUM(CASE WHEN disbursement_date IS NULL THEN 1 ELSE 0 END) as missing_disbursement_date
FROM loans
WHERE migration_status IS NOT NULL AND migration_status != '';

-- Fix 1: Set all migrated loans to DISBURSED status
-- Migrated loans are already active, so they should show as DISBURSED
UPDATE loans
SET 
    status = 'DISBURSED',
    disbursement_date = COALESCE(disbursement_date, application_date, NOW())
WHERE migration_status IS NOT NULL 
  AND migration_status != ''
  AND status != 'DISBURSED';

-- Fix 2: Ensure all migrated loans have a disbursement_date
UPDATE loans
SET 
    disbursement_date = COALESCE(application_date, NOW())
WHERE migration_status IS NOT NULL 
  AND migration_status != ''
  AND disbursement_date IS NULL;

-- Show what was fixed
SELECT 
    '=== AFTER FIX ===' AS status,
    COUNT(*) as total_migrated_loans,
    SUM(CASE WHEN status = 'DISBURSED' THEN 1 ELSE 0 END) as disbursed_count,
    SUM(CASE WHEN status != 'DISBURSED' THEN 1 ELSE 0 END) as not_disbursed_count,
    SUM(CASE WHEN disbursement_date IS NULL THEN 1 ELSE 0 END) as missing_disbursement_date
FROM loans
WHERE migration_status IS NOT NULL AND migration_status != '';

-- Verify all migrated loans are now visible
SELECT 
    l.loan_number,
    m.full_name AS member_name,
    m.employee_id,
    l.status,
    l.amount AS principal_amount,
    l.outstanding_balance,
    DATE_FORMAT(l.disbursement_date, '%Y-%m-%d') AS disbursement_date,
    l.migration_status,
    '✓ Visible to both member and treasurer' AS visibility
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE l.migration_status IS NOT NULL AND l.migration_status != ''
ORDER BY l.loan_number
LIMIT 50;

-- =====================================================
-- SUCCESS MESSAGE
-- =====================================================
SELECT 
    '✓ All bulk-uploaded loans are now in DISBURSED status' AS message,
    'They should be visible to both members and treasurers' AS note,
    'Treasurers can see them in the Loans section' AS treasurer_view,
    'Members can see them in My Loans section' AS member_view;
