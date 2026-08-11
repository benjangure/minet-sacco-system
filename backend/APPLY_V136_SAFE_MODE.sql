-- =====================================================
-- Apply V136 Migration with MySQL Safe Update Mode
-- =====================================================
-- Run this script if you encounter Error 1175 in MySQL Workbench
-- This script is safe to run - it uses WHERE clauses with KEY columns

-- Use your database
USE minetsacco;

-- Step 1: Verify the full_name column exists (should show it was added)
DESCRIBE members;

-- Step 2: Migrate existing data - concatenate firstName and lastName
-- Using id > 0 satisfies MySQL safe update mode
UPDT full_name = TRIM(CONCAT(
    COALESCE(first_name, ''),
    ' ',
    COALESCE(laATE members 
SEst_name, '')
))
WHERE full_name IS NULL AND id > 0;

-- Step 3: Handle cases where firstName already contains the full name
UPDATE members 
SET full_name = TRIM(first_name)
WHERE (full_name IS NULL OR full_name = '' OR full_name = ' ') AND id > 0;

-- Step 4: Verify data was migrated
SELECT 
    member_number,
    first_name,
    last_name,
    full_name,
    CASE 
        WHEN full_name IS NULL OR full_name = '' THEN 'MISSING'
        ELSE 'OK'
    END as status
FROM members 
LIMIT 20;

-- Step 5: Check for any members with missing full_name
SELECT COUNT(*) as members_with_missing_fullname
FROM members 
WHERE full_name IS NULL OR full_name = '';

-- Step 6: Make fullName NOT NULL after migration (only if all rows are populated)
-- Uncomment the line below after verifying all members have full_name populated
-- ALTER TABLE members MODIFY COLUMN full_name VARCHAR(150) NOT NULL;

-- Step 7: Add index on fullName for searching
-- Check if index already exists first
SELECT COUNT(*) INTO @index_exists
FROM information_schema.statistics 
WHERE table_schema = 'minetsacco' 
  AND table_name = 'members' 
  AND index_name = 'idx_members_full_name';

-- Only create index if it doesn't exist
SET @sql = IF(@index_exists = 0, 
    'CREATE INDEX idx_members_full_name ON members(full_name)', 
    'SELECT "Index already exists" as message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 8: Drop the old lastName column
-- Uncomment after verifying full_name is populated and code is updated
-- ALTER TABLE members DROP COLUMN last_name;

-- Final verification
SELECT 
    'Total Members' as metric,
    COUNT(*) as count
FROM members
UNION ALL
SELECT 
    'Members with full_name' as metric,
    COUNT(*) as count
FROM members 
WHERE full_name IS NOT NULL AND full_name != ''
UNION ALL
SELECT 
    'Members missing full_name' as metric,
    COUNT(*) as count
FROM members 
WHERE full_name IS NULL OR full_name = '';
