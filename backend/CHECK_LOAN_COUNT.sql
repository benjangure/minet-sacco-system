-- =====================================================
-- CHECK TOTAL LOAN COUNT
-- Compare database count vs what treasurer sees
-- =====================================================

USE minetsacco;

-- Total count of all loans by status
SELECT 
    status,
    COUNT(*) as count,
    SUM(CASE WHEN migration_status = 'MIGRATED' THEN 1 ELSE 0 END) as migrated_count
FROM loans
GROUP BY status
ORDER BY status;

-- Total DISBURSED loans (what treasurer should see)
SELECT 
    COUNT(*) as total_disbursed_loans
FROM loans
WHERE status = 'DISBURSED';

-- DISBURSED loans breakdown
SELECT 
    'DISBURSED - Migrated' as category,
    COUNT(*) as count
FROM loans
WHERE status = 'DISBURSED'
  AND migration_status = 'MIGRATED'
UNION ALL
SELECT 
    'DISBURSED - Regular' as category,
    COUNT(*) as count
FROM loans
WHERE status = 'DISBURSED'
  AND (migration_status IS NULL OR migration_status = '');

-- Check if Tobias's loan exists
SELECT 
    'Tobias Loan Check' as info,
    l.id,
    l.loan_number,
    m.full_name,
    l.status,
    l.migration_status,
    'Should be counted in DISBURSED loans' as note
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE m.full_name LIKE '%Tobias%Mugendi%'
ORDER BY l.id DESC;

-- SUMMARY
SELECT 
    '============ SUMMARY ============' as summary,
    (SELECT COUNT(*) FROM loans WHERE status = 'DISBURSED') as total_disbursed,
    (SELECT COUNT(*) FROM loans WHERE status = 'DISBURSED' AND migration_status = 'MIGRATED') as migrated_disbursed,
    (SELECT COUNT(*) FROM loans WHERE status = 'DISBURSED' AND (migration_status IS NULL OR migration_status = '')) as regular_disbursed,
    'If treasurer sees 99 loans, one migrated loan is missing' as note;
