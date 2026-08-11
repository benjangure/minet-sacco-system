-- Verify Next of Kin columns exist in guarantors table
-- Check if the database is ready for Next of Kin feature

USE minetsacco;

-- Check current structure of guarantors table
DESCRIBE guarantors;

-- Check specifically for Next of Kin columns
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

-- If no rows returned above, the columns are missing!
