-- Script to insert missing top-up history records for loan 367
-- This fixes the disbursed_by_id error by using the correct column name

-- Check current loan state for loan 367
SELECT 
    id,
    loan_number,
    amount,
    outstanding_balance,
    original_principal,
    total_topup_amount,
    topup_count
FROM loans 
WHERE id = 367;

-- Check disbursed top-up requests for this loan
SELECT 
    id,
    requested_amount,
    status,
    disbursement_date,
    disbursed_by,
    purpose
FROM loan_topup_requests 
WHERE loan_id = 367 
  AND status = 'DISBURSED';

-- Insert top-up history for disbursed requests (using correct column name)
INSERT INTO loan_topup_history (
    loan_id,
    topup_amount,
    outstanding_before_topup,
    outstanding_after_topup,
    principal_paid_before_topup,
    new_guarantors_added,
    topup_date,
    processed_by,
    notes
)
SELECT 
    r.loan_id,
    r.requested_amount AS topup_amount,
    -- Calculate based on current outstanding (14666.67) minus the top-up
    (SELECT outstanding_balance FROM loans WHERE id = 367) - r.requested_amount AS outstanding_before_topup,
    (SELECT outstanding_balance FROM loans WHERE id = 367) AS outstanding_after_topup,
    -- Principal paid before = original principal - outstanding before topup
    (SELECT original_principal FROM loans WHERE id = 367) - 
        ((SELECT outstanding_balance FROM loans WHERE id = 367) - r.requested_amount) AS principal_paid_before_topup,
    (SELECT COUNT(*) FROM topup_guarantors WHERE topup_request_id = r.id) AS new_guarantors_added,
    COALESCE(r.disbursement_date, NOW()) AS topup_date,
    r.disbursed_by AS processed_by,  -- Fixed: changed from disbursed_by_id to disbursed_by
    r.purpose AS notes
FROM loan_topup_requests r
WHERE r.loan_id = 367
  AND r.status = 'DISBURSED'
  AND NOT EXISTS (
      SELECT 1 
      FROM loan_topup_history h 
      WHERE h.loan_id = 367
  );

-- Verify the insertion
SELECT 
    id,
    loan_id,
    topup_amount,
    outstanding_before_topup,
    outstanding_after_topup,
    principal_paid_before_topup,
    topup_date,
    processed_by
FROM loan_topup_history
WHERE loan_id = 367
ORDER BY topup_date DESC;
