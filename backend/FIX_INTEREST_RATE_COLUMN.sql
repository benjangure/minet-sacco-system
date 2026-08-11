-- Fix interest_rate column to handle larger values
-- Current error: "Out of range value for column 'interest_rate'"

USE minetsacco;

-- Check current structure of interest_rate column
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE,
    NUMERIC_PRECISION,
    NUMERIC_SCALE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'minetsacco'
    AND TABLE_NAME = 'loan_migration_items'
    AND COLUMN_NAME = 'interest_rate';

-- Modify interest_rate to DECIMAL(10,4) to handle larger values
-- This allows values like 12.50, 15.75, etc. (up to 999999.9999)
ALTER TABLE loan_migration_items
MODIFY COLUMN interest_rate DECIMAL(10,4) NULL
COMMENT 'Annual interest rate percentage';

-- Also fix related columns that might have the same issue
ALTER TABLE loan_migration_items
MODIFY COLUMN interest_collected DECIMAL(15,2) NULL DEFAULT 0
COMMENT 'Total interest collected so far';

ALTER TABLE loan_migration_items
MODIFY COLUMN total_interest DECIMAL(15,2) NULL DEFAULT 0
COMMENT 'Total interest for the loan';

-- Verify the changes
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE,
    NUMERIC_PRECISION,
    NUMERIC_SCALE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'minetsacco'
    AND TABLE_NAME = 'loan_migration_items'
    AND COLUMN_NAME IN ('interest_rate', 'interest_collected', 'total_interest')
ORDER BY COLUMN_NAME;

-- Success message
SELECT 'interest_rate and related columns fixed!' AS Status;
