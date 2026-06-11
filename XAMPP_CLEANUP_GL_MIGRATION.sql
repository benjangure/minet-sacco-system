-- ============================================================
-- XAMPP CLEANUP SCRIPT FOR GL MIGRATION FIX
-- Run this in MySQL Workbench or phpMyAdmin
-- ============================================================
-- This script removes the failed GL migrations and cleans up 
-- related tables so the corrected migrations can run fresh

USE sacco_db;

-- Step 1: Drop GL tables in reverse dependency order
DROP TABLE IF EXISTS gl_account_audit;
DROP TABLE IF EXISTS gl_manual_entries;
DROP TABLE IF EXISTS gl_account_calculations;
DROP TABLE IF EXISTS gl_accounts;

-- Step 2: Mark V116 and V117 migrations as failed in Flyway history
-- This tells Flyway to re-run the migrations on next startup
DELETE FROM flyway_schema_history 
WHERE version IN ('116', '117') 
  AND description IN ('Create GL Tables', 'Populate GL Accounts');

-- Step 3: Verify cleanup (optional - shows what remains)
SELECT * FROM flyway_schema_history 
WHERE version >= '115' 
ORDER BY version DESC;

-- ============================================================
-- After running this script:
-- 1. Restart the Spring Boot backend application
-- 2. The corrected V116 and V117 migrations will run automatically
-- 3. Check application logs for successful migration
-- ============================================================
