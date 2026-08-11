-- Migration script to add missing columns to loans table
-- Run this on your production database

-- Step 1: Add missing columns to loans table

-- Add principal_repaid column first (if not exists)
ALTER TABLE loans 
ADD COLUMN principal_repaid DECIMAL(15,2) DEFAULT 0.00 AFTER outstanding_balance;

-- Add interest_collected column (if not exists)
ALTER TABLE loans 
ADD COLUMN interest_collected DECIMAL(15,2) DEFAULT 0.00 AFTER total_interest;

-- Add interest_remaining column (if not exists)
ALTER TABLE loans 
ADD COLUMN interest_remaining DECIMAL(15,2) DEFAULT 0.00 AFTER interest_rate;

-- Add interest_collected_manual_override column
ALTER TABLE loans 
ADD COLUMN interest_collected_manual_override BOOLEAN DEFAULT FALSE AFTER interest_collected;

-- Add principal_repaid_manual_override column
ALTER TABLE loans 
ADD COLUMN principal_repaid_manual_override BOOLEAN DEFAULT FALSE AFTER principal_repaid;

-- Step 2: Verify the columns were added
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_NAME = 'loans' 
AND COLUMN_NAME IN (
    'interest_collected', 
    'interest_collected_manual_override', 
    'principal_repaid', 
    'principal_repaid_manual_override', 
    'interest_remaining'
)
ORDER BY COLUMN_NAME;

-- Step 3: Initialize values for existing loans
UPDATE loans 
SET 
    interest_collected = COALESCE(interest_collected, 0.00),
    principal_repaid = COALESCE(principal_repaid, 0.00),
    interest_remaining = COALESCE(interest_remaining, total_interest),
    interest_collected_manual_override = COALESCE(interest_collected_manual_override, FALSE),
    principal_repaid_manual_override = COALESCE(principal_repaid_manual_override, FALSE)
WHERE interest_collected IS NULL 
   OR principal_repaid IS NULL 
   OR interest_remaining IS NULL;
