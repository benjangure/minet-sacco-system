-- V51__Add_max_loan_to_savings_multiplier.sql
-- Add configurable 3x savings maximum rule to loan eligibility rules

ALTER TABLE loan_eligibility_rules ADD COLUMN max_loan_to_savings_multiplier DECIMAL(5,2) DEFAULT 3.0 NOT NULL;

-- Update existing rules to have 3x multiplier
UPDATE loan_eligibility_rules SET max_loan_to_savings_multiplier = 3.0;
