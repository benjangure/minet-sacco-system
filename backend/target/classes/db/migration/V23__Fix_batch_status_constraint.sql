-- V23: Fix batch status constraint to allow COMPLETED status
-- Drop the old constraint and create a new one with correct values
ALTER TABLE bulk_batches DROP CONSTRAINT chk_batch_status;

ALTER TABLE bulk_batches ADD CONSTRAINT chk_batch_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'PROCESSING', 'COMPLETED', 'FAILED'));
