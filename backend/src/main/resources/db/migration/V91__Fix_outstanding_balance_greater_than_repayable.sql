-- Fix loans where outstanding_balance is greater than total_repayable
-- This can happen if the loan was created with incorrect calculations
-- Outstanding balance should NEVER exceed total repayable

UPDATE loans 
SET outstanding_balance = total_repayable 
WHERE outstanding_balance > total_repayable;

-- Verify the fix
-- SELECT id, loan_number, status, total_repayable, outstanding_balance FROM loans WHERE outstanding_balance > total_repayable;
