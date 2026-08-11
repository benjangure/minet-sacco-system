-- ✅ RUN THIS SQL SCRIPT ONLY
-- The interest_collected_manual_override column already exists

-- Add the principal_repaid column to store manual value
ALTER TABLE loans 
ADD COLUMN principal_repaid DECIMAL(15, 2) DEFAULT NULL 
COMMENT 'Manually set principal repaid amount (overrides calculated value)';

-- Add the override flag
ALTER TABLE loans 
ADD COLUMN principal_repaid_manual_override BOOLEAN DEFAULT FALSE 
COMMENT 'TRUE when treasurer manually set principalRepaid, FALSE for automatic calculation';

-- Set existing loans to FALSE (they use automatic calculation)
UPDATE loans 
SET principal_repaid_manual_override = FALSE;

-- Verify the columns were added
SELECT 'Columns added successfully!' AS Status;

SELECT COLUMN_NAME, COLUMN_TYPE, COLUMN_DEFAULT, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'loans' 
AND COLUMN_NAME IN ('interest_collected_manual_override', 'principal_repaid', 'principal_repaid_manual_override')
ORDER BY COLUMN_NAME;
