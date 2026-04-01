-- V50__Add_guarantor_approval_fields.sql
-- Add rejection reason and approval timestamp to guarantors table

ALTER TABLE guarantors ADD COLUMN rejection_reason VARCHAR(500) NULL;
ALTER TABLE guarantors ADD COLUMN approved_at DATETIME NULL;

-- Create index for faster queries on guarantor status
CREATE INDEX idx_guarantor_status ON guarantors(status);
CREATE INDEX idx_guarantor_loan_status ON guarantors(loan_id, status);
