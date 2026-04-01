-- Add calculation fields to bulk_loan_items
ALTER TABLE bulk_loan_items ADD COLUMN IF NOT EXISTS total_interest DECIMAL(19,2);
ALTER TABLE bulk_loan_items ADD COLUMN IF NOT EXISTS total_repayable DECIMAL(19,2);
ALTER TABLE bulk_loan_items ADD COLUMN IF NOT EXISTS monthly_repayment DECIMAL(19,2);

-- Add eligibility status fields to bulk_loan_items
ALTER TABLE bulk_loan_items ADD COLUMN IF NOT EXISTS guarantor1_eligibility_status VARCHAR(20);
ALTER TABLE bulk_loan_items ADD COLUMN IF NOT EXISTS guarantor2_eligibility_status VARCHAR(20);
