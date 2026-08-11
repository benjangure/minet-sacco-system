-- =====================================================
-- Add ONLY Missing Performance Indexes
-- =====================================================
-- This adds the 2 missing critical indexes identified by CHECK_INDEXES.sql
-- Run this in MySQL Workbench
-- =====================================================

USE tminet;

-- Missing index 1: transactions table (account_id lookups)
-- Used heavily in treasurer dashboard and reports
CREATE INDEX idx_transactions_account_id ON transactions(account_id);

-- Missing index 2: guarantors table (loan_id lookups)
-- Used when displaying loan guarantors
CREATE INDEX idx_guarantors_loan_id ON guarantors(loan_id);

-- Verify the indexes were created
SELECT 'Indexes added successfully. Run CHECK_INDEXES.sql to verify.' AS Status;
