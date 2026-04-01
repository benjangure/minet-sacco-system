-- Make loanNumber nullable to allow assignment only on disbursement
ALTER TABLE loans MODIFY COLUMN loan_number VARCHAR(50) NULL;

-- Create index on disbursement_date for performance
CREATE INDEX IF NOT EXISTS idx_loans_disbursement_date ON loans(disbursement_date);

-- Create index on status for faster queries
CREATE INDEX IF NOT EXISTS idx_loans_status ON loans(status);
