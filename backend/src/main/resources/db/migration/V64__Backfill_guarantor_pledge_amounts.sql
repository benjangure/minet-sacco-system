-- Migration: Backfill guarantor pledge amounts for loans already disbursed
-- Purpose: Fix guarantor pledges that were not set before the fix was implemented
-- This ensures existing guarantors have their pledges frozen correctly

-- Update all ACTIVE guarantors to have pledge_amount equal to their loan amount
UPDATE guarantors g
JOIN loans l ON g.loan_id = l.id
SET g.pledge_amount = l.amount
WHERE g.status = 'ACTIVE' 
  AND g.pledge_amount IS NULL
  AND l.status IN ('DISBURSED', 'REPAID', 'DEFAULTED');

-- Update all RELEASED guarantors (loans fully repaid) to have pledge_amount = 0
UPDATE guarantors g
JOIN loans l ON g.loan_id = l.id
SET g.pledge_amount = 0
WHERE g.status = 'RELEASED' 
  AND g.pledge_amount IS NULL;

-- Log the migration
-- This helps verify the migration worked
SELECT 
  'Guarantor Pledge Backfill Complete' as migration_status,
  COUNT(*) as total_guarantors_updated,
  SUM(CASE WHEN status = 'ACTIVE' THEN 1 ELSE 0 END) as active_guarantors,
  SUM(CASE WHEN status = 'RELEASED' THEN 1 ELSE 0 END) as released_guarantors
FROM guarantors
WHERE pledge_amount IS NOT NULL;
