-- V81: Fix missing payment method and reference number columns in bulk_transaction_items table
-- These columns should have been added by V10 but appear to be missing

ALTER TABLE bulk_transaction_items 
ADD COLUMN IF NOT EXISTS loan_repayment_payment_method VARCHAR(20) DEFAULT 'SALARY_DEDUCTION' COMMENT 'Payment method for loan repayment (SALARY_DEDUCTION, CASH, MPESA, BANK_TRANSFER, CHEQUE, OTHER)';

ALTER TABLE bulk_transaction_items 
ADD COLUMN IF NOT EXISTS loan_repayment_reference_number VARCHAR(50) NULL COMMENT 'Reference number for loan repayment (payroll ref, cheque number, mpesa receipt, etc.)';

-- Add index for performance if it doesn't exist
CREATE INDEX IF NOT EXISTS idx_bulk_transaction_items_payment_method ON bulk_transaction_items(loan_repayment_payment_method);
