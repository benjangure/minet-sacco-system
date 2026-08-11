-- Fix Flyway migration failures
-- Run this in MySQL Workbench

USE minetsacco;

-- Delete the failed migration entries from flyway_schema_history
DELETE FROM flyway_schema_history WHERE version = '145' AND success = 0;
DELETE FROM flyway_schema_history WHERE version = '146' AND success = 0;

-- Verify - should show only successful migrations
SELECT version, description, success, installed_on 
FROM flyway_schema_history 
WHERE version IN ('145', '146')
ORDER BY version;
