-- Check members table structure
USE minetsacco;

DESCRIBE members;

-- Check actual column names
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'minetsacco' 
  AND TABLE_NAME = 'members'
ORDER BY ORDINAL_POSITION;

-- Check loans table structure
DESCRIBE loans;

-- Check actual loans column names
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'minetsacco' 
  AND TABLE_NAME = 'loans'
ORDER BY ORDINAL_POSITION;
