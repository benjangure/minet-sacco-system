-- Fix outstanding balance for all loans
-- Outstanding balance should equal total_repayable for DISBURSED and REPAID loans
-- This ensures accurate repayment tracking

UPDATE loans 
SET outstanding_balance = total_repayable 
WHERE (status = 'DISBURSED' OR status = 'REPAID') 
AND outstanding_balance != total_repayable;
