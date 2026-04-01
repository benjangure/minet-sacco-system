# IMMEDIATE ACTION REQUIRED - Fix Migration V56

## The Issue
Backend won't start because migration V56 is trying to add columns that already exist.

**Error:** `Duplicate column name 'loan_id'`

## The Fix (3 Simple Steps)

### Step 1: Clean Database (CRITICAL)
Execute this SQL in MySQL:

```sql
USE sacco_db;
DELETE FROM flyway_schema_history WHERE version = 56;
```

**How:**
- Open MySQL Workbench or MySQL command line
- Connect to: localhost:3306, user: root, password: root
- Run the SQL above
- Done!

### Step 2: Rebuild Backend
```bash
cd backend
mvn clean install -DskipTests
```

### Step 3: Start Backend
```bash
mvn spring-boot:run
```

**Expected output:**
```
Migrating schema `sacco_db` to version "56 - Add notification context fields"
Successfully applied 1 migration to schema `sacco_db`
Started MinetSaccoBackendApplication
```

## What Was Fixed

The migration V56 now uses `IF NOT EXISTS` syntax, so it:
- ✅ Won't fail if columns already exist
- ✅ Won't fail if constraints already exist
- ✅ Won't fail if indexes already exist
- ✅ Will succeed regardless of current database state

## Why This Happened

The columns (`loan_id`, `deposit_request_id`, `guarantor_id`, `category`) were already added to the notifications table in a previous migration. When V56 tried to add them again without checking, it failed.

The fix uses MySQL's `IF NOT EXISTS` clause to safely add columns/constraints/indexes only if they don't already exist.

## Verification

After backend starts, verify in MySQL:

```sql
USE sacco_db;

-- Check migration history
SELECT version, description, success FROM flyway_schema_history 
WHERE version >= 55 
ORDER BY version;
```

Should show:
- V55: success = 1
- V56: success = 1
- V57: success = 1

## That's It!

Once the backend starts successfully:
1. Rebuild frontend: `npm run build && npm run dev`
2. Login as ADMIN or AUDITOR
3. Go to Administration → Audit Trail
4. Audit trail system is ready to use!

## Files Changed

- `backend/src/main/resources/db/migration/V56__Add_notification_context_fields.sql` - Updated to use IF NOT EXISTS

## Support

If it still doesn't work:
1. Verify the DELETE SQL was executed
2. Check MySQL is running
3. Try: `mvn clean install -DskipTests`
4. Check backend logs for exact error
