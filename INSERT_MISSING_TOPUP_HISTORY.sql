-- Manual fix: Insert missing top-up history for loan 367
-- This creates the history record for the top-up that was already approved

-- Insert the history record based on the top-up request that was disbursed
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
    14666.67 - r.requested_amount AS outstanding_before_topup,
    14666.67 AS outstanding_after_topup,
    641750 - (14666.67 - r.requested_amount) AS principal_paid_before_topup,
    (SELECT COUNT(*) FROM topup_guarantors WHERE topup_request_id = r.id) AS new_guarantors_added,
    COALESCE(r.disbursement_date, NOW()) AS topup_date,
    r.disbursed_by_id AS processed_by,
    r.purpose AS notes
FROM loan_topup_requests r
WHERE r.loan_id = 367
  AND r.status = 'DISBURSED'
  AND NOT EXISTS (
      SELECT 1 
      FROM loan_topup_history h 
      WHERE h.loan_id = 367
  );

-- Verify the insert
SELECT 
    l.loan_number,
    h.topup_amount,
    h.outstanding_before_topup,
    h.outstanding_after_topup,
    h.topup_date,
    u.username AS processed_by
FROM loan_topup_history h
JOIN loans l ON h.loan_id = l.id
LEFT JOIN users u ON h.processed_by = u.id
WHERE h.loan_id = 367;
