-- Add missing columns to loan_repayments table
-- These columns exist in the entity but may be missing from the database
-- if the table was created before V68 migration
ALTER TABLE loan_repayments ADD COLUMN IF NOT EXISTS payment_date DATETIME NULL;
ALTER TABLE loan_repayments ADD COLUMN IF NOT EXISTS reference_number VARCHAR(100) NULL;

-- Backfill payment_date with created_at for existing records
UPDATE loan_repayments SET payment_date = created_at WHERE payment_date IS NULL;
