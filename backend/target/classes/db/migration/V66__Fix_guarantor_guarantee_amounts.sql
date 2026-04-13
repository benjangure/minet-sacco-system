-- V66: Fix guarantor guarantee amounts
-- This migration fixes guarantors where guarantee_amount is NULL or 0
-- by setting it to their pledge_amount (which was correctly set during disbursement)

UPDATE guarantors
SET guarantee_amount = pledge_amount
WHERE (guarantee_amount IS NULL OR guarantee_amount = 0)
  AND pledge_amount > 0
  AND status = 'ACTIVE';

-- Log the migration completion
SELECT 'Guarantor guarantee amounts fixed successfully' as status;
