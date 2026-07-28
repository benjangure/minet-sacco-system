-- V46: Add missing fund amount columns to bulk_transaction_items table
-- These columns are defined in the BulkTransactionItem entity but were missing from the schema
-- DISABLED: These columns are already added by V11__Add_new_contribution_columns.sql

-- ALTER TABLE bulk_transaction_items ADD COLUMN benevolent_fund_amount DECIMAL(15,2) DEFAULT 0;
-- ALTER TABLE bulk_transaction_items ADD COLUMN development_fund_amount DECIMAL(15,2) DEFAULT 0;
-- ALTER TABLE bulk_transaction_items ADD COLUMN school_fees_amount DECIMAL(15,2) DEFAULT 0;
-- ALTER TABLE bulk_transaction_items ADD COLUMN holiday_fund_amount DECIMAL(15,2) DEFAULT 0;
-- ALTER TABLE bulk_transaction_items ADD COLUMN emergency_fund_amount DECIMAL(15,2) DEFAULT 0;
