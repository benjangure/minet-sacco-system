# Production Server SQL Scripts Deployment Guide

## Overview
This guide lists all SQL scripts to run on the production server for the latest features and fixes.

## Prerequisites
- SSH access to production server
- MySQL Workbench or mysql CLI access to production database
- Backend application must be restarted after running all scripts

---

## Script 1: Complete Bulk Upload Fix
**File:** `COMPLETE_BULK_UPLOAD_FIX.sql`

**Purpose:** Fixes all bulk upload issues including:
- Bulk transaction upload
- Loan migration upload
- Missing columns (`item_row_number`, `row_number`)
- Interest rate column size issues

**Status:** ✅ **REQUIRED** - Run this first

**Tables Modified:**
- `bulk_transaction_items`
- `loan_migration_items`

**Estimated Time:** ~5 seconds

---

## Script 2: Verify Next of Kin Schema
**File:** `VERIFY_NEXT_OF_KIN_SCHEMA.sql`

**Purpose:** Check if Next of Kin columns already exist in production

**Status:** ℹ️ **VERIFICATION ONLY** - Run to check, then decide if Script 3 is needed

**Expected Output:**
- If 4 rows returned → Columns exist, skip Script 3
- If 0 rows returned → Columns missing, run Script 3

**Estimated Time:** < 1 second

---

## Script 3: Add Next of Kin Columns
**File:** `ADD_NEXT_OF_KIN_COLUMNS.sql`

**Purpose:** Enables the "Next of Kin as Optional Guarantor" feature for:
- New loan applications
- Loan top-ups

**Status:** ⚠️ **CONDITIONAL** - Only run if Script 2 shows missing columns

**Tables Modified:**
- `guarantors` (adds 4 columns)

**Columns Added:**
- `is_next_of_kin` (BOOLEAN)
- `next_of_kin_name` (VARCHAR 255)
- `next_of_kin_phone` (VARCHAR 20)
- `next_of_kin_relationship` (VARCHAR 50)

**Estimated Time:** ~3 seconds

---

## Deployment Steps

### Step 1: Backup Database
```bash
# Connect to production server
ssh user@production-server

# Backup database
mysqldump -u root -p minetsacco > minetsacco_backup_$(date +%Y%m%d_%H%M%S).sql
```

### Step 2: Run SQL Scripts

#### A. Fix Bulk Upload (REQUIRED)
```sql
-- In MySQL Workbench or mysql CLI:
USE minetsacco;
SOURCE /path/to/COMPLETE_BULK_UPLOAD_FIX.sql;
```

#### B. Verify Next of Kin Schema (CHECK FIRST)
```sql
USE minetsacco;
SOURCE /path/to/VERIFY_NEXT_OF_KIN_SCHEMA.sql;
```

#### C. Add Next of Kin Columns (IF NEEDED)
```sql
-- Only if Step B showed 0 rows
USE minetsacco;
SOURCE /path/to/ADD_NEXT_OF_KIN_COLUMNS.sql;
```

### Step 3: Restart Backend
```bash
# Restart the backend application
sudo systemctl restart minetsacco-backend
# OR
sudo service minetsacco-backend restart
# OR (if running in terminal)
# Stop current process (Ctrl+C) and run: java -jar minetsacco-backend.jar
```

### Step 4: Verify Functionality

#### Test Bulk Upload
1. Login as staff user
2. Navigate to "Bulk Processing"
3. Upload a test Excel file
4. Verify: Upload completes without errors

#### Test Loan Migration
1. Login as staff user  
2. Navigate to "Loan Migration"
3. Upload a test loan migration file
4. Verify: Upload completes without errors

#### Test Next of Kin Feature
1. Login as a member
2. Navigate to "Apply for Loan"
3. Scroll down to see "Next of Kin as Optional Guarantor" checkbox
4. Check the box and fill in details
5. Submit application
6. Verify: Application submits successfully

---

## Rollback Plan

If any issues occur:

### Restore Database
```bash
mysql -u root -p minetsacco < minetsacco_backup_YYYYMMDD_HHMMSS.sql
```

### Remove Added Columns (if needed)
```sql
USE minetsacco;

-- Remove Next of Kin columns
ALTER TABLE guarantors DROP COLUMN IF EXISTS is_next_of_kin;
ALTER TABLE guarantors DROP COLUMN IF EXISTS next_of_kin_name;
ALTER TABLE guarantors DROP COLUMN IF EXISTS next_of_kin_phone;
ALTER TABLE guarantors DROP COLUMN IF EXISTS next_of_kin_relationship;

-- Remove bulk upload columns (NOT RECOMMENDED - these are needed)
-- ALTER TABLE bulk_transaction_items DROP COLUMN IF EXISTS item_row_number;
-- ALTER TABLE bulk_transaction_items DROP COLUMN IF EXISTS `row_number`;
-- ALTER TABLE loan_migration_items DROP COLUMN IF EXISTS item_row_number;
-- ALTER TABLE loan_migration_items DROP COLUMN IF EXISTS `row_number`;
```

---

## Summary

| Script | Required | Order | Restart Backend After |
|--------|----------|-------|----------------------|
| COMPLETE_BULK_UPLOAD_FIX.sql | ✅ Yes | 1 | After all scripts |
| VERIFY_NEXT_OF_KIN_SCHEMA.sql | ℹ️ Check only | 2 | No |
| ADD_NEXT_OF_KIN_COLUMNS.sql | ⚠️ Conditional | 3 | After all scripts |

**Total Estimated Time:** < 1 minute (excluding backup and restart)

---

## Post-Deployment Checklist

- [ ] All SQL scripts ran without errors
- [ ] Verification queries show expected columns
- [ ] Backend application restarted successfully
- [ ] Bulk upload tested and working
- [ ] Loan migration tested and working
- [ ] Next of Kin feature visible on member loan application
- [ ] Next of Kin feature visible on member top-up request
- [ ] No console errors in browser developer tools

---

## Support

If you encounter any issues:

1. Check MySQL error logs: `/var/log/mysql/error.log`
2. Check backend application logs
3. Verify database connection settings in backend `.env` file
4. Restore from backup if critical issue occurs

---

**Last Updated:** February 7, 2026
**Version:** 1.0
