-- =====================================================
-- COMPLETE FIX: Member Names Showing NULL
-- =====================================================
-- Run this entire script in MySQL Workbench to fix the issue once and for all

USE minetsacco;

-- Step 1: Check current state
SELECT 
    'Current State Check' as step,
    COUNT(*) as total_members,
    SUM(CASE WHEN full_name IS NULL OR full_name = '' THEN 1 ELSE 0 END) as members_with_null_fullname,
    SUM(CASE WHEN first_name IS NOT NULL AND first_name != '' THEN 1 ELSE 0 END) as members_with_firstname
FROM members;

-- Step 2: Ensure full_name column exists
SET @col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'members' AND COLUMN_NAME = 'full_name');

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE members ADD COLUMN full_name VARCHAR(150) AFTER first_name', 
    'SELECT "Column full_name already exists" AS message');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Step 3: Migrate data from first_name + last_name to full_name
-- Handle all possible cases
UPDATE members 
SET full_name = CASE
    -- If last_name exists, concatenate first_name + last_name
    WHEN last_name IS NOT NULL AND last_name != '' THEN 
        TRIM(CONCAT(COALESCE(first_name, ''), ' ', COALESCE(last_name, '')))
    -- If only first_name exists, use it
    WHEN first_name IS NOT NULL AND first_name != '' THEN 
        TRIM(first_name)
    -- Default to 'Unknown'
    ELSE 'Unknown'
END
WHERE (full_name IS NULL OR full_name = '' OR full_name = ' ') 
  AND id > 0;

-- Step 4: Check results after migration
SELECT 
    'After Migration' as step,
    COUNT(*) as total_members,
    SUM(CASE WHEN full_name IS NULL OR full_name = '' THEN 1 ELSE 0 END) as members_with_null_fullname,
    SUM(CASE WHEN full_name = 'Unknown' THEN 1 ELSE 0 END) as members_with_unknown_name
FROM members;

-- Step 5: Show sample of migrated data
SELECT 
    id,
    member_number,
    first_name,
    last_name,
    full_name,
    CASE 
        WHEN full_name IS NULL OR full_name = '' THEN 'STILL NULL'
        ELSE 'OK'
    END as status
FROM members 
LIMIT 20;

-- Step 6: Update member_credentials table with full names
UPDATE member_credentials mc
INNER JOIN members m ON mc.member_id = m.id
SET mc.member_name = m.full_name
WHERE mc.member_name IS NULL 
   OR mc.member_name = '' 
   OR mc.member_name = 'null'
   OR mc.member_name LIKE '% null'
   OR mc.member_name LIKE 'null %';

-- Step 7: Verify member_credentials are fixed
SELECT 
    'Member Credentials Status' as step,
    COUNT(*) as total_credentials,
    SUM(CASE WHEN member_name IS NULL OR member_name = '' OR member_name = 'null' THEN 1 ELSE 0 END) as credentials_with_null_name
FROM member_credentials;

-- Step 8: Show sample of fixed credentials
SELECT 
    mc.member_id,
    mc.username,
    mc.member_name,
    m.full_name as actual_full_name,
    CASE 
        WHEN mc.member_name IS NULL OR mc.member_name = '' OR mc.member_name = 'null' THEN 'STILL NULL'
        ELSE 'OK'
    END as status
FROM member_credentials mc
INNER JOIN members m ON mc.member_id = m.id
LIMIT 20;

-- Step 9: Clean up Flyway history (remove failed migrations)
DELETE FROM flyway_schema_history WHERE version IN ('145', '146') AND success = 0;

-- Step 10: Final summary
SELECT '✅ FIX COMPLETE' as status, 
       'Restart backend to apply code changes' as next_step;

SELECT 
    'Final Status' as report,
    (SELECT COUNT(*) FROM members WHERE full_name IS NULL OR full_name = '') as members_with_null_fullname,
    (SELECT COUNT(*) FROM member_credentials WHERE member_name IS NULL OR member_name = '' OR member_name = 'null') as credentials_with_null_name,
    CASE 
        WHEN (SELECT COUNT(*) FROM members WHERE full_name IS NULL OR full_name = '') = 0 
         AND (SELECT COUNT(*) FROM member_credentials WHERE member_name IS NULL OR member_name = '' OR member_name = 'null') = 0 
        THEN '✅ ALL FIXED'
        ELSE '⚠️ SOME ISSUES REMAIN'
    END as result;
