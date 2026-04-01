-- Add payment_method and description columns to loan_repayments table
ALTER TABLE loan_repayments ADD COLUMN payment_method VARCHAR(50);
ALTER TABLE loan_repayments ADD COLUMN description TEXT;
