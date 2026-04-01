-- Delete the failed V56 migration
USE sacco_db;
DELETE FROM flyway_schema_history WHERE version = 56;
