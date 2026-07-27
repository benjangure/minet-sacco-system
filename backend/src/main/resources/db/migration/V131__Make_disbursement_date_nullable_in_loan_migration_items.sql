-- Make disbursement_date nullable in loan_migration_items table
-- This allows failed migration rows to be stored with error messages for audit trail
-- Previously, rows with missing/invalid dates would fail at INSERT time with cryptic DB errors

ALTER TABLE loan_migration_items MODIFY COLUMN disbursement_date DATE NULL;
