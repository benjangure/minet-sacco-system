-- ============================================================================
-- CLEAN_MEMBERS_ONLY.sql
-- ============================================================================
-- Purpose: Delete all member data while keeping staff users and system config
-- Use Case: E2E testing with fresh member data
-- Keeps: Staff users, loan products, fund config, eligibility rules
-- Deletes: All members and their related data
-- ============================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- Delete member-related data
DELETE FROM audit_log WHERE member_id IS NOT NULL;
DELETE FROM notification WHERE user_id IN (SELECT id FROM user WHERE member_id IS NOT NULL);
DELETE FROM deposit_request;
DELETE FROM bulk_disbursement_item;
DELETE FROM bulk_batch;
DELETE FROM loan_repayment;
DELETE FROM guarantor;
DELETE FROM loan;
DELETE FROM transaction WHERE account_id IN (SELECT id FROM account WHERE member_id IS NOT NULL);
DELETE FROM account WHERE member_id IS NOT NULL;
DELETE FROM kyc_document;
DELETE FROM member_contribution;
DELETE FROM member;

-- Delete member-related users
DELETE FROM user WHERE member_id IS NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;

-- Verify cleanup
SELECT 'Members' as table_name, COUNT(*) as remaining_rows FROM member
UNION ALL
SELECT 'Accounts', COUNT(*) FROM account
UNION ALL
SELECT 'Loans', COUNT(*) FROM loan
UNION ALL
SELECT 'Transactions', COUNT(*) FROM transaction
UNION ALL
SELECT 'Staff Users', COUNT(*) FROM user WHERE member_id IS NULL
UNION ALL
SELECT 'Loan Products', COUNT(*) FROM loan_product
UNION ALL
SELECT 'Fund Config', COUNT(*) FROM fund_configuration;

-- ============================================================================
-- Cleanup Complete
-- ============================================================================
-- All member data has been deleted
-- Staff users and system configuration remain intact
-- Ready for fresh E2E testing
-- ============================================================================
