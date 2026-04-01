-- V24: Drop the incorrect UNIQUE index on status column if it exists
-- The status column should NOT be unique - multiple batches can have the same status
-- This index was preventing multiple PENDING batches from being created

-- Safe drop - only if index exists
ALTER TABLE bulk_batches DROP INDEX IF EXISTS status;

-- Also remove any CHECK constraints that might be limiting status values
ALTER TABLE bulk_batches DROP CONSTRAINT IF EXISTS chk_batch_status;
