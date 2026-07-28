-- Add third guarantor support to bulk_loan_items
-- Allows up to 3 guarantors per loan (max 3)
ALTER TABLE bulk_loan_items ADD COLUMN guarantor3 VARCHAR(50);
ALTER TABLE bulk_loan_items ADD COLUMN guarantor3_eligibility_status VARCHAR(20);
