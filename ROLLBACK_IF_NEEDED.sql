-- ========================================
-- EMERGENCY ROLLBACK SCRIPT
-- ========================================
-- Use this if something goes wrong during the process
-- This will restore the database from the backup file
-- ========================================

-- ⚠️  THIS SCRIPT IS FOR REFERENCE ONLY
-- You need to execute these commands from PowerShell/Command Line

/*

EMERGENCY ROLLBACK PROCEDURE
=============================

If the financial data deletion or import goes wrong, follow these steps:

STEP 1: Find your backup file
------------------------------
Location: .\database_backups\backup_before_financial_reset_YYYYMMDD_HHMMSS.sql
Example:  .\database_backups\backup_before_financial_reset_20260807_143000.sql


STEP 2: Drop the current database
----------------------------------
Open MySQL command line:
  mysql -u root -p

Then run:
  DROP DATABASE IF EXISTS sacco_db;
  CREATE DATABASE sacco_db;
  exit;


STEP 3: Restore from backup
----------------------------
Run this command from PowerShell (in the project directory):

  mysql -u root -p sacco_db < .\database_backups\backup_before_financial_reset_YYYYMMDD_HHMMSS.sql

Replace YYYYMMDD_HHMMSS with your actual backup timestamp.


STEP 4: Verify restoration
---------------------------
Open MySQL and verify:

  mysql -u root -p
  USE sacco_db;
  
  -- Check record counts
  SELECT COUNT(*) FROM loans;
  SELECT COUNT(*) FROM loan_repayments;
  SELECT COUNT(*) FROM transactions;
  
  -- Check financial data
  SELECT SUM(amount) FROM loans;
  SELECT SUM(outstanding_balance) FROM loans;


QUICK ROLLBACK POWERSHELL SCRIPT
=================================

Copy and paste this into PowerShell (update the backup filename):

*/

-- Save this as ROLLBACK.ps1 if you want to run it:

/*
# ROLLBACK.ps1
# Quick rollback script

$BACKUP_FILE = ".\database_backups\backup_before_financial_reset_YYYYMMDD_HHMMSS.sql"
$MYSQL_USER = "root"
$DATABASE = "sacco_db"

Write-Host "================================================" -ForegroundColor Red
Write-Host "  EMERGENCY ROLLBACK" -ForegroundColor Red
Write-Host "================================================" -ForegroundColor Red
Write-Host ""
Write-Host "⚠️  This will restore the database to its previous state" -ForegroundColor Yellow
Write-Host ""
Write-Host "Backup file: $BACKUP_FILE" -ForegroundColor White
Write-Host ""
$confirm = Read-Host "Type 'ROLLBACK' to continue"

if ($confirm -ne "ROLLBACK") {
    Write-Host "Cancelled." -ForegroundColor Yellow
    exit 0
}

Write-Host ""
Write-Host "Dropping current database..." -ForegroundColor Yellow
mysql -u $MYSQL_USER -e "DROP DATABASE IF EXISTS $DATABASE;"
mysql -u $MYSQL_USER -e "CREATE DATABASE $DATABASE;"

Write-Host "Restoring from backup..." -ForegroundColor Yellow
mysql -u $MYSQL_USER $DATABASE < $BACKUP_FILE

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ ROLLBACK SUCCESSFUL!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Database has been restored to previous state." -ForegroundColor Green
    Write-Host "Verify the restoration before proceeding." -ForegroundColor Yellow
} else {
    Write-Host ""
    Write-Host "❌ ROLLBACK FAILED!" -ForegroundColor Red
    Write-Host "Check the error messages above." -ForegroundColor Red
}
*/

-- ========================================
-- ALTERNATIVE: Selective rollback queries
-- ========================================

-- If you only need to rollback specific data (not recommended),
-- you would need to:

-- 1. Re-import specific tables from backup
-- 2. Use mysqldump with --no-create-info to get just the data
-- 3. Filter specific tables

-- Example (advanced users only):
/*
  mysqldump -u root -p --no-create-info sacco_db loan_repayments > loan_repayments_backup.sql
  mysql -u root -p sacco_db < loan_repayments_backup.sql
*/

-- ========================================
-- CONTACT FOR HELP
-- ========================================

/*
If you encounter issues during rollback:

1. Do NOT panic
2. Do NOT run any more scripts
3. Keep the backup file safe
4. Contact the database administrator
5. Share this information:
   - What script you were running
   - Any error messages
   - Backup file location
   - Current database state

The backup file contains a complete snapshot of your data
and can always be restored.
*/
