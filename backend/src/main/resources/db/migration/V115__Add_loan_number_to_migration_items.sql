-- Add loan_number column to loan_migration_items table
-- This allows preserving historical loan numbers during migration
ALTER TABLE loan_migration_items ADD COLUMN loan_number VARCHAR(50) AFTER employee_id;
