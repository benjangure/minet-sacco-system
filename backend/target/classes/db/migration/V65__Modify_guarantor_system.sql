-- Migration: Modify guarantor system to support unlimited guarantors with custom amounts
-- Purpose: Remove fixed 3-guarantor slots and support flexible guarantor amounts

-- Step 1: Create guarantor_repayment_tracking table
CREATE TABLE IF NOT EXISTS guarantor_repayment_tracking (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  guarantor_id BIGINT NOT NULL,
  loan_id BIGINT NOT NULL,
  repayment_amount DECIMAL(15,2) NOT NULL,
  frozen_pledge_before DECIMAL(15,2) NOT NULL,
  frozen_pledge_after DECIMAL(15,2) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (guarantor_id) REFERENCES guarantors(id),
  FOREIGN KEY (loan_id) REFERENCES loans(id),
  INDEX idx_guarantor_id (guarantor_id),
  INDEX idx_loan_id (loan_id)
);

-- Step 2: Create guarantor_default_tracking table
CREATE TABLE IF NOT EXISTS guarantor_default_tracking (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  guarantor_id BIGINT NOT NULL,
  loan_id BIGINT NOT NULL,
  default_amount DECIMAL(15,2) NOT NULL,
  guarantor_share DECIMAL(15,2) NOT NULL,
  debit_amount DECIMAL(15,2) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (guarantor_id) REFERENCES guarantors(id),
  FOREIGN KEY (loan_id) REFERENCES loans(id),
  INDEX idx_guarantor_id (guarantor_id),
  INDEX idx_loan_id (loan_id)
);

-- Step 3: Remove old guarantor fields from loans table (if they exist)
-- These will be replaced by the one-to-many relationship via guarantors table
ALTER TABLE loans 
DROP COLUMN IF EXISTS guarantor1_id,
DROP COLUMN IF EXISTS guarantor2_id,
DROP COLUMN IF EXISTS guarantor3_id,
DROP COLUMN IF EXISTS guarantor1_eligibility_status,
DROP COLUMN IF EXISTS guarantor1_eligibility_errors,
DROP COLUMN IF EXISTS guarantor2_eligibility_status,
DROP COLUMN IF EXISTS guarantor2_eligibility_errors,
DROP COLUMN IF EXISTS guarantor3_eligibility_status,
DROP COLUMN IF EXISTS guarantor3_eligibility_errors;

-- Step 4: Ensure guarantors table has all required fields
-- Add columns if they don't exist
ALTER TABLE guarantors 
ADD COLUMN IF NOT EXISTS pledge_amount DECIMAL(15,2),
ADD COLUMN IF NOT EXISTS guarantee_amount DECIMAL(15,2),
ADD COLUMN IF NOT EXISTS self_guarantee BOOLEAN DEFAULT FALSE,
ADD COLUMN IF NOT EXISTS rejection_reason TEXT,
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;

-- Step 5: Create indexes for better query performance
ALTER TABLE guarantors 
ADD INDEX IF NOT EXISTS idx_loan_id (loan_id),
ADD INDEX IF NOT EXISTS idx_member_id (member_id),
ADD INDEX IF NOT EXISTS idx_status (status),
ADD INDEX IF NOT EXISTS idx_loan_member_status (loan_id, member_id, status);

-- Step 6: Add self_guarantee flag to loans table to track if member self-guaranteed
ALTER TABLE loans 
ADD COLUMN IF NOT EXISTS self_guarantee BOOLEAN DEFAULT FALSE;

-- Step 7: Verify migration
SELECT 'Guarantor system migration completed successfully' as status;
