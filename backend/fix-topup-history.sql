-- Fix missing top-up history for loan 367
-- Based on notification: top-up of 5000, new balance 14666.67

-- First, let's check the top-up request details
SELECT 
    id,
    loan_id,
    requested_amount,
    status,
    disbursement_date,
    reviewed_by_id,
    disbursed_by_id,
    purpose
FROM loan_topup_requests 
WHERE loan_id = 367 
ORDER BY id DESC;

-- Check current loan state
SELECT 
    id,
    loan_number,
    amount,
    outstanding_balance,
    total_topup_amount,
    topup_count,
    last_topup_date
FROM loans 
WHERE id = 367;

-- Insert the missing history record if it doesn't exist
-- Assuming: outstanding before = 9666.67, topup = 5000, outstanding after = 14666.67
INSERT INTO loan_topup_history 
    (loan_id, topup_amount, outstanding_before_topup, outstanding_after_topup, 
     principal_paid_before_topup, new_guarantors_added, topup_date, processed_by, notes)
SELECT 
    r.loan_id,
    r.requested_amount,
    14666.67 - r.requested_amount AS outstanding_before,  -- Calculate backwards
    14666.67 AS outstanding_after,
    (SELECT original_principal - (outstanding_balance - requested_amount) FROM loans WHERE id = 367) AS principal_paid,
    (SELECT COUNT(*) FROM topup_guarantors WHERE topup_request_id = r.id) AS guarantor_count,
    r.disbursement_date,
    r.disbursed_by_id,
    r.purpose
FROM loan_topup_requests r
WHERE r.loan_id = 367 
  AND r.status = 'DISBURSED'
  AND NOT EXISTS (
      SELECT 1 FROM loan_topup_history h 
      WHERE h.loan_id = 367 
        AND h.topup_amount = r.requested_amount
        AND h.topup_date = r.disbursement_date
  );

-- Verify the insert
SELECT * FROM loan_topup_history WHERE loan_id = 367;
