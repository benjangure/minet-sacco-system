-- ============================================================================
-- ADD NEXT OF KIN COLUMNS TO GUARANTORS TABLE
-- ============================================================================
-- This enables the Next of Kin as Optional Guarantor feature
-- Run this on production server if columns don't exist
-- ============================================================================

USE minetsacco;

-- Add is_next_of_kin column
SET @column_exists_1 = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'minetsacco'
        AND TABLE_NAME = 'guarantors'
        AND COLUMN_NAME = 'is_next_of_kin'
);

SET @sql1 = IF(@column_exists_1 = 0,
    'ALTER TABLE guarantors ADD COLUMN is_next_of_kin BOOLEAN DEFAULT FALSE COMMENT ''True if this guarantor is a next of kin (non-member)''',
    'SELECT ''guarantors.is_next_of_kin already exists'' AS message'
);

PREPARE stmt1 FROM @sql1;
EXECUTE stmt1;
DEALLOCATE PREPARE stmt1;

-- Add next_of_kin_name column
SET @column_exists_2 = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'minetsacco'
        AND TABLE_NAME = 'guarantors'
        AND COLUMN_NAME = 'next_of_kin_name'
);

SET @sql2 = IF(@column_exists_2 = 0,
    'ALTER TABLE guarantors ADD COLUMN next_of_kin_name VARCHAR(255) NULL COMMENT ''Full name of next of kin''',
    'SELECT ''guarantors.next_of_kin_name already exists'' AS message'
);

PREPARE stmt2 FROM @sql2;
EXECUTE stmt2;
DEALLOCATE PREPARE stmt2;

-- Add next_of_kin_phone column
SET @column_exists_3 = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'minetsacco'
        AND TABLE_NAME = 'guarantors'
        AND COLUMN_NAME = 'next_of_kin_phone'
);

SET @sql3 = IF(@column_exists_3 = 0,
    'ALTER TABLE guarantors ADD COLUMN next_of_kin_phone VARCHAR(20) NULL COMMENT ''Phone number of next of kin (+254XXXXXXXXX format)''',
    'SELECT ''guarantors.next_of_kin_phone already exists'' AS message'
);

PREPARE stmt3 FROM @sql3;
EXECUTE stmt3;
DEALLOCATE PREPARE stmt3;

-- Add next_of_kin_relationship column
SET @column_exists_4 = (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = 'minetsacco'
        AND TABLE_NAME = 'guarantors'
        AND COLUMN_NAME = 'next_of_kin_relationship'
);

SET @sql4 = IF(@column_exists_4 = 0,
    'ALTER TABLE guarantors ADD COLUMN next_of_kin_relationship VARCHAR(50) NULL COMMENT ''Relationship to loan applicant (Spouse, Parent, Sibling, etc)''',
    'SELECT ''guarantors.next_of_kin_relationship already exists'' AS message'
);

PREPARE stmt4 FROM @sql4;
EXECUTE stmt4;
DEALLOCATE PREPARE stmt4;

-- ============================================================================
-- VERIFICATION: Check all columns were added correctly
-- ============================================================================
SELECT 
    COLUMN_NAME,
    DATA_TYPE,
    COLUMN_TYPE,
    IS_NULLABLE,
    COLUMN_DEFAULT,
    COLUMN_COMMENT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'minetsacco'
    AND TABLE_NAME = 'guarantors'
    AND COLUMN_NAME IN ('is_next_of_kin', 'next_of_kin_name', 'next_of_kin_phone', 'next_of_kin_relationship')
ORDER BY COLUMN_NAME;

-- ============================================================================
-- SUCCESS MESSAGE
-- ============================================================================
SELECT '✓ Next of Kin columns added successfully!' AS Status,
       'Next of Kin feature is now ready for production' AS Message;
