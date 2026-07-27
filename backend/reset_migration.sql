-- Reset failed V107 migration
DELETE FROM flyway_schema_history WHERE version = '107';
