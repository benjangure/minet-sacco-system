-- Fix duplicate loan numbers by clearing loan numbers for loans that are not DISBURSED
-- IMPORTANT: Do NOT clear loan numbers for REPAID loans - they must keep their numbers permanently
-- This ensures loan numbers are permanent and never lost, even when fully repaid

UPDATE loans 
SET loan_number = NULL 
WHERE status NOT IN ('DISBURSED', 'REPAID') 
AND loan_number IS NOT NULL;
