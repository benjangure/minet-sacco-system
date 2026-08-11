-- =====================================================
-- FIX: Member Credentials Showing NULL Names
-- =====================================================
-- The members table already has full_name populated
-- We just need to update the member_credentials table

USE minetsacco;

-- Temporarily disable safe update mode
SET SQL_SAFE_UPDATES = 0;

-- Step 1: Check current state
SELECT 
    'Before Fix' as status,
    COUNT(*) as total_credentials,
    SUM(CASE WHEN member_name IS NULL OR member_name = '' OR member_name = 'null' THEN 1 ELSE 0 END) as credentials_with_null_name
FROM member_credentials;

-- Step 2: Update member_credentials table with full names from members table
UPDATE member_credentials mc
INNER JOIN members m ON mc.member_id = m.id
SET mc.member_name = m.full_name
WHERE mc.member_name IS NULL 
   OR mc.member_name = '' 
   OR mc.member_name = 'null'
   OR mc.member_name LIKE '% null'
   OR mc.member_name LIKE 'null %';

-- Re-enable safe update mode
SET SQL_SAFE_UPDATES = 1;

-- Step 3: Verify the fix
SELECT 
    'After Fix' as status,
    COUNT(*) as total_credentials,
    SUM(CASE WHEN member_name IS NULL OR member_name = '' OR member_name = 'null' THEN 1 ELSE 0 END) as credentials_still_null
FROM member_credentials;

-- Step 4: Show sample of fixed credentials
SELECT 
    mc.username,
    mc.member_name,
    m.full_name as from_members_table,
    CASE 
        WHEN mc.member_name IS NULL OR mc.member_name = '' OR mc.member_name = 'null' THEN '❌ STILL NULL'
        ELSE '✅ FIXED'
    END as status
FROM member_credentials mc
INNER JOIN members m ON mc.member_id = m.id
LIMIT 20;

-- Step 5: Final summary
SELECT 
    CASE 
        WHEN (SELECT COUNT(*) FROM member_credentials WHERE member_name IS NULL OR member_name = '' OR member_name = 'null') = 0 
        THEN '✅ ALL MEMBER NAMES FIXED! Refresh your browser.'
        ELSE '⚠️ Some credentials still have null names - check the members table'
    END as result;
