# Final Fix for Migration V56 - Duplicate Column Error

## Problem
Migration V56 is failing with: **"Duplicate column name 'loan_id'"**

This means the columns (`loan_id`, `deposit_request_id`, `guarantor_id`, `category`) already exist in the notifications table from a previous migration.

## Root Cause
The columns were already added in a previous migration, but V56 is trying to add them again without checking if they exist.

## Solution

### Step 1: Clean Up Failed Migration (REQUIRED)

Execute this SQL in your MySQL database:

```sql
USE sacco_db;

-- Delete the failed migration record
DELETE FROM flyway_schema_history WHERE version = 56;

-- Verify it's deleted (should return 0 rows)
SELECT COUNT(*) FROM flyway_schema_history WHERE version = 56;
```

**How to execute:**
1. Open MySQL Workbench or MySQL command line
2. Connect to your database (localhost:3306, user: root, password: root)
3. Copy and paste the SQL above
4. Execute it

### Step 2: Verify Column Status

Run this to see what columns exist:

```sql
USE sacco_db;

-- Check all columns in notifications table
DESCRIBE notifications;

-- Check specifically for the columns we need
SELECT COLUMN_NAME 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'sacco_db' 
AND TABLE_NAME = 'notifications'
AND COLUMN_NAME IN ('loan_id', 'deposit_request_id', 'guarantor_id', 'category');
```

Expected output: You should see all 4 columns already exist.

### Step 3: Rebuild Backend

```bash
cd backend
mvn clean install -DskipTests
```

### Step 4: Start Backend

```bash
mvn spring-boot:run
```

Expected output:
```
2026-03-27T15:XX:XX.XXX+03:00  INFO ... Migrating schema `sacco_db` to version "56 - Add notification context fields"
2026-03-27T15:XX:XX.XXX+03:00  INFO ... Successfully applied 1 migration to schema `sacco_db`
2026-03-27T15:XX:XX.XXX+03:00  INFO ... Started MinetSaccoBackendApplication
```

## What Changed in V56

The migration now uses `IF NOT EXISTS` syntax:

```sql
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS loan_id BIGINT;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS deposit_request_id BIGINT;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS guarantor_id BIGINT;
ALTER TABLE notifications ADD COLUMN IF NOT EXISTS category VARCHAR(50);

ALTER TABLE notifications ADD CONSTRAINT IF NOT EXISTS fk_notification_loan FOREIGN KEY (loan_id) REFERENCES loans(id) ON DELETE SET NULL;
ALTER TABLE notifications ADD CONSTRAINT IF NOT EXISTS fk_notification_guarantor FOREIGN KEY (guarantor_id) REFERENCES guarantors(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_notification_loan_id ON notifications(loan_id);
CREATE INDEX IF NOT EXISTS idx_notification_category ON notifications(category);
```

This way:
- ✅ If columns already exist, they won't be added again
- ✅ If constraints already exist, they won't be added again
- ✅ If indexes already exist, they won't be created again
- ✅ Migration will succeed regardless of current state

## Troubleshooting

### If you still get "Duplicate column" error:

1. Verify the deletion worked:
```sql
SELECT COUNT(*) FROM flyway_schema_history WHERE version = 56;
-- Should return 0
```

2. Check if there are other failed migrations:
```sql
SELECT * FROM flyway_schema_history WHERE success = 0;
```

3. If there are other failed migrations, delete them:
```sql
DELETE FROM flyway_schema_history WHERE success = 0;
```

4. Rebuild and restart backend

### If backend still won't start:

1. Check the exact error in the logs
2. Verify MySQL is running: `mysql -u root -p"root" -e "SELECT 1;"`
3. Verify database exists: `mysql -u root -p"root" -e "SHOW DATABASES;"`
4. Try a clean rebuild: `mvn clean install -DskipTests`

### If you see "outOfOrder mode is active" warning:

This is normal and expected. It means Flyway is allowing out-of-order migrations. This is fine for development.

## Verification

After successful startup, verify:

1. Check migration history:
```sql
SELECT version, description, success FROM flyway_schema_history 
WHERE version >= 55 
ORDER BY version;
```

Expected output:
```
version | description                              | success
--------|------------------------------------------|--------
55      | Fix_member_user_member_id               | 1
56      | Add_notification_context_fields         | 1
57      | Create_audit_logs_table                 | 1
```

2. Check notifications table has all columns:
```sql
DESCRIBE notifications;
```

Should include: `loan_id`, `deposit_request_id`, `guarantor_id`, `category`

3. Check audit_logs table was created:
```sql
DESCRIBE audit_logs;
```

Should show all audit log columns

## Files Modified

- `backend/src/main/resources/db/migration/V56__Add_notification_context_fields.sql` - Updated to use IF NOT EXISTS

## Next Steps

1. Execute the cleanup SQL
2. Rebuild backend: `mvn clean install -DskipTests`
3. Start backend: `mvn spring-boot:run`
4. Rebuild frontend: `npm run build && npm run dev`
5. Test audit trail functionality

## Success Criteria

✅ Backend starts without errors
✅ All migrations run successfully (V55, V56, V57)
✅ No "Duplicate column" errors
✅ No "failed migration" errors
✅ Audit trail page loads and works
✅ Audit logs appear when actions are performed

## Support

If you encounter any issues:

1. Check the backend logs for the exact error
2. Verify MySQL is running and accessible
3. Verify the cleanup SQL was executed
4. Try a clean rebuild: `mvn clean install -DskipTests`
5. Check that all migrations are marked as success in flyway_schema_history
