-- Final SQL migration script - only add columns that don't exist yet
-- Run these one by one and skip any that give "Duplicate column" errors

-- 1. Add principal_repaid_manual_override (after the principal_repaid column that now exists)
ALTER TABLE loans 
ADD COLUMN principal_repaid_manual_override BOOLEAN DEFAULT FALSE 
AFTER principal_repaid;

-- 2. Add interest_remaining column
ALTER TABLE loans 
ADD COLUMN interest_remaining DECIMAL(15,2) DEFAULT 0.00 
AFTER interest_rate;

-- 3. Verify all required columns now exist
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'loans' 
AND COLUMN_NAME IN (
    'interest_collected', 
    'interest_collected_manual_override', 
    'principal_repaid', 
    'principal_repaid_manual_override', 
    'interest_remaining'
)
ORDER BY COLUMN_NAME;

-- 4. Initialize values for existing loans (set defaults where NULL)
UPDATE loans 
SET 
    interest_remaining = COALESCE(interest_remaining, total_interest),
    principal_repaid_manual_override = COALESCE(principal_repaid_manual_override, FALSE)
WHERE interest_remaining IS NULL 
   OR principal_repaid_manual_override IS NULL;
