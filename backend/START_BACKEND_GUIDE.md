# Backend Startup Guide - Fix All Migration Issues

## Problem
Flyway migrations V145 and V146 are failing due to:
1. V145: SQL syntax error with `IF NOT EXISTS` (now fixed)
2. V146: Duplicate `full_name` column already exists in database

## Solution - One-Time Fix

### Step 1: Clean Failed Migrations from Database

Open **MySQL Workbench** and run this SQL:

```sql
USE minetsacco;

-- Remove failed migration entries
DELETE FROM flyway_schema_history WHERE version = '145' AND success = 0;
DELETE FROM flyway_schema_history WHERE version = '146' AND success = 0;

-- Verify cleanup
SELECT version, description, success, installed_on 
FROM flyway_schema_history 
WHERE version IN ('145', '146')
ORDER BY version;
```

**Expected result:** No rows with `success = 0`

### Step 2: Start the Backend

```bash
cd backend
.\mvnw spring-boot:run
```

The backend will now:
- ✅ Skip or successfully run V145 (fixed syntax)
- ✅ Skip or successfully run V146 (now idempotent - checks if columns exist)
- ✅ Start successfully

### Step 3: Verify

Check that the backend started:
- Look for: `Started MinetSaccoBackendApplication`
- Access: http://localhost:9090

## What Was Fixed

### V145 Migration Fixed
- **Before:** `ADD COLUMN IF NOT EXISTS` (MySQL doesn't support this)
- **After:** `ADD COLUMN` (standard MySQL syntax)

### V146 Migration Made Idempotent
The migration now:
1. Checks if `full_name` column exists before adding it
2. Checks if index exists before creating it
3. Checks if `last_name` column exists before dropping it
4. Uses prepared statements with conditionals for safe execution

**This means:**
- If columns already exist → Skip gracefully
- If columns don't exist → Create them
- Migration can be run multiple times safely

## If Issues Persist

### Check Database State
```sql
DESCRIBE members;
```

Look for:
- ✅ `full_name` column should exist
- ✅ `last_name` column should NOT exist (or will be removed by migration)
- ✅ `first_name` column should still exist (kept for compatibility)

### Check Flyway History
```sql
SELECT version, description, success, execution_time, installed_on 
FROM flyway_schema_history 
WHERE version >= '145'
ORDER BY version;
```

All migrations should show `success = 1`

### Nuclear Option (ONLY IF NEEDED)
If migrations are completely broken:

```sql
-- DANGEROUS: Only use if you understand the implications
USE minetsacco;

-- Check current schema first
DESCRIBE members;

-- If full_name doesn't exist, add it manually
ALTER TABLE members ADD COLUMN full_name VARCHAR(150) AFTER first_name;

-- Migrate data
UPDATE members 
SET full_name = TRIM(CONCAT(COALESCE(first_name, ''), ' ', COALESCE(last_name, '')))
WHERE (full_name IS NULL OR full_name = '') AND id > 0;

-- Make NOT NULL
ALTER TABLE members MODIFY COLUMN full_name VARCHAR(150) NOT NULL;

-- Add index
CREATE INDEX idx_members_full_name ON members(full_name);

-- Drop last_name
ALTER TABLE members DROP COLUMN last_name;

-- Mark migration as successful
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
VALUES (
    (SELECT MAX(installed_rank) + 1 FROM flyway_schema_history fsh),
    '146',
    'Consolidate Member Names To FullName',
    'SQL',
    'V146__Consolidate_Member_Names_To_FullName.sql',
    NULL,
    USER(),
    0,
    1
);
```

## Features Now Working

After successful startup:

1. ✅ **Member Name Fix**: Member Credentials page shows full names (not "null")
2. ✅ **Refresh Icon**: Header has refresh button (left of bell icon)
3. ✅ **Real-time Notifications**: WebSocket notifications work system-wide
4. ✅ **Bulk Processing**: Template uses "Full Name" instead of "First Name, Last Name"

## Files Modified

- `V145__TopUp_MultiStage_Approval_Flow.sql` - Fixed SQL syntax
- `V146__Consolidate_Member_Names_To_FullName.sql` - Made idempotent
- `BulkProcessingService.java` - Uses `getFullName()` instead of concatenation
- `RealtimeNotificationService.java` - Fixed method signatures
- `AppLayout.tsx` - Added refresh icon

## Support

If you encounter any other issues, check:
1. MySQL is running on port 3306
2. Database `minetsacco` exists
3. User `root` with password `root` has access
4. Backend logs for specific error messages
