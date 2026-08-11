-- ============================================================================
-- DIRECT TEST LOAN CREATION - Complete Workflow to Treasurer
-- ============================================================================
-- This SQL script creates a test loan directly in the database
-- that goes through all steps to PENDING_TREASURER status
-- ============================================================================

-- Step 1: Find existing members to use as test users
SELECT 
    id, 
    member_number, 
    first_name, 
    last_name, 
    email,
    phone
FROM members 
LIMIT 5;

-- Note: Copy the IDs from above to use in the variables below
-- Replace these with actual member IDs from your database:
SET @loan_applicant_id = 1;
SET @guarantor1_id = 2;
SET @guarantor2_id = 3;

-- Step 2: Get a loan product ID
SELECT id, name, interest_rate, max_amount, min_amount
FROM loan_products
LIMIT 1;

-- Replace with actual loan product ID:
SET @loan_product_id = 1;

-- Step 3: Set loan parameters
SET @loan_amount = 50000.00;
SET @interest_rate = 10.00;
SET @term_months = 12;
SET @total_interest = @loan_amount * (@interest_rate / 100);
SET @total_repayable = @loan_amount + @total_interest;
SET @monthly_repayment = @total_repayable / @term_months;
SET @guarantor_pledge = @loan_amount / 2;

-- Step 4: Generate loan number
SET @loan_number = CONCAT('LN', YEAR(NOW()), LPAD(FLOOR(RAND() * 10000), 4, '0'));

-- Step 5: Create the loan application
INSERT INTO loans (
    member_id,
    loan_product_id,
    loan_number,
    amount,
    interest_rate,
    term_months,
    status,
    monthly_repayment,
    total_interest,
    total_repayable,
    original_principal,
    original_amount,
    outstanding_balance,
    purpose,
    application_date,
    created_at,
    updated_at
) VALUES (
    @loan_applicant_id,
    @loan_product_id,
    @loan_number,
    @loan_amount,
    @interest_rate,
    @term_months,
    'PENDING_GUARANTOR_APPROVAL',  -- Initial status
    @monthly_repayment,
    @total_interest,
    @total_repayable,
    @loan_amount,
    @loan_amount,
    @total_repayable,
    'Test loan for treasurer notification workflow',
    NOW(),
    NOW(),
    NOW()
);

-- Get the newly created loan ID
SET @new_loan_id = LAST_INSERT_ID();

SELECT CONCAT('✓ Created loan ID: ', @new_loan_id, ' with number: ', @loan_number) as status;

-- Step 6: Create guarantor records
INSERT INTO guarantors (
    loan_id,
    guarantor_member_id,
    pledged_amount,
    status,
    created_at
) VALUES 
(
    @new_loan_id,
    @guarantor1_id,
    @guarantor_pledge,
    'PENDING',
    NOW()
),
(
    @new_loan_id,
    @guarantor2_id,
    @guarantor_pledge,
    'PENDING',
    NOW()
);

SELECT '✓ Created 2 guarantor records' as status;

-- Step 7: Get guarantor IDs for approval
SET @guarantor_record_1 = (SELECT id FROM guarantors WHERE loan_id = @new_loan_id AND guarantor_member_id = @guarantor1_id);
SET @guarantor_record_2 = (SELECT id FROM guarantors WHERE loan_id = @new_loan_id AND guarantor_member_id = @guarantor2_id);

-- Step 8: Simulate guarantor approvals
UPDATE guarantors 
SET status = 'ACCEPTED',
    response_date = NOW(),
    comments = 'Test approval - automated workflow (Guarantor 1)'
WHERE id = @guarantor_record_1;

UPDATE guarantors 
SET status = 'ACCEPTED',
    response_date = NOW(),
    comments = 'Test approval - automated workflow (Guarantor 2)'
WHERE id = @guarantor_record_2;

SELECT '✓ All guarantors approved' as status;

-- Step 9: Update loan status to PENDING_TREASURER
-- This simulates what the backend does when all guarantors approve
UPDATE loans
SET status = 'PENDING_TREASURER',
    updated_at = NOW()
WHERE id = @new_loan_id;

SELECT '✓ Loan status updated to PENDING_TREASURER' as status;

-- Step 10: Create a notification for the treasurer (optional - backend usually does this)
-- Check if notifications table exists first
INSERT INTO notifications (
    user_id,
    member_id,
    title,
    message,
    type,
    reference_type,
    reference_id,
    is_read,
    created_at
)
SELECT 
    u.id,
    NULL,
    'New Loan Awaiting Approval',
    CONCAT('Loan ', @loan_number, ' for KES ', FORMAT(@loan_amount, 2), ' is pending treasurer approval.'),
    'LOAN_APPROVAL',
    'LOAN',
    @new_loan_id,
    FALSE,
    NOW()
FROM users u
WHERE u.role = 'TREASURER'
LIMIT 1;

SELECT '✓ Notification created for treasurer' as status;

-- ============================================================================
-- VERIFICATION QUERIES
-- ============================================================================

SELECT '============================================' as '';
SELECT '  FINAL LOAN STATUS' as '';
SELECT '============================================' as '';

SELECT 
    l.id as 'Loan ID',
    l.loan_number as 'Loan Number',
    l.status as 'Status',
    CONCAT(m.first_name, ' ', m.last_name) as 'Applicant',
    l.amount as 'Amount',
    l.term_months as 'Term (Months)',
    l.monthly_repayment as 'Monthly Repayment',
    l.application_date as 'Application Date'
FROM loans l
JOIN members m ON l.member_id = m.id
WHERE l.id = @new_loan_id;

SELECT '--------------------------------------------' as '';
SELECT '  GUARANTOR DETAILS' as '';
SELECT '--------------------------------------------' as '';

SELECT 
    g.id as 'Guarantor ID',
    CONCAT(m.first_name, ' ', m.last_name) as 'Guarantor Name',
    g.pledged_amount as 'Pledged Amount',
    g.status as 'Status',
    g.response_date as 'Response Date'
FROM guarantors g
JOIN members m ON g.guarantor_member_id = m.id
WHERE g.loan_id = @new_loan_id;

SELECT '--------------------------------------------' as '';
SELECT '  TREASURER NOTIFICATIONS' as '';
SELECT '--------------------------------------------' as '';

SELECT 
    n.id as 'Notification ID',
    u.username as 'Treasurer',
    n.title as 'Title',
    n.message as 'Message',
    n.is_read as 'Read',
    n.created_at as 'Created At'
FROM notifications n
JOIN users u ON n.user_id = u.id
WHERE n.reference_id = @new_loan_id
  AND n.reference_type = 'LOAN'
  AND u.role = 'TREASURER';

SELECT '============================================' as '';
SELECT '  ✓✓✓ SUCCESS! ✓✓✓' as '';
SELECT '  The loan is now PENDING_TREASURER status.' as '';
SELECT '  The treasurer should see this notification!' as '';
SELECT '============================================' as '';

-- ============================================================================
-- CLEANUP (Optional - run this to remove the test loan)
-- ============================================================================
/*
DELETE FROM notifications WHERE reference_id = @new_loan_id AND reference_type = 'LOAN';
DELETE FROM guarantors WHERE loan_id = @new_loan_id;
DELETE FROM loans WHERE id = @new_loan_id;
SELECT '✓ Test loan cleaned up' as status;
*/
