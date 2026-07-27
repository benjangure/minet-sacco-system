SELECT 
    l.id, 
    l.loan_number,
    l.member_id,
    l.amount, 
    l.outstanding_balance,
    l.migration_status,
    (SELECT COALESCE(SUM(principal_amount),0) FROM loan_repayments WHERE loan_id = l.id) AS live_repayment_sum,
    l.amount - (SELECT COALESCE(SUM(principal_amount),0) FROM loan_repayments WHERE loan_id = l.id) AS recalced_outstanding,
    CASE 
        WHEN l.outstanding_balance IS NULL THEN 'NULL_OUTSTANDING'
        WHEN l.outstanding_balance = l.amount - (SELECT COALESCE(SUM(principal_amount),0) FROM loan_repayments WHERE loan_id = l.id) 
        THEN 'OK' 
        ELSE 'CORRUPTED' 
    END AS status
FROM loans l
WHERE l.migration_status IS NOT NULL AND l.migration_status != ''
ORDER BY l.id;
