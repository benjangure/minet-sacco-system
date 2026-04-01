-- V11: Add new contribution columns to bulk_transaction_items
-- Adds support for: SCHOOL_FEES, HOLIDAY_FUND, EMERGENCY_FUND

ALTER TABLE bulk_transaction_items
ADD COLUMN IF NOT EXISTS school_fees_amount DECIMAL(15,2) DEFAULT 0.00,
ADD COLUMN IF NOT EXISTS holiday_fund_amount DECIMAL(15,2) DEFAULT 0.00,
ADD COLUMN IF NOT EXISTS emergency_fund_amount DECIMAL(15,2) DEFAULT 0.00;
