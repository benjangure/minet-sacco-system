-- Add max_total_borrowing_limit column to loan_products
-- This field enables enforcement of cumulative borrowing limits per product per member
-- Example: Emergency Loan 1 may have a max_total_borrowing_limit of 150,000
-- A member can borrow multiple times but cumulative total cannot exceed this limit

ALTER TABLE loan_products
ADD COLUMN max_total_borrowing_limit DECIMAL(19,2) DEFAULT NULL COMMENT 'Maximum cumulative amount a member can borrow on this product';

-- Add index for performance when checking outstanding balances
CREATE INDEX idx_loan_product_borrowing_limit ON loan_products(max_total_borrowing_limit);
