-- V45: Add missing columns to bulk_member_items table
-- These columns are defined in the BulkMemberItem entity but were missing from the schema

ALTER TABLE bulk_member_items ADD COLUMN employer VARCHAR(100);
ALTER TABLE bulk_member_items ADD COLUMN bank VARCHAR(100);
ALTER TABLE bulk_member_items ADD COLUMN bank_account VARCHAR(50);
ALTER TABLE bulk_member_items ADD COLUMN next_of_kin VARCHAR(100);
ALTER TABLE bulk_member_items ADD COLUMN nok_phone VARCHAR(15);
