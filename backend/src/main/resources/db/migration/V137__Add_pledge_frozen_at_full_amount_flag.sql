-- Add flag to distinguish between manually-set pledges (do not reduce on repayment)
-- and auto-calculated pledges (reduce proportionally on repayment)
ALTER TABLE guarantors ADD COLUMN pledge_frozen_at_full_amount BOOLEAN DEFAULT false;

-- Set existing pledges to false (they were auto-calculated and can be reduced)
UPDATE guarantors SET pledge_frozen_at_full_amount = false WHERE pledge_frozen_at_full_amount IS NULL;
