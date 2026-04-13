-- Add frozen_savings column to track self-guarantee freezes (if not already present)
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS frozen_savings DECIMAL(19,2) DEFAULT 0.00 NOT NULL COMMENT 'Amount of savings frozen for self-guarantee loans. Cannot be withdrawn until loan is repaid.';
