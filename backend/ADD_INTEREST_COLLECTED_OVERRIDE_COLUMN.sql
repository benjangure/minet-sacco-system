-- Manual SQL Script: Add interest_collected_manual_override column
-- Run this directly in your MySQL database

-- Add the column
ALTER TABLE loans 
ADD COLUMN interest_collected_manual_override BOOLEAN DEFAULT FALSE 
COMMENT 'TRUE when treasurer manually set interestCollected, FALSE for automatic calculation from repayments';

-- Set existing loans to FALSE (they use migration snapshots, not manual overrides)
UPDATE loans 
SET interest_collected_manual_override = FALSE 
WHERE interest_collected IS NOT NULL;

-- Verify the column was added
SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'loans' AND COLUMN_NAME = 'interest_collected_manual_override';
