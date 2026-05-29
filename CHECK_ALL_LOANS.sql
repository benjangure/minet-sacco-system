-- Check ALL loans to see which one has the -40% issue
SELECT 
  id, 
  loan_number, 
  status, 
  amount,
  total_interest,
  total_repayable, 
  outstanding_balance,
  (total_repayable - outstanding_balance) as calculated_repaid,
  ROUND(((total_repayable - outstanding_balance) / total_repayable) * 100, 2) as repayment_percentage
FROM loans 
ORDER BY id;
