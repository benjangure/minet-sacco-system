-- Reset V103 migration so it can run again
-- Run this manually if V103 didn't execute properly

DELETE FROM flyway_schema_history WHERE version = 103;
