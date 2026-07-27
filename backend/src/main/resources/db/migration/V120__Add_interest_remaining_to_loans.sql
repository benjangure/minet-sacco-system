-- Add or adjust interest_remaining column on loans table
ALTER TABLE loans ADD COLUMN IF NOT EXISTS interest_remaining DECIMAL(19,2) DEFAULT NULL;
ALTER TABLE loans MODIFY COLUMN interest_remaining DECIMAL(19,2) DEFAULT NULL;

-- Update existing loans: interest_remaining = totalInterest initially (until repayments start)
UPDATE loans SET interest_remaining = total_interest WHERE interest_remaining IS NULL;
