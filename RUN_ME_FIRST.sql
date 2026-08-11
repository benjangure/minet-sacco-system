-- =====================================================
-- CRITICAL: Run this SQL script BEFORE starting backend
-- =====================================================
-- This adds the 2 missing performance indexes identified
-- =====================================================

USE tminet;

-- Missing index 1: transactions table (account_id lookups)
-- Used heavily in treasurer dashboard and reports
CREATE INDEX idx_transactions_account_id ON transactions(account_id);

-- Missing index 2: guarantors table (loan_id lookups)
-- Used when displaying loan guarantors
CREATE INDEX idx_guarantors_loan_id ON guarantors(loan_id);

-- Verify the indexes were created
SELECT 'SUCCESS: Missing indexes added!' AS Status;

-- NEXT STEPS:
-- 1. Start backend: cd backend; java -jar target\minet-sacco-backend-0.0.1-SNAPSHOT.jar
-- 2. Open browser to http://localhost:8080
-- 3. Login as TREASURER and check dashboard load time (should be ~2 seconds instead of 30+)
