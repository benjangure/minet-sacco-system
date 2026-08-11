-- =====================================================
-- SIMPLE VERSION: Apply Performance Indexes
-- =====================================================
-- Instructions:
-- 1. Run this in phpMyAdmin SQL tab
-- 2. Ignore "Duplicate key name" errors - they're OK!
-- 3. As long as you see some "Query OK" messages, it's working
-- =====================================================

USE tminet;

-- 1. LOANS TABLE INDEXES
CREATE INDEX idx_loans_member_id ON loans(member_id);
CREATE INDEX idx_loans_status ON loans(status);
CREATE INDEX idx_loans_member_status ON loans(member_id, status);
CREATE INDEX idx_loans_loan_number ON loans(loan_number);
CREATE INDEX idx_loans_disbursement_date ON loans(disbursement_date);
CREATE INDEX idx_loans_created_at ON loans(created_at);

-- 2. TRANSACTIONS TABLE INDEXES
CREATE INDEX idx_transactions_account_id ON transactions(account_id);
CREATE INDEX idx_transactions_date ON transactions(transaction_date);
CREATE INDEX idx_transactions_account_date ON transactions(account_id, transaction_date);
CREATE INDEX idx_transactions_type ON transactions(transaction_type);
CREATE INDEX idx_transactions_account_type ON transactions(account_id, transaction_type);

-- 3. GUARANTORS TABLE INDEXES
CREATE INDEX idx_guarantors_loan_id ON guarantors(loan_id);
CREATE INDEX idx_guarantors_member_id ON guarantors(member_id);
CREATE INDEX idx_guarantors_status ON guarantors(status);
CREATE INDEX idx_guarantors_member_status ON guarantors(member_id, status);

-- 4. LOAN_REPAYMENTS TABLE INDEXES
CREATE INDEX idx_loan_repayments_loan_id ON loan_repayments(loan_id);
CREATE INDEX idx_loan_repayments_date ON loan_repayments(repayment_date);
CREATE INDEX idx_loan_repayments_loan_date ON loan_repayments(loan_id, repayment_date);
CREATE INDEX idx_loan_repayments_created_at ON loan_repayments(created_at);

-- 5. ACCOUNTS TABLE INDEXES
CREATE INDEX idx_accounts_member_id ON accounts(member_id);
CREATE INDEX idx_accounts_type ON accounts(account_type);
CREATE INDEX idx_accounts_member_type ON accounts(member_id, account_type);

-- 6. MEMBERS TABLE INDEXES
CREATE INDEX idx_members_member_number ON members(member_number);
CREATE INDEX idx_members_status ON members(status);
CREATE INDEX idx_members_employee_id ON members(employee_id);
CREATE INDEX idx_members_national_id ON members(national_id);
CREATE INDEX idx_members_created_at ON members(created_at);

-- 7. USERS TABLE INDEXES
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_member_id ON users(member_id);
CREATE INDEX idx_users_role ON users(role);

-- 8. NOTIFICATIONS TABLE INDEXES
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_type ON notifications(notification_type);
CREATE INDEX idx_notifications_created_at ON notifications(created_at);

-- 9. AUDIT_LOGS TABLE INDEXES
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_entity_type ON audit_logs(entity_type);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);

-- 10. KYC_DOCUMENTS TABLE INDEXES
CREATE INDEX idx_kyc_documents_member_id ON kyc_documents(member_id);
CREATE INDEX idx_kyc_documents_status ON kyc_documents(status);
CREATE INDEX idx_kyc_documents_member_status ON kyc_documents(member_id, status);

-- =====================================================
-- DONE! 
-- Even if you saw "Duplicate key name" errors, 
-- the indexes that didn't exist are now created.
-- 
-- Now verify indexes were created by running this:
-- =====================================================

SELECT 
    TABLE_NAME,
    INDEX_NAME,
    GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS COLUMNS
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'tminet'
  AND INDEX_NAME LIKE 'idx_%'
GROUP BY TABLE_NAME, INDEX_NAME
ORDER BY TABLE_NAME, INDEX_NAME;
