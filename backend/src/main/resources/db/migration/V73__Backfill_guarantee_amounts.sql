-- Migration: Backfill guarantee_amount for existing guarantor records
-- Purpose: Set guarantee_amount to loan amount for all existing guarantors
-- This ensures the partial guarantee feature works correctly

-- For all guarantors where guarantee_amount is NULL, set it to the loan amount
UPDATE guarantors g
JOIN loans l ON g.loan_id = l.id
SET g.guarantee_amount = l.amount
WHERE g.guarantee_amount IS NULL;

-- Verify the backfill
SELECT 
  'Guarantee Amount Backfill Complete' as migration_status,
  COUNT(*) as total_guarantors,
  SUM(CASE WHEN guarantee_amount IS NOT NULL THEN 1 ELSE 0 END) as guarantors_with_amount,
  SUM(CASE WHEN guarantee_amount IS NULL THEN 1 ELSE 0 END) as guarantors_without_amount
FROM guarantors;
