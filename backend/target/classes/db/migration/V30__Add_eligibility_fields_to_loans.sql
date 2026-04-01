-- Add eligibility tracking fields to loans table
-- This allows storing eligibility validation results for both individual and bulk loans

-- Member eligibility fields
ALTER TABLE loans ADD COLUMN IF NOT EXISTS member_eligibility_status VARCHAR(20);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS member_eligibility_errors TEXT;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS member_eligibility_warnings TEXT;

-- Guarantor 1 eligibility fields
ALTER TABLE loans ADD COLUMN IF NOT EXISTS guarantor1_eligibility_status VARCHAR(20);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS guarantor1_eligibility_errors TEXT;

-- Guarantor 2 eligibility fields
ALTER TABLE loans ADD COLUMN IF NOT EXISTS guarantor2_eligibility_status VARCHAR(20);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS guarantor2_eligibility_errors TEXT;

-- Guarantor 3 eligibility fields
ALTER TABLE loans ADD COLUMN IF NOT EXISTS guarantor3_eligibility_status VARCHAR(20);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS guarantor3_eligibility_errors TEXT;
