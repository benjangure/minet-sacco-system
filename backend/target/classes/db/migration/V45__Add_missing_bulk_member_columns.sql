-- V45: Add missing columns to bulk_member_items table
-- These columns are defined in the BulkMemberItem entity but were missing from the schema

ALTER TABLE bulk_member_items ADD COLUMN IF NOT EXISTS employer VARCHAR(100);
ALTER TABLE bulk_member_items ADD COLUMN IF NOT EXISTS bank VARCHAR(100);
ALTER TABLE bulk_member_items ADD COLUMN IF NOT EXISTS bank_account VARCHAR(50);
ALTER TABLE bulk_member_items ADD COLUMN IF NOT EXISTS next_of_kin VARCHAR(100);
ALTER TABLE bulk_member_items ADD COLUMN IF NOT EXISTS nok_phone VARCHAR(15);
