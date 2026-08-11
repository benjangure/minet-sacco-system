-- Fix ALL bulk upload tables - add missing item_row_number column
-- This fixes bulk transactions, loan migration, and any other bulk upload features

USE minetsacco;

-- Fix bulk_transaction_items table
SET @column_exists_1 = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'minetsacco'
        AND TABLE_NAME = 'bulk_transaction_items'
        AND COLUMN_NAME = 'item_row_number'
);

SET @sql1 = IF(@column_exists_1 = 0,
    'ALTER TABLE bulk_transaction_items ADD COLUMN item_row_number INT NULL COMMENT ''Row number from the uploaded file''',
    'SELECT ''bulk_transaction_items.item_row_number already exists'' AS message'
);

PREPARE stmt1 FROM @sql1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

-- Fix bulk_transaction_items.row_number
SET @column_exists_2 = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'minetsacco'
        AND TABLE_NAME = 'bulk_transaction_items'
        AND COLUMN_NAME = 'row_number'
);

SET @sql2 = IF(@column_exists_2 = 0,
    'ALTER TABLE bulk_transaction_items ADD COLUMN row_number INT NULL DEFAULT 0 COMMENT ''Row number for internal tracking''',
    'SELECT ''bulk_transaction_items.row_number already exists'' AS message'
);

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- Fix loan_migration_items table
SET @column_exists_3 = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'minetsacco'
        AND TABLE_NAME = 'loan_migration_items'
        AND COLUMN_NAME = 'item_row_number'
);

SET @sql3 = IF(@column_exists_3 = 0,
    'ALTER TABLE loan_migration_items ADD COLUMN item_row_number INT NULL COMMENT ''Row number from the uploaded file''',
    'SELECT ''loan_migration_items.item_row_number already exists'' AS message'
);

PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

-- Fix loan_migration_items.row_number
SET @column_exists_4 = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'minetsacco'
        AND TABLE_NAME = 'loan_migration_items'
        AND COLUMN_NAME = 'row_number'
);

SET @sql4 = IF(@column_exists_4 = 0,
    'ALTER TABLE loan_migration_items ADD COLUMN row_number INT NULL DEFAULT 0 COMMENT ''Row number for internal tracking''',
    'SELECT ''loan_migration_items.row_number already exists'' AS message'
);

PREPARE stmt4 FROM @sql4;
EXECUTE stmt4;
DEALLOCATE PREPARE stmt4;

-- Verify all columns were added
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'minetsacco'
    AND TABLE_NAME IN ('bulk_transaction_items', 'loan_migration_items')
    AND COLUMN_NAME IN ('item_row_number', 'row_number')
ORDER BY TABLE_NAME, COLUMN_NAME;

-- Success message
SELECT 'All bulk upload tables have been fixed!' AS Status;
