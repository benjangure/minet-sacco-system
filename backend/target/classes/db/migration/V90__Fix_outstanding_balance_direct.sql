-- Direct fix for outstanding balance - ensures it equals total_repayable
-- This is a follow-up to V89 to ensure the fix is applied

UPDATE loans 
SET outstanding_balance = total_repayable 
WHERE (status = 'DISBURSED' OR status = 'REPAID') 
AND outstanding_balance != total_repayable;

-- Verify the fix was applied
-- SELECT id, loan_number, status, total_repayable, outstanding_balance FROM loans WHERE status IN ('DISBURSED', 'REPAID');
