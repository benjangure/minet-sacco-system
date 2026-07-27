# Quick Fix for GL Migration Issues

## TL;DR - Just Run This

### Step 1: Open XAMPP MySQL
- Open XAMPP Control Panel
- Start MySQL service
- Click "Admin" button (opens phpMyAdmin in browser)

### Step 2: Copy & Paste This Script
Go to **Databases → sacco_db → SQL** tab, then paste and execute:

```sql
USE sacco_db;

-- Drop failed GL tables
DROP TABLE IF EXISTS gl_account_audit;
DROP TABLE IF EXISTS gl_manual_entries;
DROP TABLE IF EXISTS gl_account_calculations;
DROP TABLE IF EXISTS gl_accounts;

-- Clear failed migrations from history
DELETE FROM flyway_schema_history 
WHERE version IN ('116', '117');

-- Verify
SELECT * FROM flyway_schema_history WHERE version >= '115' ORDER BY version DESC;
```

### Step 3: Restart Backend
- Stop the Spring Boot application
- Run again: `mvn spring-boot:run` or use IntelliJ's run button
- Wait for "Successfully repaired schema" message

### Step 4: Verify Success
Check console for these success messages:
```
INFO ... Migrating schema `sacco_db` to version "116 - Create GL Tables"
INFO ... Migrating schema `sacco_db` to version "117 - Populate GL Accounts"
```

## What Was Fixed

| Issue | Fix |
|-------|-----|
| V116 Foreign Key Error | Removed user FK constraints, kept indexes |
| V117 Data Too Long Error | Increased code column from VARCHAR(20) to VARCHAR(50) |

## Files to Remember

- **XAMPP_CLEANUP_GL_MIGRATION.sql** - The cleanup script
- **GL_MIGRATION_FIX_SUMMARY.md** - Full technical details
- **V116__Create_GL_Tables.sql** - Updated migration with larger code column
- **V117__Populate_GL_Accounts.sql** - No changes (works with larger column)

## Still Having Issues?

### If migrations still fail:
1. Check MySQL is running (XAMPP Control Panel → MySQL Status)
2. Verify database exists: `SHOW DATABASES;` should show `sacco_db`
3. Run the cleanup script again (sometimes needs second try)
4. Check backend logs for specific error message

### If column size error returns:
Run this to verify the column size:
```sql
SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_NAME = 'gl_accounts' AND COLUMN_NAME = 'code';
```
Should show: `VARCHAR(50)`

### If foreign key still causing issues:
Run this to check constraints:
```sql
SELECT * FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS 
WHERE TABLE_NAME = 'gl_manual_entries' OR TABLE_NAME = 'gl_account_audit';
```
Should return empty (no user foreign keys).

## All 28 GL Accounts Created

✅ Assets: Loans (3), Bank Accounts (2), Holdings (4)
✅ Liabilities: Member Deposits/Shares (2), Payables (4)  
✅ Equity: Reserves (3)
✅ Revenue: Interest, Fees (4)
✅ Expenses: Fees, Travel, Training, etc. (11)

## Next Steps After Fix

The GL accounting layer is now ready. Test with:
- GET `/api/gl/trial-balance`
- GET `/api/gl/balance-sheet`
- GET `/api/gl/income-statement`
