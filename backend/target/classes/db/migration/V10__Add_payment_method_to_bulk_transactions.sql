-- Add payment method and reference number columns to bulk_transaction_items table
-- This enhances bulk loan repayments to support proper payment method tracking

ALTER TABLE bulk_transaction_items 
ADD COLUMN loan_repayment_payment_method VARCHAR(20) DEFAULT 'SALARY_DEDUCTION' COMMENT 'Payment method for loan repayment (SALARY_DEDUCTION, CASH, MPESA, BANK_TRANSFER, CHEQUE, OTHER)';

ALTER TABLE bulk_transaction_items 
ADD COLUMN loan_repayment_reference_number VARCHAR(50) NULL COMMENT 'Reference number for loan repayment (payroll ref, cheque number, mpesa receipt, etc.)';

-- Add indexes for performance if needed
CREATE INDEX idx_bulk_transaction_items_payment_method ON bulk_transaction_items(loan_repayment_payment_method);
