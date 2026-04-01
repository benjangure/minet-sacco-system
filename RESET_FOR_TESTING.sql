-- RESET_FOR_TESTING.sql
-- Clears all loans, guarantors, transactions, and resets member account balances to zero
-- Keeps all members, users, and loan products intact for fresh testing

SET FOREIGN_KEY_CHECKS = 0;

-- Delete all loan repayments
DELETE FROM loan_repayments;

-- Delete all guarantors
DELETE FROM guarantors;

-- Delete all loans
DELETE FROM loans;

-- Delete all transactions
DELETE FROM transactions;

-- Delete all bulk items (related to loans/transactions)
DELETE FROM bulk_disbursement_items;
DELETE FROM bulk_loan_items;
DELETE FROM bulk_transaction_items;
DELETE FROM bulk_member_items;

-- Delete all bulk batches
DELETE FROM bulk_batches;

-- Reset all account balances to zero
UPDATE accounts SET balance = 0;

-- Reset member status to ACTIVE (in case any were suspended)
UPDATE members SET status = 'ACTIVE' WHERE status IN ('SUSPENDED', 'INACTIVE');

-- Clear audit logs
DELETE FROM audit_logs;

-- Clear user activity logs
DELETE FROM user_activity_logs;

-- Clear notifications
DELETE FROM notifications;

-- Clear KYC document audits
DELETE FROM kyc_document_audits;

-- Clear KYC documents
DELETE FROM kyc_documents;

SET FOREIGN_KEY_CHECKS = 1;

-- Verify the reset
SELECT 'Members' as entity, COUNT(*) as count FROM members
UNION ALL
SELECT 'Accounts', COUNT(*) FROM accounts
UNION ALL
SELECT 'Loans', COUNT(*) FROM loans
UNION ALL
SELECT 'Guarantors', COUNT(*) FROM guarantors
UNION ALL
SELECT 'Transactions', COUNT(*) FROM transactions;
