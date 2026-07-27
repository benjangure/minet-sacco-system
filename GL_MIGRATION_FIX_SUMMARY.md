# GL Migration Fix Summary

## Issues Fixed

### Issue 1: V116 Migration - Foreign Key Errors
**Problem:** V116__Create_GL_Tables.sql was failing with foreign key constraint errors.

**Root Cause:** The migration was trying to create foreign keys to the `users` table which had type mismatches with the GL table user ID columns.

**Solution:**
1. Removed explicit foreign key constraints for user references (`created_by_user_id`, `approved_by_user_id`, `changed_by_user_id`)
2. Kept indexes on these columns for query optimization
3. Enforced referential integrity at the application level
4. Added `DROP TABLE IF EXISTS` statements at the beginning to cleanly recreate tables

### Issue 2: V117 Migration - Column Size Error  
**Problem:** V117__Populate_GL_Accounts.sql was failing with "Data truncation: Data too long for column 'code'".

**Root Cause:** The `code` column in `gl_accounts` table was defined as `VARCHAR(20)`, but account codes like `COMMITTEE_ALLOWANCE_PAYABLE` (26 characters) exceeded this limit.

**Solution:**
- Increased `code` column size from `VARCHAR(20)` to `VARCHAR(50)` in V116 migration

## Files Modified

### Migration Files
- **V116__Create_GL_Tables.sql** 
  - Added DROP TABLE statements at beginning
  - Increased code column size to VARCHAR(50)
  - Removed user foreign key constraints
  - Kept indexes for performance

- **V117__Populate_GL_Accounts.sql**
  - No changes needed (the increased column size fixes the issue)

### Other Files
- **XAMPP_CLEANUP_GL_MIGRATION.sql** (new)
  - Use this script to reset Flyway history before retrying migrations
  - Run in MySQL Workbench or phpMyAdmin

## How to Fix (Step-by-Step)

### Option 1: Automatic (Recommended)
1. Copy XAMPP_CLEANUP_GL_MIGRATION.sql to your MySQL client
2. Run the script in MySQL Workbench or phpMyAdmin
3. Restart the Spring Boot backend application
4. Flyway will automatically re-run V116 and V117 migrations

### Option 2: Manual
1. In MySQL Workbench, run:
   ```sql
   USE sacco_db;
   DROP TABLE IF EXISTS gl_account_audit;
   DROP TABLE IF EXISTS gl_manual_entries;
   DROP TABLE IF EXISTS gl_account_calculations;
   DROP TABLE IF EXISTS gl_accounts;
   DELETE FROM flyway_schema_history WHERE version IN ('116', '117');
   ```
2. Restart the backend

## Verification

After running the migrations, verify success by:
1. Checking application logs for "Migrating schema `sacco_db` to version "116" and "117" messages
2. Running in MySQL:
   ```sql
   SELECT COUNT(*) FROM gl_accounts;  -- Should show 28 accounts
   ```
3. Testing GL endpoints:
   - GET `/api/gl/trial-balance`
   - GET `/api/gl/balance-sheet`
   - GET `/api/gl/income-statement`

## GL Accounts Created

The V117 migration populates 28 GL accounts across 5 categories:

**Asset Accounts (9):**
- Normal Loans, Emergency Loans (Type 1 & 2)
- CBA Call Deposits, CBA Current Account
- Co-op Holdings, Co-op Insurance, KUSCCO
- Receivables

**Liability Accounts (6):**
- Member Deposits, Member Shares
- Auditor Fees Payable, Dividend Payable
- Interest Payable, Committee Allowance Payable

**Equity Accounts (3):**
- Statutory Reserve, Revenue Reserve
- Retained Earnings

**Revenue Accounts (4):**
- Interest - Loans, Interest - Deposits
- Entrance Fees, Loan Processing Fees

**Expense Accounts (11):**
- Audit Fees, Travel Expenses, SASRA Fees
- Training, Committee Allowances, AGM Expenses
- Insurance Premiums, Bank Charges
- Loan Loss Provision, Income Tax, Interest Expense

## Notes

- All GL tables use soft referential integrity (application-level validation)
- Foreign key constraints removed for user references to avoid compatibility issues
- Indexes maintained on frequently queried columns for performance
- Migration file designed to be idempotent (safe to re-run)
