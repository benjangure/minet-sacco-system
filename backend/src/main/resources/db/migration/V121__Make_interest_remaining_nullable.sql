-- Make interest_remaining nullable for loans table
ALTER TABLE loans MODIFY COLUMN interest_remaining DECIMAL(19,2) NULL;

-- Ensure any current loans with no interest_remaining are left as NULL
UPDATE loans SET interest_remaining = NULL WHERE interest_remaining = 0 AND total_interest IS NULL;
