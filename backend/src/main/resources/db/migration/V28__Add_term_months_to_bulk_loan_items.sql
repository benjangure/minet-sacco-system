-- Add term_months column to bulk_loan_items
-- This allows the Excel template to specify the loan term for each item
ALTER TABLE bulk_loan_items ADD COLUMN IF NOT EXISTS term_months INT;
