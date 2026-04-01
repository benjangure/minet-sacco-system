-- V18__Clean_test_data.sql
-- Remove all test member data to prepare for fresh testing
-- IMPORTANT: Only deletes test members and related data, preserves system users

-- Disable foreign key checks temporarily
SET FOREIGN_KEY_CHECKS = 0;

-- Delete ALL members and their related data (keeping system users intact)
DELETE FROM kyc_document_audit;
DELETE FROM kyc_documents;
DELETE FROM loan_repayments;
DELETE FROM loans;
DELETE FROM transactions;
DELETE FROM accounts;
DELETE FROM members;

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;
