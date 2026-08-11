-- ============================================
-- Rename Database: tminet → sacco_dbprod
-- ============================================
-- Run this in MySQL Workbench on your LOCAL machine

-- Step 1: Create new database
CREATE DATABASE IF NOT EXISTS sacco_dbprod CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Step 2: Rename all tables (49 tables)
RENAME TABLE tminet.accounts TO sacco_dbprod.accounts;
RENAME TABLE tminet.audit_logs TO sacco_dbprod.audit_logs;
RENAME TABLE tminet.batch_deletion_audit TO sacco_dbprod.batch_deletion_audit;
RENAME TABLE tminet.bulk_batches TO sacco_dbprod.bulk_batches;
RENAME TABLE tminet.bulk_disbursement_items TO sacco_dbprod.bulk_disbursement_items;
RENAME TABLE tminet.bulk_loan_data_update_items TO sacco_dbprod.bulk_loan_data_update_items;
RENAME TABLE tminet.bulk_loan_items TO sacco_dbprod.bulk_loan_items;
RENAME TABLE tminet.bulk_member_items TO sacco_dbprod.bulk_member_items;
RENAME TABLE tminet.bulk_transaction_items TO sacco_dbprod.bulk_transaction_items;
RENAME TABLE tminet.deposit_requests TO sacco_dbprod.deposit_requests;
RENAME TABLE tminet.flyway_schema_history TO sacco_dbprod.flyway_schema_history;
RENAME TABLE tminet.fund_configurations TO sacco_dbprod.fund_configurations;
RENAME TABLE tminet.gl_account_audit TO sacco_dbprod.gl_account_audit;
RENAME TABLE tminet.gl_account_calculations TO sacco_dbprod.gl_account_calculations;
RENAME TABLE tminet.gl_accounts TO sacco_dbprod.gl_accounts;
RENAME TABLE tminet.gl_manual_entries TO sacco_dbprod.gl_manual_entries;
RENAME TABLE tminet.guarantor_default_tracking TO sacco_dbprod.guarantor_default_tracking;
RENAME TABLE tminet.guarantor_repayment_tracking TO sacco_dbprod.guarantor_repayment_tracking;
RENAME TABLE tminet.guarantors TO sacco_dbprod.guarantors;
RENAME TABLE tminet.hr_loan_decisions TO sacco_dbprod.hr_loan_decisions;
RENAME TABLE tminet.kyc_document_audit TO sacco_dbprod.kyc_document_audit;
RENAME TABLE tminet.kyc_documents TO sacco_dbprod.kyc_documents;
RENAME TABLE tminet.loan_eligibility_rules TO sacco_dbprod.loan_eligibility_rules;
RENAME TABLE tminet.loan_migration_items TO sacco_dbprod.loan_migration_items;
RENAME TABLE tminet.loan_migration_snapshots TO sacco_dbprod.loan_migration_snapshots;
RENAME TABLE tminet.loan_products TO sacco_dbprod.loan_products;
RENAME TABLE tminet.loan_repayment_requests TO sacco_dbprod.loan_repayment_requests;
RENAME TABLE tminet.loan_repayments TO sacco_dbprod.loan_repayments;
RENAME TABLE tminet.loan_topup_history TO sacco_dbprod.loan_topup_history;
RENAME TABLE tminet.loan_topup_requests TO sacco_dbprod.loan_topup_requests;
RENAME TABLE tminet.loans TO sacco_dbprod.loans;
RENAME TABLE tminet.login_history TO sacco_dbprod.login_history;
RENAME TABLE tminet.member_credentials TO sacco_dbprod.member_credentials;
RENAME TABLE tminet.member_exits TO sacco_dbprod.member_exits;
RENAME TABLE tminet.member_reactivations TO sacco_dbprod.member_reactivations;
RENAME TABLE tminet.member_suspensions TO sacco_dbprod.member_suspensions;
RENAME TABLE tminet.members TO sacco_dbprod.members;
RENAME TABLE tminet.members_legacy_view TO sacco_dbprod.members_legacy_view;
RENAME TABLE tminet.migration_batches TO sacco_dbprod.migration_batches;
RENAME TABLE tminet.notifications TO sacco_dbprod.notifications;
RENAME TABLE tminet.push_subscriptions TO sacco_dbprod.push_subscriptions;
RENAME TABLE tminet.support_tickets TO sacco_dbprod.support_tickets;
RENAME TABLE tminet.system_settings TO sacco_dbprod.system_settings;
RENAME TABLE tminet.topup_guarantors TO sacco_dbprod.topup_guarantors;
RENAME TABLE tminet.transactions TO sacco_dbprod.transactions;
RENAME TABLE tminet.user_activity_logs TO sacco_dbprod.user_activity_logs;
RENAME TABLE tminet.user_deletion_requests TO sacco_dbprod.user_deletion_requests;
RENAME TABLE tminet.user_devices TO sacco_dbprod.user_devices;
RENAME TABLE tminet.users TO sacco_dbprod.users;

-- Step 3: Verify all tables were moved
SELECT COUNT(*) AS remaining_tables FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tminet';
-- Should return 0

SELECT COUNT(*) AS new_db_tables FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'sacco_dbprod';
-- Should return 49

-- Step 4: Drop old database (only if count is 0 above)
DROP DATABASE tminet;

-- Step 5: Create user for new database (if needed)
CREATE USER IF NOT EXISTS 'sacco_admin'@'localhost' IDENTIFIED BY '0a0b0c0D.';
GRANT ALL PRIVILEGES ON sacco_dbprod.* TO 'sacco_admin'@'localhost';
FLUSH PRIVILEGES;

-- Done! Database renamed successfully
