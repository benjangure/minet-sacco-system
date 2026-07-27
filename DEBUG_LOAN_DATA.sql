-- Check actual loan data to understand the issue
SELECT 
  id, 
  loan_number, 
  status, 
  amount,
  total_interest,
  total_repayable, 
  outstanding_balance,
  (total_repayable - outstanding_balance) as calculated_repaid
FROM loans 
WHERE status IN ('DISBURSED', 'REPAID')
ORDER BY id;

-- Also check if there are any NULL values
SELECT 
  id,
  loan_number,
  total_repayable IS NULL as total_repayable_null,
  outstanding_balance IS NULL as outstanding_balance_null
FROM loans
WHERE status IN ('DISBURSED', 'REPAID');
