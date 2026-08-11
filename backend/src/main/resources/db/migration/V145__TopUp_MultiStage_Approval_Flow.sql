-- V145__TopUp_MultiStage_Approval_Flow.sql
-- Migrates the loan_topup_requests table to support a full multi-stage
-- approval pipeline that mirrors the loan approval flow:
--   PENDING_GUARANTOR_APPROVAL → PENDING_LOAN_OFFICER_REVIEW
--   → PENDING_CREDIT_COMMITTEE → PENDING_TREASURER → APPROVED → DISBURSED
--
-- Changes:
--  1. Rename legacy PENDING_REVIEW rows to PENDING_LOAN_OFFICER_REVIEW
--  2. Widen the status column to hold the new longer enum values
--  3. Add reviewed_at / approved_at timestamps for audit trail

-- Step 1: Widen the column to fit the longest new value
-- (MySQL/MariaDB VARCHAR, safe to enlarge)
ALTER TABLE loan_topup_requests
    MODIFY COLUMN status VARCHAR(60) NOT NULL DEFAULT 'PENDING_GUARANTOR_APPROVAL';

-- Step 2: Rename any existing PENDING_REVIEW rows to the new canonical value
UPDATE loan_topup_requests
SET    status = 'PENDING_LOAN_OFFICER_REVIEW'
WHERE  status = 'PENDING_REVIEW';

-- Step 3: Add an approved_at column (records when treasurer final-approves)
ALTER TABLE loan_topup_requests
    ADD COLUMN approved_at DATETIME NULL;

-- Step 4: Add a loan_officer_reviewed_at column for audit trail
ALTER TABLE loan_topup_requests
    ADD COLUMN loan_officer_reviewed_at DATETIME NULL;

-- Step 5: Add credit_committee_reviewed_at
ALTER TABLE loan_topup_requests
    ADD COLUMN credit_committee_reviewed_at DATETIME NULL;

-- Step 6: Add treasurer_reviewed_at
ALTER TABLE loan_topup_requests
    ADD COLUMN treasurer_reviewed_at DATETIME NULL;

-- Step 7: Add rejection_stage to record which stage caused a rejection
ALTER TABLE loan_topup_requests
    ADD COLUMN rejection_stage VARCHAR(60) NULL;
