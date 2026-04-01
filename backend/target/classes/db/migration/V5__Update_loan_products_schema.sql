-- Add missing columns to loan_products table
ALTER TABLE loan_products 
    ADD COLUMN min_term_months INT DEFAULT 1,
    ADD COLUMN max_term_months INT DEFAULT 12,
    ADD COLUMN is_active BOOLEAN DEFAULT TRUE;

-- Update existing records to use term_months as max_term_months
UPDATE loan_products SET max_term_months = term_months WHERE term_months IS NOT NULL;

-- Drop the old term_months column
ALTER TABLE loan_products DROP COLUMN term_months;

-- Update existing loan products with reasonable defaults
UPDATE loan_products SET min_term_months = 3 WHERE min_term_months IS NULL;
UPDATE loan_products SET max_term_months = 12 WHERE max_term_months IS NULL;
UPDATE loan_products SET is_active = TRUE WHERE is_active IS NULL;
