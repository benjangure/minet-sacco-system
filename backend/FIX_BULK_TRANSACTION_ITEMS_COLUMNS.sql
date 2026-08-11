-- Fix bulk_transaction_items table - add both missing columns
-- This fixes the bulk upload and loan migration issues

USE minetsacco;

-- First, check current table structure
DESCRIBE bulk_transaction_items;

-- Add item_row_number column if it doesn't exist
SET @column_exists = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'minetsacco'
        AND TABLE_NAME = 'bulk_transaction_items'
        AND COLUMN_NAME = 'item_row_number'
);

SET @sql = IF(@column_exists = 0,
    'ALTER TABLE bulk_transaction_items ADD COLUMN item_row_number INT NULL COMMENT ''Row number from the uploaded file for tracking''',
    'SELECT ''Column item_row_number already exists'' AS message'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add row_number column with default value
SET @column_exists_2 = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'minetsacco'
        AND TABLE_NAME = 'bulk_transaction_items'
        AND COLUMN_NAME = 'row_number'
);

SET @sql2 = IF(@column_exists_2 = 0,
    'ALTER TABLE bulk_transaction_items ADD COLUMN row_number INT NULL DEFAULT 0 COMMENT ''Row number for internal tracking''',
    'SELECT ''Column row_number already exists'' AS message'
);

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- Verify both columns were added
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'minetsacco'
    AND TABLE_NAME = 'bulk_transaction_items'
    AND COLUMN_NAME IN ('item_row_number', 'row_number')
ORDER BY COLUMN_NAME;

-- Show complete table structure
DESCRIBE bulk_transaction_items;
