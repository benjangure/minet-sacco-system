-- ============================================================================
-- COMPLETE BULK UPLOAD FIX FOR PRODUCTION SERVER
-- ============================================================================
-- This script fixes all bulk upload issues:
-- 1. Adds missing item_row_number columns
-- 2. Fixes row_number columns with default values
-- 3. Fixes interest_rate column size
-- Run this on production server, then restart backend
-- ============================================================================

USE minetsacco;

-- ============================================================================
-- STEP 1: Add item_row_number to bulk_transaction_items
-- ============================================================================
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

-- ============================================================================
-- STEP 2: Add row_number to bulk_transaction_items
-- ============================================================================
SET @column_exists_2 = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'minetsacco'
        AND TABLE_NAME = 'bulk_transaction_items'
        AND COLUMN_NAME = 'row_number'
);

SET @sql2 = IF(@column_exists_2 = 0,
    'ALTER TABLE bulk_transaction_items ADD COLUMN `row_number` INT NULL DEFAULT 0 COMMENT ''Row number for internal tracking''',
    'SELECT ''bulk_transaction_items.row_number already exists'' AS message'
);

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- ============================================================================
-- STEP 3: Fix row_number default value in bulk_transaction_items
-- ============================================================================
ALTER TABLE bulk_transaction_items
MODIFY COLUMN `row_number` INT NULL DEFAULT 0
COMMENT 'Row number for internal tracking';

-- ============================================================================
-- STEP 4: Add item_row_number to loan_migration_items
-- ============================================================================
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

-- ============================================================================
-- STEP 5: Add row_number to loan_migration_items
-- ============================================================================
SET @column_exists_4 = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'minetsacco'
        AND TABLE_NAME = 'loan_migration_items'
        AND COLUMN_NAME = 'row_number'
);

SET @sql4 = IF(@column_exists_4 = 0,
    'ALTER TABLE loan_migration_items ADD COLUMN `row_number` INT NULL DEFAULT 0 COMMENT ''Row number for internal tracking''',
    'SELECT ''loan_migration_items.row_number already exists'' AS message'
);

PREPARE stmt4 FROM @sql4;
EXECUTE stmt4;
DEALLOCATE PREPARE stmt4;

-- ============================================================================
-- STEP 6: Fix row_number default value in loan_migration_items
-- ============================================================================
ALTER TABLE loan_migration_items
MODIFY COLUMN `row_number` INT NULL DEFAULT 0
COMMENT 'Row number for internal tracking';

-- ============================================================================
-- STEP 7: Fix interest_rate column size in loan_migration_items
-- ============================================================================
ALTER TABLE loan_migration_items
MODIFY COLUMN interest_rate DECIMAL(10,4) NULL
COMMENT 'Annual interest rate percentage';

-- ============================================================================
-- STEP 8: Fix interest_collected column size
-- ============================================================================
ALTER TABLE loan_migration_items
MODIFY COLUMN interest_collected DECIMAL(15,2) NULL DEFAULT 0
COMMENT 'Total interest collected so far';

-- ============================================================================
-- STEP 9: Fix total_interest column size
-- ============================================================================
ALTER TABLE loan_migration_items
MODIFY COLUMN total_interest DECIMAL(15,2) NULL DEFAULT 0
COMMENT 'Total interest for the loan';

-- ============================================================================
-- VERIFICATION: Check all columns were fixed correctly
-- ============================================================================
SELECT 
    TABLE_NAME,
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'minetsacco'
    AND (
        (TABLE_NAME = 'bulk_transaction_items' AND COLUMN_NAME IN ('item_row_number', 'row_number'))
        OR
        (TABLE_NAME = 'loan_migration_items' AND COLUMN_NAME IN ('item_row_number', 'row_number', 'interest_rate', 'interest_collected', 'total_interest'))
    )
ORDER BY TABLE_NAME, COLUMN_NAME;

-- ============================================================================
-- SUCCESS MESSAGE
-- ============================================================================
SELECT '✓ All bulk upload fixes applied successfully!' AS Status,
       '⚠ Remember to restart the backend server for changes to take effect' AS Important;
