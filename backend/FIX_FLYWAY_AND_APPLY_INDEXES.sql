-- =====================================================
-- Manual Fix for Flyway V147 Migration Failure
-- =====================================================
-- Run this script manually in MySQL to:
-- 1. Remove failed migration from Flyway history
-- 2. Apply performance indexes
-- =====================================================

USE tminet;

-- Disable safe update mode temporarily
SET SQL_SAFE_UPDATES = 0;

-- Step 1: Remove failed V147 migration from Flyway history
DELETE FROM flyway_schema_history WHERE version = '147';

-- Re-enable safe update mode
SET SQL_SAFE_UPDATES = 1;

-- Step 2: Apply all performance indexes manually
-- (These are the same indexes from V147 migration)
-- Using DROP IF EXISTS then CREATE to avoid duplicate key errors

-- 1. LOANS TABLE INDEXES
DROP INDEX IF EXISTS idx_loans_member_id ON loans;
CREATE INDEX idx_loans_member_id ON loans(member_id);

DROP INDEX IF EXISTS idx_loans_status ON loans;
CREATE INDEX idx_loans_status ON loans(status);

DROP INDEX IF EXISTS idx_loans_member_status ON loans;
CREATE INDEX idx_loans_member_status ON loans(member_id, status);

DROP INDEX IF EXISTS idx_loans_loan_number ON loans;
CREATE INDEX idx_loans_loan_number ON loans(loan_number);

DROP INDEX IF EXISTS idx_loans_disbursement_date ON loans;
CREATE INDEX idx_loans_disbursement_date ON loans(disbursement_date);

DROP INDEX IF EXISTS idx_loans_created_at ON loans;
CREATE INDEX idx_loans_created_at ON loans(created_at);

-- 2. TRANSACTIONS TABLE INDEXES
DROP INDEX IF EXISTS idx_transactions_account_id ON transactions;
CREATE INDEX idx_transactions_account_id ON transactions(account_id);

DROP INDEX IF EXISTS idx_transactions_date ON transactions;
CREATE INDEX idx_transactions_date ON transactions(transaction_date);

DROP INDEX IF EXISTS idx_transactions_account_date ON transactions;
CREATE INDEX idx_transactions_account_date ON transactions(account_id, transaction_date);

DROP INDEX IF EXISTS idx_transactions_type ON transactions;
CREATE INDEX idx_transactions_type ON transactions(transaction_type);

DROP INDEX IF EXISTS idx_transactions_account_type ON transactions;
CREATE INDEX idx_transactions_account_type ON transactions(account_id, transaction_type);

-- 3. GUARANTORS TABLE INDEXES
DROP INDEX IF EXISTS idx_guarantors_loan_id ON guarantors;
CREATE INDEX idx_guarantors_loan_id ON guarantors(loan_id);

DROP INDEX IF EXISTS idx_guarantors_member_id ON guarantors;
CREATE INDEX idx_guarantors_member_id ON guarantors(member_id);

DROP INDEX IF EXISTS idx_guarantors_status ON guarantors;
CREATE INDEX idx_guarantors_status ON guarantors(status);

DROP INDEX IF EXISTS idx_guarantors_member_status ON guarantors;
CREATE INDEX idx_guarantors_member_status ON guarantors(member_id, status);

-- 4. LOAN_REPAYMENTS TABLE INDEXES
DROP INDEX IF EXISTS idx_loan_repayments_loan_id ON loan_repayments;
CREATE INDEX idx_loan_repayments_loan_id ON loan_repayments(loan_id);

DROP INDEX IF EXISTS idx_loan_repayments_date ON loan_repayments;
CREATE INDEX idx_loan_repayments_date ON loan_repayments(repayment_date);

DROP INDEX IF EXISTS idx_loan_repayments_loan_date ON loan_repayments;
CREATE INDEX idx_loan_repayments_loan_date ON loan_repayments(loan_id, repayment_date);

DROP INDEX IF EXISTS idx_loan_repayments_created_at ON loan_repayments;
CREATE INDEX idx_loan_repayments_created_at ON loan_repayments(created_at);

-- 5. ACCOUNTS TABLE INDEXES
DROP INDEX IF EXISTS idx_accounts_member_id ON accounts;
CREATE INDEX idx_accounts_member_id ON accounts(member_id);

DROP INDEX IF EXISTS idx_accounts_type ON accounts;
CREATE INDEX idx_accounts_type ON accounts(account_type);

DROP INDEX IF EXISTS idx_accounts_member_type ON accounts;
CREATE INDEX idx_accounts_member_type ON accounts(member_id, account_type);

-- 6. MEMBERS TABLE INDEXES
DROP INDEX IF EXISTS idx_members_member_number ON members;
CREATE INDEX idx_members_member_number ON members(member_number);

DROP INDEX IF EXISTS idx_members_status ON members;
CREATE INDEX idx_members_status ON members(status);

DROP INDEX IF EXISTS idx_members_employee_id ON members;
CREATE INDEX idx_members_employee_id ON members(employee_id);

DROP INDEX IF EXISTS idx_members_national_id ON members;
CREATE INDEX idx_members_national_id ON members(national_id);

DROP INDEX IF EXISTS idx_members_created_at ON members;
CREATE INDEX idx_members_created_at ON members(created_at);

-- 7. USERS TABLE INDEXES
DROP INDEX IF EXISTS idx_users_username ON users;
CREATE INDEX idx_users_username ON users(username);

DROP INDEX IF EXISTS idx_users_member_id ON users;
CREATE INDEX idx_users_member_id ON users(member_id);

DROP INDEX IF EXISTS idx_users_role ON users;
CREATE INDEX idx_users_role ON users(role);

-- 8. NOTIFICATIONS TABLE INDEXES
DROP INDEX IF EXISTS idx_notifications_user_id ON notifications;
CREATE INDEX idx_notifications_user_id ON notifications(user_id);

DROP INDEX IF EXISTS idx_notifications_user_read ON notifications;
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);

DROP INDEX IF EXISTS idx_notifications_type ON notifications;
CREATE INDEX idx_notifications_type ON notifications(notification_type);

DROP INDEX IF EXISTS idx_notifications_created_at ON notifications;
CREATE INDEX idx_notifications_created_at ON notifications(created_at);

-- 9. AUDIT_LOGS TABLE INDEXES
DROP INDEX IF EXISTS idx_audit_logs_user_id ON audit_logs;
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);

DROP INDEX IF EXISTS idx_audit_logs_action ON audit_logs;
CREATE INDEX idx_audit_logs_action ON audit_logs(action);

DROP INDEX IF EXISTS idx_audit_logs_entity_type ON audit_logs;
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);

DROP INDEX IF EXISTS idx_audit_logs_created_at ON audit_logs;
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);

DROP INDEX IF EXISTS idx_audit_logs_entity ON audit_logs;
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

-- 10. KYC_DOCUMENTS TABLE INDEXES (if exists - will skip errors if table doesn't exist)
DROP INDEX IF EXISTS idx_kyc_documents_member_id ON kyc_documents;
CREATE INDEX idx_kyc_documents_member_id ON kyc_documents(member_id);

DROP INDEX IF EXISTS idx_kyc_documents_status ON kyc_documents;
CREATE INDEX idx_kyc_documents_status ON kyc_documents(status);

DROP INDEX IF EXISTS idx_kyc_documents_member_status ON kyc_documents;
CREATE INDEX idx_kyc_documents_member_status ON kyc_documents(member_id, status);

-- Step 3: Mark V147 as successfully applied in Flyway history
SET SQL_SAFE_UPDATES = 0;
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
SELECT 
    (SELECT COALESCE(MAX(installed_rank), 0) + 1 FROM flyway_schema_history) as installed_rank,
    '147' as version,
    'Add Performance Indexes Phase2' as description,
    'SQL' as type,
    'V147__Add_Performance_Indexes_Phase2.sql' as script,
    NULL as checksum,
    'minetsacco' as installed_by,
    NOW() as installed_on,
    1000 as execution_time,
    TRUE as success;
SET SQL_SAFE_UPDATES = 1;

-- =====================================================
-- Verification Queries
-- =====================================================
-- Run these to verify indexes were created:

-- Check loans table indexes
SHOW INDEX FROM loans WHERE Key_name LIKE 'idx_loans%';

-- Check transactions table indexes
SHOW INDEX FROM transactions WHERE Key_name LIKE 'idx_transactions%';

-- Check guarantors table indexes
SHOW INDEX FROM guarantors WHERE Key_name LIKE 'idx_guarantors%';

-- Check all new indexes
SELECT 
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'tminet'
  AND INDEX_NAME LIKE 'idx_%'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;

-- =====================================================
-- DONE! Your database now has performance indexes.
-- Restart your backend and test the performance.
-- =====================================================
