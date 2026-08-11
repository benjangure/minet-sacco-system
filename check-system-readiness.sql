-- ============================================================================
-- SYSTEM READINESS CHECK
-- ============================================================================
-- This script checks if your database has the necessary data
-- to create a test loan for treasurer notifications
-- ============================================================================

SELECT '============================================' as '';
SELECT '  SYSTEM READINESS CHECK' as '';
SELECT '============================================' as '';
SELECT '' as '';

-- Check 1: Members
SELECT '1. CHECKING MEMBERS...' as '';
SELECT 
    COUNT(*) as total_members,
    CASE 
        WHEN COUNT(*) >= 3 THEN '✓ PASS - Sufficient members'
        ELSE '✗ FAIL - Need at least 3 members'
    END as status
FROM members;

SELECT '' as '';
SELECT 'Available Members:' as '';
SELECT 
    id,
    member_number,
    CONCAT(first_name, ' ', last_name) as full_name,
    email,
    phone,
    status
FROM members
ORDER BY id
LIMIT 10;

SELECT '' as '';
SELECT '--------------------------------------------' as '';

-- Check 2: Loan Products
SELECT '2. CHECKING LOAN PRODUCTS...' as '';
SELECT 
    COUNT(*) as total_products,
    CASE 
        WHEN COUNT(*) >= 1 THEN '✓ PASS - Loan products available'
        ELSE '✗ FAIL - Need at least 1 loan product'
    END as status
FROM loan_products;

SELECT '' as '';
SELECT 'Available Loan Products:' as '';
SELECT 
    id,
    name,
    interest_rate,
    min_amount,
    max_amount,
    min_term_months,
    max_term_months,
    status
FROM loan_products
ORDER BY id
LIMIT 5;

SELECT '' as '';
SELECT '--------------------------------------------' as '';

-- Check 3: Users with TREASURER role
SELECT '3. CHECKING TREASURER USERS...' as '';
SELECT 
    COUNT(*) as total_treasurers,
    CASE 
        WHEN COUNT(*) >= 1 THEN '✓ PASS - Treasurer user exists'
        ELSE '⚠ WARNING - No treasurer user found (notifications may not work)'
    END as status
FROM users
WHERE role = 'TREASURER';

SELECT '' as '';
SELECT 'Treasurer Users:' as '';
SELECT 
    id,
    username,
    email,
    role,
    is_active
FROM users
WHERE role = 'TREASURER'
ORDER BY id
LIMIT 5;

SELECT '' as '';
SELECT '--------------------------------------------' as '';

-- Check 4: Member Account Balances
SELECT '4. CHECKING MEMBER ACCOUNT BALANCES...' as '';
SELECT 'Member Savings Accounts (needed for loan eligibility):' as '';
SELECT 
    m.id as member_id,
    m.member_number,
    CONCAT(m.first_name, ' ', m.last_name) as member_name,
    a.account_type,
    a.balance,
    CASE 
        WHEN a.balance >= 10000 THEN '✓ Eligible'
        ELSE '⚠ Low balance'
    END as eligibility_status
FROM members m
LEFT JOIN accounts a ON m.id = a.member_id AND a.account_type = 'SAVINGS'
ORDER BY a.balance DESC
LIMIT 10;

SELECT '' as '';
SELECT '--------------------------------------------' as '';

-- Check 5: Recent Loans
SELECT '5. CHECKING RECENT LOANS...' as '';
SELECT 
    COUNT(*) as total_loans,
    COUNT(CASE WHEN status = 'PENDING_TREASURER' THEN 1 END) as pending_treasurer,
    COUNT(CASE WHEN status = 'PENDING_GUARANTOR_APPROVAL' THEN 1 END) as pending_guarantors,
    COUNT(CASE WHEN status = 'DISBURSED' THEN 1 END) as disbursed
FROM loans;

SELECT '' as '';
SELECT 'Recent Loans:' as '';
SELECT 
    l.id,
    l.loan_number,
    CONCAT(m.first_name, ' ', m.last_name) as applicant,
    l.amount,
    l.status,
    l.application_date
FROM loans l
JOIN members m ON l.member_id = m.id
ORDER BY l.application_date DESC
LIMIT 10;

SELECT '' as '';
SELECT '--------------------------------------------' as '';

-- Check 6: Notifications Table
SELECT '6. CHECKING NOTIFICATIONS TABLE...' as '';
SELECT 
    CASE 
        WHEN COUNT(*) > 0 THEN '✓ PASS - Notifications table exists'
        ELSE '✗ FAIL - Notifications table may not exist'
    END as status
FROM information_schema.tables 
WHERE table_schema = DATABASE() 
  AND table_name = 'notifications';

SELECT '' as '';
SELECT 'Recent Loan Notifications:' as '';
SELECT 
    n.id,
    u.username as recipient,
    n.title,
    n.type,
    n.is_read,
    n.created_at
FROM notifications n
LEFT JOIN users u ON n.user_id = u.id
WHERE n.reference_type = 'LOAN'
ORDER BY n.created_at DESC
LIMIT 5;

SELECT '' as '';
SELECT '============================================' as '';
SELECT '  READINESS SUMMARY' as '';
SELECT '============================================' as '';

-- Final Summary
SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM members) >= 3 
         AND (SELECT COUNT(*) FROM loan_products) >= 1
         AND (SELECT COUNT(*) FROM users WHERE role = 'TREASURER') >= 1
        THEN '✓✓✓ SYSTEM READY ✓✓✓'
        ELSE '⚠ SYSTEM NOT READY - Check failed items above'
    END as final_status;

SELECT '' as '';
SELECT 'Next Steps:' as '';
SELECT '1. Run create-test-loan-for-treasurer.ps1 (API method)' as step;
SELECT '   OR' as '';
SELECT '2. Run create-test-loan-direct.sql (SQL method)' as step;
SELECT '' as '';
SELECT 'See TEST_LOAN_CREATION_GUIDE.md for detailed instructions' as '';
SELECT '============================================' as '';
