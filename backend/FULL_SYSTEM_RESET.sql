-- ============================================================================
-- FULL_SYSTEM_RESET.sql
-- ============================================================================
-- Purpose: Complete system reset keeping only admin user
-- Use Case: Full system reset for fresh testing from scratch
-- Keeps: Admin user only
-- Deletes: All members, staff users, loans, transactions, and all data
-- WARNING: This is destructive - use with caution!
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- Delete all audit logs
DELETE FROM audit_log;

-- Delete all notifications
DELETE FROM notification;

-- Delete all member-related data
DELETE FROM deposit_request;
DELETE FROM bulk_disbursement_item;
DELETE FROM bulk_batch;
DELETE FROM loan_repayment;
DELETE FROM guarantor;
DELETE FROM loan;
DELETE FROM transaction;
DELETE FROM account;
DELETE FROM kyc_document;
DELETE FROM member_contribution;
DELETE FROM member;

-- Delete all users except admin
DELETE FROM user WHERE username != 'admin';

-- Reset auto-increment counters
ALTER TABLE user AUTO_INCREMENT = 2;
ALTER TABLE member AUTO_INCREMENT = 1;
ALTER TABLE account AUTO_INCREMENT = 1;
ALTER TABLE loan AUTO_INCREMENT = 1;
ALTER TABLE transaction AUTO_INCREMENT = 1;
ALTER TABLE audit_log AUTO_INCREMENT = 1;

SET FOREIGN_KEY_CHECKS = 1;

-- Verify reset
SELECT 'Users' as table_name, COUNT(*) as remaining_rows FROM user
UNION ALL
SELECT 'Members', COUNT(*) FROM member
UNION ALL
SELECT 'Accounts', COUNT(*) FROM account
UNION ALL
SELECT 'Loans', COUNT(*) FROM loan
UNION ALL
SELECT 'Transactions', COUNT(*) FROM transaction
UNION ALL
SELECT 'Loan Products', COUNT(*) FROM loan_product
UNION ALL
SELECT 'Fund Config', COUNT(*) FROM fund_configuration
UNION ALL
SELECT 'Audit Logs', COUNT(*) FROM audit_log;

-- Show remaining admin user
SELECT 'Admin User Details:' as info;
SELECT id, username, role, created_at FROM user WHERE username = 'admin';

-- ============================================================================
-- Full System Reset Complete
-- ============================================================================
-- Only admin user remains
-- All member and transaction data deleted
-- System ready for fresh initialization
-- ============================================================================
