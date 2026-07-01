-- Add interest_collected column to loans table
-- This tracks how much interest has already been collected during migration (historical)
ALTER TABLE loans ADD COLUMN interest_collected DECIMAL(19, 2) DEFAULT 0 AFTER total_interest;

-- Update interestRemaining calculation for migrated loans
-- For new loans: interest_collected defaults to 0
-- For migrated loans: interestRemaining = totalInterest - interestCollected

-- Add interest_collected column to loan_migration_items table
ALTER TABLE loan_migration_items ADD COLUMN interest_collected DECIMAL(19, 2) AFTER outstanding_balance;

-- Update totalInterest logic in calculateRepaymentDetails()
-- Now: totalInterest is pre-calculated total
-- interestCollected tracks what was already paid during migration
-- interestRemaining = totalInterest - interestCollected (automatically)
