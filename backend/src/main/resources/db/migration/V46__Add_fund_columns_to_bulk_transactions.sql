-- V46: Add missing fund amount columns to bulk_transaction_items table
-- These columns are defined in the BulkTransactionItem entity but were missing from the schema

ALTER TABLE bulk_transaction_items ADD COLUMN IF NOT EXISTS benevolent_fund_amount DECIMAL(15,2) DEFAULT 0;
ALTER TABLE bulk_transaction_items ADD COLUMN IF NOT EXISTS development_fund_amount DECIMAL(15,2) DEFAULT 0;
ALTER TABLE bulk_transaction_items ADD COLUMN IF NOT EXISTS school_fees_amount DECIMAL(15,2) DEFAULT 0;
ALTER TABLE bulk_transaction_items ADD COLUMN IF NOT EXISTS holiday_fund_amount DECIMAL(15,2) DEFAULT 0;
ALTER TABLE bulk_transaction_items ADD COLUMN IF NOT EXISTS emergency_fund_amount DECIMAL(15,2) DEFAULT 0;
