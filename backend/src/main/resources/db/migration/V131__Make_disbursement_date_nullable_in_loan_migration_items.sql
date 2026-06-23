-- Allow disbursement_date to be NULL so that validation-failed rows
-- (e.g. rows missing disbursement_date) can still be persisted with
-- their FAILED status and error message intact, instead of being
-- silently dropped because the original NOT NULL constraint blocked
-- the save.
ALTER TABLE loan_migration_items MODIFY COLUMN disbursement_date DATE NULL;
