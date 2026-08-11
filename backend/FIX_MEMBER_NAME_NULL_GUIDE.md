# Fix "null" in Member Name Column - Quick Guide

## Problem
The Member Credentials page shows "null" in the Member Name column after database migration.

## Root Cause
The database migration V136 consolidated `first_name` and `last_name` columns into a single `full_name` column. The backend code was still trying to concatenate `firstName + " " + lastName`, which resulted in "null" because `lastName` column no longer exists.

## Solution Applied

### 1. Backend Code Fixed ✅
**File:** `backend/src/main/java/com/minet/sacco/service/BulkProcessingService.java`

Changed line 1149 from:
```java
member.getFirstName() + " " + member.getLastName()
```

To:
```java
member.getFullName()
```

### 2. Database Migration (Manual Step Required)

Since you encountered MySQL safe update mode error, follow these steps:

#### Option A: Run the Safe Mode Script (Recommended)

1. Open MySQL Workbench
2. Open the file: `backend/APPLY_V146_SAFE_MODE.sql`
3. Execute the entire script
4. Review the verification results at the end

#### Option B: Disable Safe Mode Temporarily

1. In MySQL Workbench, go to **Edit → Preferences → SQL Editor**
2. Uncheck **"Safe Updates"**
3. Reconnect to the database
4. Run the migration: `backend/src/main/resources/db/migration/V146__Consolidate_Member_Names_To_FullName.sql`
5. Re-enable Safe Updates

## Verification Steps

After running the migration:

```sql
-- 1. Check that full_name column exists and is populated
SELECT member_number, first_name, last_name, full_name 
FROM members 
LIMIT 10;

-- 2. Verify no null or empty full_name values
SELECT COUNT(*) 
FROM members 
WHERE full_name IS NULL OR full_name = '';
-- Should return 0
```

## Test the Fix

1. **Restart the backend** (to reload the updated Java class)
   ```bash
   # Stop the running backend if it's running
   # Then start it again
   ```

2. **Add a test member** via Bulk Processing with a Full Name

3. **Check Member Credentials page**
   - Navigate to: Administration → Member Credentials
   - Verify the Member Name column shows full names correctly (not "null")

## Files Modified

- ✅ `backend/src/main/java/com/minet/sacco/service/BulkProcessingService.java` - Uses `getFullName()`
- ✅ `backend/src/main/resources/db/migration/V146__Consolidate_Member_Names_To_FullName.sql` - Updated with safe mode workaround
- ✅ `minetsacco-main/src/pages/BulkProcessing.tsx` - Template already shows "Full Name" instead of "First Name, Last Name"

## Notes

- The `last_name` column will be dropped by the migration
- The `first_name` column is kept temporarily for backward compatibility
- A future migration can drop `first_name` once all code references are updated
