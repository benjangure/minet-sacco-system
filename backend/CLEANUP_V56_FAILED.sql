-- Clean up failed V56 migration
DELETE FROM flyway_schema_history WHERE version = 56;

-- Verify cleanup
SELECT version, description, success FROM flyway_schema_history WHERE version >= 55 ORDER BY version;
