-- Fix fund_type enum to remove REGISTRATION_FEE
ALTER TABLE fund_configurations MODIFY COLUMN fund_type ENUM('benevolent_fund','development_fund','school_fees','holiday_fund','emergency_fund') NOT NULL;
