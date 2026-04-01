-- V52__Fix_notification_type_column.sql
-- Fix notification type column to have proper length

ALTER TABLE notifications MODIFY COLUMN type VARCHAR(50) NOT NULL;
