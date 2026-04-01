-- V22: Convert fund_type column from ENUM to VARCHAR(255)
-- This allows storing fund types as strings instead of enum constants
ALTER TABLE fund_configurations MODIFY COLUMN fund_type VARCHAR(255) NOT NULL;
