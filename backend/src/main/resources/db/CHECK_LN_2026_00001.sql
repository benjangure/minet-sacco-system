-- Check what happened to LN-2026-00001
SELECT 
    id,
    loan_number,
    member_id,
    amount,
    status,
    outstanding_balance,
    application_date,
    approval_date,
    disbursement_date,
    created_at,
    updated_at
FROM loans
WHERE id = 1 OR loan_number = 'LN-2026-00001' OR loan_number IS NULL
ORDER BY id;

-- Check all loans with NULL loan_number
SELECT 
    id,
    loan_number,
    member_id,
    amount,
    status,
    outstanding_balance
FROM loans
WHERE loan_number IS NULL
ORDER BY id;

-- Check the loan that should be LN-2026-00001
SELECT 
    id,
    loan_number,
    member_id,
    amount,
    status,
    outstanding_balance
FROM loans
WHERE status = 'REPAID'
ORDER BY id;
