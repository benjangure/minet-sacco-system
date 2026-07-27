-- Add original_principal column to loans table
-- This field stores the original loan principal amount and never changes
-- Used for proportional guarantee calculations (principal only, not interest)

ALTER TABLE loans 
ADD COLUMN IF NOT EXISTS original_principal DECIMAL(19,2) DEFAULT 0.00 NOT NULL 
COMMENT 'Original loan principal amount. Used for proportional guarantee calculations. Never changes after loan creation.';

-- Update existing loans to set original_principal from amount column
UPDATE loans 
SET original_principal = amount 
WHERE original_principal = 0.00 AND amount IS NOT NULL;

-- Add index for performance
CREATE INDEX IF NOT EXISTS idx_loans_original_principal ON loans(original_principal);

SELECT 'original_principal column added to loans table successfully' as status;
