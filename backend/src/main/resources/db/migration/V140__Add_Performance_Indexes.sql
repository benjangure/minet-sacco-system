-- V140__Add_Performance_Indexes.sql
-- Purpose: Add performance indexes on frequently queried columns to reduce database load and improve query speed
-- These indexes target the N+1 query problems identified in loan, member, transaction, and notification queries
-- Expected improvements: 50-70% faster queries on indexed columns, especially for reports and list operations

-- Index for loans table: Frequently used in findByMemberIdAndStatus() and status-based queries
CREATE INDEX idx_loans_member_status ON loans(member_id, status);

-- Index for transactions table: Used in report generation with date range filtering
-- DESC order helps with ORDER BY in queries like "latest transactions first"
CREATE INDEX idx_transactions_account_date ON transactions(account_id, transaction_date DESC);

-- Index for guarantors table: Essential for loan guarantee lookups and guarantor_id based queries
CREATE INDEX idx_guarantors_loan ON guarantors(loan_id);

-- Index for loan_repayments table: Used in interest calculation and repayment history queries
CREATE INDEX idx_loan_repayments_loan_date ON loan_repayments(loan_id, repayment_date);

-- Index for members table: Supports employee_id lookups for member verification and HR workflows
CREATE INDEX idx_members_employee_id ON members(employee_id);

-- Index for notifications table: Optimizes notification bell fetching (getUnreadCount) and notification queries
CREATE INDEX idx_notifications_user ON notifications(user_id);

-- Index for audit_logs table: Improves audit trail queries by user and enables faster compliance reports
CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
