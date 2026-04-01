-- Add missing calculated fields and loan number to loans table
ALTER TABLE loans 
    ADD COLUMN loan_number VARCHAR(50) UNIQUE,
    ADD COLUMN monthly_repayment DECIMAL(15,2),
    ADD COLUMN total_interest DECIMAL(15,2),
    ADD COLUMN total_repayable DECIMAL(15,2),
    ADD COLUMN outstanding_balance DECIMAL(15,2),
    ADD COLUMN purpose TEXT,
    ADD COLUMN rejection_reason TEXT;

-- Create index on loan_number for faster lookups
CREATE INDEX idx_loan_number ON loans(loan_number);
