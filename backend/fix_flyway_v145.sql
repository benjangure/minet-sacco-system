-- Fix Flyway V145 migration failure
-- Remove the failed migration record so it can run again

USE tminet;

-- Delete the failed V145 migration record
DELETE FROM flyway_schema_history 
WHERE version = '145' AND success = 0;

-- Verify it's gone
SELECT version, description, success, installed_on 
FROM flyway_schema_history 
ORDER BY installed_rank DESC 
LIMIT 10;
