-- Check Flyway migration history
SELECT version, description, type, success FROM flyway_schema_history ORDER BY version DESC LIMIT 10;
