-- Add missing item_row_number column to bulk_transaction_items table
-- This column is needed for tracking the row number in the uploaded file

USE minetsacco;

-- Check if column exists before adding
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'minetsacco'
    AND TABLE_NAME = 'bulk_transaction_items'
    AND COLUMN_NAME = 'item_row_number';

-- Add the column (remove IF NOT EXISTS - not supported in MySQL ALTER TABLE)
ALTER TABLE bulk_transaction_items
ADD COLUMN item_row_number INT NULL
COMMENT 'Row number from the uploaded file for tracking';

-- Verify the column was added
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'minetsacco'
    AND TABLE_NAME = 'bulk_transaction_items'
    AND COLUMN_NAME = 'item_row_number';

-- Show the complete table structure
DESCRIBE bulk_transaction_items;
