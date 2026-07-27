-- Add username column to audit_logs table for JSON serialization
ALTER TABLE audit_logs ADD COLUMN username VARCHAR(50);

-- Backfill existing audit logs with usernames from users table
UPDATE audit_logs al
SET username = (SELECT u.username FROM users u WHERE u.id = al.user_id)
WHERE al.user_id IS NOT NULL;
