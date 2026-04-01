-- Cleanup script for failed migrations V59, V60, V61
-- Execute this in MySQL before restarting the backend

-- Delete the failed migration records from Flyway history
DELETE FROM flyway_schema_history WHERE version IN (59, 60, 61);

-- Verify the deletions
SELECT * FROM flyway_schema_history WHERE version >= 57 ORDER BY version;
