-- Check the EXACT current schema of loan_migration_items table
-- Run this in MySQL Workbench or your database client and share the output

DESCRIBE loan_migration_items;

-- Also check which columns are currently NOT NULL
SELECT 
    COLUMN_NAME, 
    DATA_TYPE, 
    IS_NULLABLE, 
    COLUMN_KEY,
    EXTRA
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'loan_migration_items' 
  AND TABLE_SCHEMA = DATABASE()
ORDER BY ORDINAL_POSITION;
