-- Add new columns to loans table for tracking rejection and original amounts
ALTER TABLE loans ADD COLUMN original_amount DECIMAL(19,2);
ALTER TABLE loans ADD COLUMN rejection_stage VARCHAR(50);

-- Create indexes for faster queries on rejection handling
CREATE INDEX idx_loans_status_rejection_stage ON loans(status, rejection_stage);
CREATE INDEX idx_guarantors_status ON guarantors(status);
CREATE INDEX idx_guarantors_rejection_reason ON guarantors(rejection_reason);
