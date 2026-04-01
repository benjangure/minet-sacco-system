-- Add disbursedBy field to Loan entity for audit trail
ALTER TABLE loans ADD COLUMN IF NOT EXISTS disbursed_by BIGINT;

-- Add foreign key constraint
ALTER TABLE loans ADD CONSTRAINT fk_loans_disbursed_by 
    FOREIGN KEY (disbursed_by) REFERENCES users(id);

-- Create index for performance
CREATE INDEX IF NOT EXISTS idx_loans_disbursed_by ON loans(disbursed_by);
