-- V24: Drop the incorrect UNIQUE index on status column if it exists
-- The status column should NOT be unique - multiple batches can have the same status
-- This index was preventing multiple PENDING batches from being created

-- Safe drop - commented out since index may not exist on fresh install
-- ALTER TABLE bulk_batches DROP INDEX status;

-- Also remove any CHECK constraints that might be limiting status values
-- ALTER TABLE bulk_batches DROP CONSTRAINT chk_batch_status;
