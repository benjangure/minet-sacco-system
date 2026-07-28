-- Add calculation fields to bulk_loan_items
ALTER TABLE bulk_loan_items ADD COLUMN total_interest DECIMAL(19,2);
ALTER TABLE bulk_loan_items ADD COLUMN total_repayable DECIMAL(19,2);
ALTER TABLE bulk_loan_items ADD COLUMN monthly_repayment DECIMAL(19,2);

-- Add eligibility status fields to bulk_loan_items
ALTER TABLE bulk_loan_items ADD COLUMN guarantor1_eligibility_status VARCHAR(20);
ALTER TABLE bulk_loan_items ADD COLUMN guarantor2_eligibility_status VARCHAR(20);
