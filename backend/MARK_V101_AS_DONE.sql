-- Mark V101 migration as completed in Flyway history
-- The columns already exist, so we just need to tell Flyway it's done

INSERT INTO flyway_schema_history 
(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES 
(
  (SELECT MAX(installed_rank) + 1 FROM flyway_schema_history fsh),
  '101',
  'Add principal repaid manual override',
  'SQL',
  'V101__Add_principal_repaid_manual_override.sql',
  NULL,
  'manual',
  NOW(),
  0,
  1
);

-- Verify it was added
SELECT * FROM flyway_schema_history WHERE version = '101';
