-- Fix row_number columns to have default values
-- This fixes the "doesn't have a default value" error

USE minetsacco;

-- Check current structure of row_number columns
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'minetsacco'
    AND TABLE_NAME IN ('bulk_transaction_items', 'loan_migration_items')
    AND COLUMN_NAME = 'row_number';

-- Modify loan_migration_items.row_number to allow NULL or have a default
-- Use backticks because row_number is a reserved keyword
ALTER TABLE loan_migration_items
MODIFY COLUMN `row_number` INT NULL DEFAULT 0
COMMENT 'Row number for internal tracking';

-- Modify bulk_transaction_items.row_number to allow NULL or have a default (if needed)
ALTER TABLE bulk_transaction_items
MODIFY COLUMN `row_number` INT NULL DEFAULT 0
COMMENT 'Row number for internal tracking';

-- Verify the changes
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_TYPE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'minetsacco'
    AND TABLE_NAME IN ('bulk_transaction_items', 'loan_migration_items')
    AND COLUMN_NAME = 'row_number'
ORDER BY TABLE_NAME;

-- Success message
SELECT 'row_number columns fixed with default values!' AS Status;
